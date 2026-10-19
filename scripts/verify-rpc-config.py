"""在隔离的成品服务器中验证 RPC 配置，需要已安装且已接受 EULA 的测试服务器"""
import argparse
import json
import os
from pathlib import Path
import shutil
import socket
import subprocess
import tempfile
import time
import tomllib
import zipfile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("installed_server", type=Path)
args = parser.parse_args()
base = args.installed_server.resolve()
root = Path(__file__).resolve().parents[1]
artifact = root / "neoforge/build/libs/realmmesh-neoforge-1.21.1-0.1.0-SNAPSHOT.jar"
assert "eula=true" in (base / "eula.txt").read_text(), "测试服务器尚未接受 EULA"
work = Path(tempfile.mkdtemp(prefix="realmmesh-config-check-"))
print(f"Evidence: {work}", flush=True)
source = work / "ConfigDiagnostic.java"
source.write_text('''package io.github.helantianshen.realmmesh.diagnostic;
import io.github.helantianshen.realmmesh.config.RpcConfig;
import io.github.helantianshen.realmmesh.neoforge.config.NeoForgeRpcConfig;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import java.nio.file.*;
@EventBusSubscriber(modid="realmmesh")
public final class ConfigDiagnostic {
    @SubscribeEvent public static void started(ServerStartedEvent event) throws Exception {
        var config = NeoForgeRpcConfig.snapshot();
        var expected = System.getProperty("config.expected");
        var actual = config.host() + ":" + config.port() + ":" + config.timeoutMillis();
        if (!actual.equals(expected)) throw new AssertionError(actual + " != " + expected);
        new RpcConfig("localhost", 1, 1);
        new RpcConfig("::1", 65535, Integer.MAX_VALUE);
        for (var bad : new Object[][] {{null, 9000, 2000}, {"  ", 9000, 2000},
                {"localhost", 0, 2000}, {"localhost", 65536, 2000}, {"localhost", 9000, 0}}) {
            try {
                new RpcConfig((String) bad[0], (int) bad[1], (int) bad[2]);
                throw new AssertionError("Invalid config accepted");
            } catch (IllegalArgumentException expectedFailure) {}
        }
        Files.writeString(Path.of(System.getProperty("config.result")), actual);
    }
}
''')
classes = work / "classes"
classes.mkdir()
classpath = os.pathsep.join(map(str, [artifact, *(base / "libraries").rglob("*.jar")]))
subprocess.run(["javac", "-encoding", "UTF-8", "-cp", classpath, "-d", str(classes), str(source)], check=True)
jar = work / "diagnostic.jar"
shutil.copy2(artifact, jar)
with zipfile.ZipFile(jar, "a") as archive:
    for path in classes.rglob("*.class"):
        archive.write(path, path.relative_to(classes))

cases = [
    ("defaults", None, {"host": "127.0.0.1", "port": 9000, "timeoutMillis": 2000}),
    ("custom", '[rpc]\nhost="localhost"\nport=19000\ntimeoutMillis=3500\n',
     {"host": "localhost", "port": 19000, "timeoutMillis": 3500}),
    ("invalid", '[rpc]\nhost="  "\nport=65536\ntimeoutMillis=0\n',
     {"host": "127.0.0.1", "port": 9000, "timeoutMillis": 2000}),
]
for name, contents, expected in cases:
    server = work / name
    (server / "mods").mkdir(parents=True)
    (server / "libraries").symlink_to(base / "libraries", target_is_directory=True)
    shutil.copy2(base / "eula.txt", server / "eula.txt")
    shutil.copy2(jar, server / "mods/realmmesh.jar")
    config = server / "config/realmmesh/connection.toml"
    if contents is not None:
        config.parent.mkdir(parents=True)
        config.write_text(contents)
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        port = sock.getsockname()[1]
    (server / "server.properties").write_text(
        f"server-ip=127.0.0.1\nserver-port={port}\nonline-mode=false\n"
        "level-name=config-test\nlevel-type=minecraft:flat\n"
        'generator-settings={"layers":[{"block":"minecraft:bedrock","height":1}],"biome":"minecraft:plains"}\n'
        "generate-structures=false\nview-distance=2\nsimulation-distance=2\nmax-players=1\n")
    marker = server / "passed.txt"
    expected_text = f'{expected["host"]}:{expected["port"]}:{expected["timeoutMillis"]}'
    with (server / "game.log").open("w") as log:
        process = subprocess.Popen([
            "java", "-Xms256m", "-Xmx1G", f"-Dconfig.expected={expected_text}",
            f"-Dconfig.result={marker}",
            "@libraries/net/neoforged/neoforge/21.1.262/unix_args.txt", "nogui"
        ], cwd=server, stdin=subprocess.PIPE, stdout=log, stderr=subprocess.STDOUT, text=True)
        try:
            deadline = time.monotonic() + 120
            while not marker.exists():
                if process.poll() is not None or time.monotonic() > deadline:
                    raise RuntimeError(f"{name} failed; inspect {server / 'game.log'}")
                time.sleep(0.2)
            assert marker.read_text() == expected_text
            assert tomllib.loads(config.read_text())["rpc"] == expected
            if name == "invalid":
                assert "Correcting" in (server / "game.log").read_text()
                assert list(config.parent.glob("*.bak")), "缺少原配置备份"
            process.stdin.write("stop\n")
            process.stdin.flush()
            assert process.wait(timeout=30) == 0
        finally:
            if process.poll() is None:
                process.terminate()
                try:
                    process.wait(timeout=15)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait()
    print(f"PASS {name}: {expected_text}", flush=True)
(work / "result.json").write_text(json.dumps({"passed": [case[0] for case in cases]}, indent=2))
