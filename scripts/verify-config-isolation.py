"""通过真实客户端连接和无压缩回环流量捕获验证配置隔离，需要可用图形会话"""
from pathlib import Path
import json
import os
import shutil
import socket
import subprocess
import tempfile
import threading
import time
import tomllib

root = Path(__file__).resolve().parents[1]
work = Path(tempfile.mkdtemp(prefix='realmmesh-isolation-check-'))
print(f'TEST_DIR={work}', flush=True)
base = root / 'neoforge/build/bootstrap-validation/packaged-server'
assert 'eula=true' in (base / 'eula.txt').read_text()
dev = work / 'project'
dev.mkdir()
for name in ['build.gradle', 'settings.gradle', 'gradle.properties', 'gradlew', 'gradlew.bat']:
    shutil.copy2(root / name, dev / name)
for name in ['buildSrc', 'common', 'neoforge', 'gradle', 'proto']:
    shutil.copytree(root / name, dev / name, ignore=shutil.ignore_patterns('.git', '.gradle', 'build', 'runs'))
fixtures = dev / 'neoforge/src/main/java/io/github/helantianshen/realmmesh/diagnostic'
fixtures.mkdir(parents=True)
for name in ['IsolationProbe.java', 'IsolationClientDiagnostic.java']:
    shutil.copy2(root / 'scripts/fixtures' / name, fixtures / name)
entry = dev / 'neoforge/src/main/java/io/github/helantianshen/realmmesh/neoforge/RealmMeshNeoForge.java'
s = entry.read_text().replace('        RealmMesh.initialize();',
    '        io.github.helantianshen.realmmesh.diagnostic.IsolationProbe.register(container);\n        RealmMesh.initialize();')
entry.write_text(s)
server, client = work / 'server', work / 'client'
for directory in [server, client]:
    (directory / 'config/realmmesh').mkdir(parents=True)
(server / 'mods').mkdir()
(server / 'libraries').symlink_to(base / 'libraries', target_is_directory=True)
shutil.copy2(base / 'eula.txt', server / 'eula.txt')
private = 'private-rpc-6a8b2c-server-only.invalid'
(server / 'config/realmmesh/connection.toml').write_text(f'[rpc]\nhost="{private}"\nport=43123\ntimeoutMillis=13579\n')
(server / 'config/isolation-positive.toml').write_text('marker="SYNC_POSITIVE_CONTROL_7b9f"\n')
client_config = client / 'config/realmmesh/connection.toml'
client_config.write_text('[rpc]\nhost="client-local.invalid"\nport=19001\ntimeoutMillis=3210\n')
(client / 'options.txt').write_text('onboardAccessibility:false\nshowAutosaveIndicator:false\nrenderDistance:2\nsimulationDistance:5\nmaxFps:30\n')
with socket.socket() as sock:
    sock.bind(('127.0.0.1', 0)); mc_port = sock.getsockname()[1]
listener = socket.socket()
listener.bind(('127.0.0.1', 0)); proxy_port = listener.getsockname()[1]
listener.listen(); listener.settimeout(180)
(server / 'server.properties').write_text(f'server-ip=127.0.0.1\nserver-port={mc_port}\nonline-mode=false\nenforce-secure-profile=false\nnetwork-compression-threshold=-1\nlevel-name=isolation-test\nlevel-type=minecraft:flat\ngenerator-settings={{"layers":[{{"block":"minecraft:bedrock","height":1}}],"biome":"minecraft:plains"}}\ngenerate-structures=false\nview-distance=2\nsimulation-distance=2\nmax-players=1\n')
with (dev / 'neoforge/build.gradle').open('a') as out:
    out.write(f"\nneoForge.runs.client {{ gameDirectory = file('{client}'); systemProperty 'isolation.evidence', '{work}'; programArgument '--username'; programArgument 'IsolationProbe'; programArgument '--quickPlayMultiplayer'; programArgument '127.0.0.1:{proxy_port}' }}\n")
with (work / 'build.log').open('w') as log:
    subprocess.run([str(dev / 'gradlew'), ':neoforge:build', '--console=plain'], cwd=dev, stdout=log, stderr=subprocess.STDOUT, check=True)
