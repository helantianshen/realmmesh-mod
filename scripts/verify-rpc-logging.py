"""复验两端关联日志、失败记录与敏感信息过滤，需要已有隔离服务器安装环境"""
from pathlib import Path
import collections
import json
import re
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
report = root / 'neoforge/build/logging-validation'
report.mkdir(parents=True, exist_ok=True)
results = {}
for script, directory in [('verify-rpc-client.py', 'client-validation'), ('verify-rpc-lifecycle.py', 'lifecycle-validation')]:
    subprocess.run([sys.executable, str(root / 'scripts' / script)], cwd=root, check=True)
    evidence = root / 'neoforge/build' / directory
    game = (evidence / 'game.log').read_text()
    center = (evidence / 'center.log').read_text()
    for secret in ['PRIVATE_RPC_PAYLOAD_SECRET', 'PRIVATE_RPC_METADATA_SECRET', 'PRIVATE_ENDPOINT_192.0.2.99_SECRET']:
        assert secret not in game + center, f'Sensitive marker found in {directory}'

    def events(text, side):
        result = collections.defaultdict(list)
        for line in text.splitlines():
            match = re.search(rf'event=rpc_{side}_(start|end)\b', line)
            if not match:
                continue
            fields = dict(re.findall(r'(\w+)=([^\s]+)', line[match.start():]))
            assert set(fields) == ({'event', 'request_id', 'method'} if match[1] == 'start' else
                                   {'event', 'request_id', 'method', 'status', 'duration_ms'}), fields
            assert re.fullmatch(r'[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}', fields['request_id'])
            assert fields['method'] == 'realmmesh.probe.v1.ProbeService/Echo'
            if match[1] == 'end':
                assert int(fields['duration_ms']) >= 0
            result[fields['request_id']].append(fields)
        assert result, f'No {side} logs'
        for request_id, rows in result.items():
            assert [r['event'] for r in rows] == [f'rpc_{side}_start', f'rpc_{side}_end'], request_id
        return result

    clients, servers = events(game, 'client'), events(center, 'server')
    common = clients.keys() & servers.keys()
    assert len(common) >= (24 if directory == 'client-validation' else 3), 'Missing correlated calls'
    for request_id in common:
        assert clients[request_id][1]['status'] == servers[request_id][1]['status'] == 'OK'
    unknown = servers.keys() - clients.keys()
    assert len(unknown) == (1 if directory == 'client-validation' else 0), 'Unexpected server IDs'
    statuses = {rows[1]['status'] for rows in clients.values()}
    assert {'OK', 'UNAVAILABLE', 'DEADLINE_EXCEEDED'} <= statuses
    if directory == 'client-validation':
        assert {'CANCELLED', 'INVALID_ARGUMENT'} <= statuses
    else:
        assert 'INTERNAL' in statuses
    # 未到达 Go 的失败请求和临时模拟服务请求只能在 Java 日志中出现
    assert clients.keys() - servers.keys()
    results[directory] = {'client_calls': len(clients), 'server_calls': len(servers),
                          'correlated_calls': len(common), 'statuses': sorted(statuses)}
    print(f'PASS {directory}: {results[directory]}', flush=True)
(report / 'result.json').write_text(json.dumps(results, indent=2))
