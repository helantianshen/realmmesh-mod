"""按候选清单导出隔离快照并复验生成与构建，不创建提交或修改 Git 暂存区"""
from pathlib import Path
import hashlib
import io
import json
import shutil
import subprocess
import tarfile
import tempfile
import os

mod = Path(__file__).resolve().parents[1]
workspace = mod.parent
work = Path(tempfile.mkdtemp(prefix='realmmesh-phase1-build-'))
print(f'TEST_DIR={work}', flush=True)
expected_proto = 'a9daf901a4e129a4a228cd75de2b7ef7f22c9ab4'

def capture(*args, cwd):
    return subprocess.check_output(args, cwd=cwd, text=True).strip()

def generated_hashes(root):
    return {str(p.relative_to(root)): hashlib.sha256(p.read_bytes()).hexdigest()
            for p in sorted((root / 'gen').rglob('*')) if p.is_file()}

before = {}
expected = {}
for name in ['realmmesh-server', 'realmmesh-mod']:
    source = workspace / name
    before[name] = capture('git', 'ls-files', '--stage', cwd=source)
    assert capture('git', '-C', 'proto', 'rev-parse', 'HEAD', cwd=source) == expected_proto
    assert not capture('git', '-C', 'proto', 'status', '--porcelain', cwd=source)
    link = capture('git', 'ls-files', '--stage', 'proto', cwd=source)
    assert link.startswith(f'160000 {expected_proto} ')
    target = work / name
    target.mkdir()
    files = (source / 'docs/PHASE1_FILES.txt').read_text().splitlines()
    assert len(files) == len(set(files))
    for name_in_repo in files:
        path = Path(name_in_repo)
        assert not path.is_absolute() and '..' not in path.parts
        assert path.parts[0] not in {'.git', '.idea', '.codex', '.tools', 'bin', 'build', '.gradle'}
        if name_in_repo == 'proto':
            continue
        assert (source / path).is_file(), name_in_repo
        (target / path).parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source / path, target / path)
    # 从已提交的协议对象导出，避免把工作区协议修改误当成固定版本
    archive = subprocess.check_output(['git', '-C', 'proto', 'archive', expected_proto], cwd=source)
    (target / 'proto').mkdir()
    with tarfile.open(fileobj=io.BytesIO(archive)) as stream:
        stream.extractall(target / 'proto', filter='data')
    expected[name] = generated_hashes(target)
    assert expected[name]
    shutil.rmtree(target / 'gen')

steps = []
def run(name, command, cwd, env=None):
    print('RUN ' + name, flush=True)
    with (work / f'{name}.log').open('w') as log:
        subprocess.run(command, cwd=cwd, env=env, stdout=log, stderr=subprocess.STDOUT, check=True)
    steps.append(name)

server = work / 'realmmesh-server'
tools = server / '.tools/bin'; tools.mkdir(parents=True)
env = os.environ.copy(); env['GOBIN'] = str(tools)
run('install-protoc-gen-go', ['go', 'install', 'google.golang.org/protobuf/cmd/protoc-gen-go@v1.36.11'], server, env)
run('install-protoc-gen-go-grpc', ['go', 'install', 'google.golang.org/grpc/cmd/protoc-gen-go-grpc@v1.6.2'], server, env)
run('go-generate', ['bash', 'scripts/generate-proto.sh'], server)
assert generated_hashes(server) == expected['realmmesh-server'], 'Go generated sources differ'
run('go-mod-download', ['go', 'mod', 'download'], server)
run('go-mod-verify', ['go', 'mod', 'verify'], server)
run('go-test', ['go', 'test', '-race', '-count=1', './...'], server)
run('go-vet', ['go', 'vet', './...'], server)
(server / 'bin').mkdir()
run('go-build', ['go', 'build', '-o', 'bin/realmmesh', './cmd/realmmesh'], server)
run('go-probe-build', ['go', 'build', '-o', 'bin/probe', './cmd/probe'], server)
java = work / 'realmmesh-mod'
run('java-generate-build', ['./gradlew', ':common:exportProtoSources', 'build', '--no-build-cache', '--rerun-tasks', '--console=plain'], java)
assert generated_hashes(java) == expected['realmmesh-mod'], 'Java generated sources differ'
for name, index in before.items():
    assert capture('git', 'ls-files', '--stage', cwd=workspace / name) == index, 'Source index changed'
result = {'passed': True, 'validation': 'isolated_candidate_snapshot_not_git_checkout',
          'proto_commit': expected_proto, 'generated_sources_match': True, 'steps': steps,
          'shared_dependency_caches': True, 'source_indexes_unchanged': True}
(work / 'result.json').write_text(json.dumps(result, indent=2))
print(json.dumps(result), flush=True)
