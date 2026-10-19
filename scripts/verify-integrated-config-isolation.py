"""复用独立服隔离验证生成的世界，以两个真实客户端验证内置服局域网配置隔离"""
from pathlib import Path
import argparse
import json
import os
import re
import shutil
import signal
import socket
import subprocess
import tempfile
import threading
import time
import tomllib
import zlib

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('dedicated_test', type=Path)
args = parser.parse_args()
assert json.loads((args.dedicated_test / 'result.json').read_text())['passed']
root = Path(__file__).resolve().parents[1]
work = Path(tempfile.mkdtemp(prefix='realmmesh-lan-isolation-'))
print(f'TEST_DIR={work}', flush=True)
with socket.socket() as sock:
    sock.bind(('127.0.0.1', 0)); port = sock.getsockname()[1]
listener = socket.socket(); listener.bind(('127.0.0.1', 0)); listener.listen(); listener.settimeout(180)
proxy_port = listener.getsockname()[1]
private = 'private-rpc-6a8b2c-server-only.invalid'
for role in ['host', 'remote']:
    project = work / f'{role}-project'
    shutil.copytree(args.dedicated_test / 'project', project, ignore=shutil.ignore_patterns('build', '.gradle', 'runs', '.git'))
    for name in ['IsolationProbe.java', 'IsolationClientDiagnostic.java']:
        shutil.copy2(root / 'scripts/fixtures' / name, project / 'neoforge/src/main/java/io/github/helantianshen/realmmesh/diagnostic' / name)
    game = work / role
    (game / 'config/realmmesh').mkdir(parents=True)
    (game / 'config/fml.toml').write_text('earlyWindowControl=false\n')
    (game / 'options.txt').write_text('onboardAccessibility:false\nrenderDistance:2\nsimulationDistance:5\nmaxFps:30\n')
    if role == 'host':
        shutil.copytree(args.dedicated_test / 'server/isolation-test', game / 'saves/isolation-test')
        (game / 'config/realmmesh/connection.toml').write_text(f'[rpc]\nhost="{private}"\nport=43123\ntimeoutMillis=13579\n')
        (game / 'config/isolation-positive.toml').write_text('marker="SYNC_POSITIVE_CONTROL_7b9f"\n')
        run_args = f"systemProperty 'isolation.role', 'host'; systemProperty 'isolation.port', '{port}'; programArgument '--username'; programArgument 'IsolationHost'; programArgument '--quickPlaySingleplayer'; programArgument 'isolation-test'"
    else:
        (game / 'config/realmmesh/connection.toml').write_text('[rpc]\nhost="client-local.invalid"\nport=19001\ntimeoutMillis=3210\n')
        run_args = f"programArgument '--username'; programArgument 'IsolationProbe'; programArgument '--quickPlayMultiplayer'; programArgument '127.0.0.1:{proxy_port}'"
    build = project / 'neoforge/build.gradle'
    text = re.sub(r'^neoForge.runs.client.*$', '', build.read_text(), flags=re.M)
    build.write_text(text + f"\nneoForge.runs.client {{ gameDirectory = file('{game}'); systemProperty 'isolation.evidence', '{work}'; {run_args} }}\n")

capture = bytearray(); connections = []; proxy_errors = []
def relay(source, target, record):
    try:
        while data := source.recv(65536):
            if record: capture.extend(data)
            target.sendall(data)
    except OSError: pass
    finally:
        try: target.shutdown(socket.SHUT_WR)
        except OSError: pass

def proxy():
    try:
        downstream, _ = listener.accept(); upstream = socket.create_connection(('127.0.0.1', port))
        connections.extend([downstream, upstream])
        threads = [threading.Thread(target=relay,args=(upstream,downstream,True)),threading.Thread(target=relay,args=(downstream,upstream,False))]
        for thread in threads:thread.start()
        for thread in threads:thread.join()
    except Exception as error:proxy_errors.append(str(error))

def wait_for(marker, processes, timeout=240):
    end=time.monotonic()+timeout
    while not (work / marker).exists():
        if (work/'failed').exists():raise RuntimeError((work/'failed').read_text())
        if any(p.poll() is not None for p in processes):raise RuntimeError('Process exited early')
        if time.monotonic()>end:raise TimeoutError(f'Timeout {marker}; inspect {work}')
        time.sleep(.2)

def varint(data, offset):
    value=0
    for i in range(5):
        byte=data[offset];offset+=1;value|=(byte&127)<<(7*i)
        if byte<128:return value,offset
    raise ValueError('Invalid VarInt')

def decode(data):
    offset=0;compressed=False;packets=[]
    while offset<len(data):
        size,offset=varint(data,offset);frame=data[offset:offset+size];offset+=size
        assert len(frame)==size
        if compressed:
            length,index=varint(frame,0)
            packet=zlib.decompress(frame[index:]) if length else frame[index:]
            if length:assert len(packet)==length
        else:
            packet=frame
            packet_id,index=varint(packet,0)
            if packet_id==3:
                threshold,end=varint(packet,index)
                assert end==len(packet)
                compressed=True
        packets.append(packet)
    assert compressed,'Compression negotiation not observed'
    return b''.join(packets)

processes=[]
try:
    for role in ['host', 'remote']:
        if role=='remote':
            thread=threading.Thread(target=proxy,daemon=True);thread.start()
        project=work/f'{role}-project'
        with (work/f'{role}.log').open('w') as log:
            process=subprocess.Popen([str(project/'gradlew'),':neoforge:runClient','--console=plain'],cwd=project,stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
        processes.append(process)
        wait_for('host-ready' if role=='host' else 'client-pass', processes)
    assert processes[1].wait(timeout=45)==0
    thread.join(timeout=10);assert not thread.is_alive() and not proxy_errors,proxy_errors
    decoded=decode(bytes(capture))
    assert b'SYNC_POSITIVE_CONTROL_7b9f' in decoded and b'isolation-positive.toml' in decoded
    assert private.encode() not in decoded and b'realmmesh/connection.toml' not in decoded
    assert tomllib.loads((work/'remote/config/realmmesh/connection.toml').read_text())['rpc']=={'host':'client-local.invalid','port':19001,'timeoutMillis':3210}
    assert private not in (work/'remote.log').read_text()
    for path in (work/'remote').rglob('*'):
        if path.is_file():assert private.encode() not in path.read_bytes(),str(path)
    (work/'stop-host').touch();assert processes[0].wait(timeout=45)==0
    (work/'server-to-client.bin').write_bytes(capture)
    (work/'server-to-client-decoded.bin').write_bytes(decoded)
    result={'passed':True,'integrated_lan_remote_joined':True,'positive_control_synced':True,
            'private_config_on_wire':False,'client_local_config_preserved':True,
            'wire_bytes':len(capture),'decoded_bytes':len(decoded)}
    (work/'result.json').write_text(json.dumps(result,indent=2));print(json.dumps(result),flush=True)
finally:
    (work/'stop-host').touch()
    for process in processes:
        if process.poll() is None:
            os.killpg(process.pid,signal.SIGTERM)
            try:process.wait(timeout=15)
            except subprocess.TimeoutExpired:os.killpg(process.pid,signal.SIGKILL);process.wait()
    for conn in connections:conn.close()
    listener.close()
