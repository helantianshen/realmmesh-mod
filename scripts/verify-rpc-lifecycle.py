"""使用隔离 NeoForge 成品服和本地 Go 中心验证生命周期和命令"""
from pathlib import Path
import tempfile, shutil, zipfile, subprocess, socket, os, time, json
root=Path(__file__).resolve().parents[1]
test=Path(tempfile.mkdtemp(prefix='realmmesh-lifecycle-check-'))
print('TEST_DIR='+str(test),flush=True)
evidence=root/'neoforge/build/lifecycle-validation'
evidence.mkdir(parents=True,exist_ok=True)
(evidence/'integration-path.txt').write_text(str(test)+'\n')
base=root/'neoforge/build/bootstrap-validation/packaged-server'
assert 'eula=true' in (base/'eula.txt').read_text(), 'Test server EULA not accepted'
server=test/'server';(server/'mods').mkdir(parents=True)
(server/'libraries').symlink_to(base/'libraries',target_is_directory=True)
shutil.copy2(base/'eula.txt',server/'eula.txt')
shutil.copy2(base/'run.sh',server/'run.sh')
(server/'user_jvm_args.txt').write_text('-Xms512m\n-Xmx2G\n')
def port():
 with socket.socket() as s:s.bind(('127.0.0.1',0));return s.getsockname()[1]
http_port,grpc_port,mc_port=port(),port(),port()
(server/'server.properties').write_text(f'server-ip=127.0.0.1\nserver-port={mc_port}\nonline-mode=false\nlevel-name=rpc-test\nlevel-type=minecraft:flat\ngenerator-settings={{"layers":[{{"block":"minecraft:bedrock","height":1}}],"biome":"minecraft:plains"}}\ngenerate-structures=false\nview-distance=2\nsimulation-distance=2\nmax-players=1\n')
(test/'config.yaml').write_text(f'server:\n  http:\n    addr: "127.0.0.1:{http_port}"\n  grpc:\n    addr: "127.0.0.1:{grpc_port}"\n')
(server/'config/realmmesh').mkdir(parents=True)
(server/'config/realmmesh/connection.toml').write_text(f'[rpc]\nhost="127.0.0.1"\nport={grpc_port}\ntimeoutMillis=2000\n')
subprocess.run(['go','build','-o',str(test/'center'),'./cmd/realmmesh'],cwd=root.parent/'realmmesh-server',check=True)
artifact=root/'neoforge/build/libs/realmmesh-neoforge-1.21.1-0.1.0-SNAPSHOT.jar'
shutil.copy2(artifact,server/'mods/realmmesh-test.jar')
embedded=test/'embedded';embedded.mkdir()
with zipfile.ZipFile(artifact) as z:
 for n in z.namelist():
  if n.startswith('META-INF/jarjar/') and n.endswith('.jar'):(embedded/Path(n).name).write_bytes(z.read(n))
source=root/'scripts/fixtures/LifecycleDiagnostic.java'
classes=test/'classes';classes.mkdir()
cp=[artifact,*embedded.glob('*.jar'),*(base/'libraries').rglob('*.jar')]
subprocess.run(['javac','-proc:none','-encoding','UTF-8','-cp',os.pathsep.join(map(str,cp)),'-d',str(classes),str(source)],check=True)
with zipfile.ZipFile(server/'mods/realmmesh-test.jar','a') as z:
 for f in classes.rglob('*.class'):z.write(f,str(f.relative_to(classes)))
center=None;game=None
center_log=(test/'center.log').open('w');game_log=(test/'game.log').open('w')
def start_center():
 p=subprocess.Popen([str(test/'center'),'-conf',str(test/'config.yaml')],stdout=center_log,stderr=subprocess.STDOUT)
 for _ in range(100):
  try:
   with socket.create_connection(('127.0.0.1',grpc_port),timeout=.1):return p
  except OSError:
   if p.poll() is not None:raise RuntimeError('center exited')
   time.sleep(.05)
 p.terminate();p.wait(timeout=15)
 raise RuntimeError('center not ready')
def wait_marker(name,seconds=120):
 end=time.monotonic()+seconds
 while not (test/name).exists():
  if (test/'failed').exists():raise RuntimeError((test/'failed').read_text())
  if game.poll() is not None:raise RuntimeError('game exited '+str(game.returncode))
  if time.monotonic()>end:raise RuntimeError('timeout '+name)
  time.sleep(.2)
 print('PASS '+name,flush=True)
try:
 game=subprocess.Popen(['java',f'-Drpc.test.dir={test}',f'-Drpc.test.port={grpc_port}','@user_jvm_args.txt','@libraries/net/neoforged/neoforge/21.1.262/unix_args.txt','nogui'],cwd=server,stdin=subprocess.PIPE,stdout=game_log,stderr=subprocess.STDOUT,text=True)
 wait_marker('offline-pass')
 center=start_center();(test/'center-started').touch()
 wait_marker('online-pass',25)
 game.stdin.write('realmmesh ping\n');game.stdin.flush()
 time.sleep(1)
 center.terminate();center.wait(timeout=15);(test/'center-stopped').touch()
 wait_marker('down-pass',20)
 center=start_center();(test/'center-restarted').touch()
 wait_marker('recovery-pass',25)
 wait_marker('all-pass',30)
 game.wait(timeout=45)
 if game.returncode!=0:raise RuntimeError('game shutdown code '+str(game.returncode))
 print('PASS game shutdown',flush=True)
 (test/'result.json').write_text(json.dumps({'passed':True,'grpc_port':grpc_port,'mc_port':mc_port,'lifecycle_commands_tested':True},indent=2))
finally:
 for p in [game,center]:
  if p is not None and p.poll() is None:
   p.terminate()
   try:p.wait(timeout=20)
   except subprocess.TimeoutExpired:p.kill();p.wait()
 center_log.close();game_log.close()
 dest=evidence
 for n in ['game.log','center.log','result.json']:
  if (test/n).exists():shutil.copy2(test/n,dest/n)