shutil.copy2(dev / 'neoforge/build/libs/realmmesh-neoforge-1.21.1-0.1.0-SNAPSHOT.jar', server / 'mods/realmmesh-isolation.jar')
streams = {'server-to-client': bytearray(), 'client-to-server': bytearray()}
connections = []
proxy_errors = []
def relay(source, target, direction):
    try:
        while data := source.recv(65536):
            streams[direction].extend(data)
            target.sendall(data)
    except OSError:
        pass
    finally:
        try: target.shutdown(socket.SHUT_WR)
        except OSError: pass

def proxy():
    try:
        downstream, _ = listener.accept()
        upstream = socket.create_connection(('127.0.0.1', mc_port))
        connections.extend([downstream, upstream])
        threads = [threading.Thread(target=relay, args=(upstream, downstream, 'server-to-client')),
                   threading.Thread(target=relay, args=(downstream, upstream, 'client-to-server'))]
        for thread in threads: thread.start()
        for thread in threads: thread.join()
    except Exception as error:
        proxy_errors.append(str(error))

def wait_until(check, processes, timeout=180):
    end = time.monotonic() + timeout
    while not check():
        if (work / 'failed').exists(): raise RuntimeError((work / 'failed').read_text())
        for process in processes:
            if process.poll() is not None: raise RuntimeError(f'Process exited early: {process.returncode}')
        if time.monotonic() > end: raise TimeoutError('Inspect logs in ' + str(work))
        time.sleep(.2)

server_process = client_process = None
thread = None
try:
    with (work / 'server.log').open('w') as log:
        server_process = subprocess.Popen(['java', '-Xms256m', '-Xmx1G', '@libraries/net/neoforged/neoforge/21.1.262/unix_args.txt', 'nogui'], cwd=server, stdin=subprocess.PIPE, stdout=log, stderr=subprocess.STDOUT, text=True)
    wait_until(lambda: 'Done (' in (work / 'server.log').read_text(), [server_process])
    thread = threading.Thread(target=proxy, daemon=True); thread.start()
    with (work / 'client.log').open('w') as log:
        client_process = subprocess.Popen([str(dev / 'gradlew'), ':neoforge:runClient', '--console=plain'], cwd=dev, stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
    wait_until(lambda: (work / 'client-pass').exists(), [server_process, client_process], 240)
    assert client_process.wait(timeout=45) == 0
    thread.join(timeout=10)
    assert not thread.is_alive() and not proxy_errors, proxy_errors
    wire = bytes(streams['server-to-client'])
    assert b'SYNC_POSITIVE_CONTROL_7b9f' in wire, 'Positive sync payload absent from capture'
    assert b'isolation-positive.toml' in wire, 'Positive sync file absent from capture'
    assert private.encode() not in wire, 'Private config found in network stream'
    assert b'realmmesh/connection.toml' not in wire, 'Private config filename found in network stream'
    assert tomllib.loads(client_config.read_text())['rpc'] == {'host':'client-local.invalid','port':19001,'timeoutMillis':3210}
    assert private not in (work / 'client.log').read_text()
    for path in client.rglob('*'):
        if path.is_file(): assert private.encode() not in path.read_bytes(), f'Private marker in client file: {path}'
    server_process.stdin.write('stop\n'); server_process.stdin.flush()
    assert server_process.wait(timeout=30) == 0
    for direction, data in streams.items(): (work / f'{direction}.bin').write_bytes(data)
    result = {'passed':True, 'remote_client_joined':True, 'positive_control_synced':True,
              'private_config_on_wire':False, 'client_local_config_preserved':True,
              'client_private_marker_found':False, 'wire_bytes':len(wire)}
    (work / 'result.json').write_text(json.dumps(result, indent=2))
    print(json.dumps(result), flush=True)
finally:
    import signal
    if client_process is not None and client_process.poll() is None:
        os.killpg(client_process.pid, signal.SIGTERM)
        try: client_process.wait(timeout=15)
        except subprocess.TimeoutExpired: os.killpg(client_process.pid, signal.SIGKILL); client_process.wait()
    if server_process is not None and server_process.poll() is None:
        server_process.terminate()
        try: server_process.wait(timeout=15)
        except subprocess.TimeoutExpired: server_process.kill(); server_process.wait()
    for conn in connections: conn.close()
    listener.close()
