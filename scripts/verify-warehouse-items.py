#!/usr/bin/env python3
"""在临时目录运行仓库物品与背包诊断，不创建游戏世界"""

import os
from pathlib import Path
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
proto = Path(os.environ.get("REALMMESH_PROTO_DIR", root.parent / "realmmesh-proto")).resolve()
with tempfile.TemporaryDirectory(prefix="realmmesh-warehouse-items-") as temporary:
    work = Path(temporary)
    init = work / "classpath.gradle"
    init.write_text("""allprojects {
    afterEvaluate { project ->
        if (project.name == 'neoforge') {
            tasks.register('warehouseDiagnosticClasspath') {
                dependsOn tasks.named('compileJava')
                doLast {
                    def output = new File(System.getProperty('warehouse.classpath'))
                    output.text = (sourceSets.main.runtimeClasspath + configurations.compileClasspath).asPath
                }
            }
        }
    }
}
""")
    classpath_file = work / "classpath.txt"
    subprocess.run([str(root / "gradlew"), f"-PrealmmeshProtoDir={proto}",
                    f"-Dwarehouse.classpath={classpath_file}", "-I", str(init),
                    ":neoforge:warehouseDiagnosticClasspath", "--console=plain"], cwd=root, check=True)
    classpath = classpath_file.read_text()
    subprocess.run(["javac", "-proc:none", "-cp", classpath, "-d", str(work),
                    str(root / "scripts/fixtures/WarehouseItemsDiagnostic.java"),
                    str(root / "scripts/fixtures/WarehouseTransferDiagnostic.java")], cwd=work, check=True)
    result = subprocess.run(["java", "-cp", str(work) + os.pathsep + classpath,
                             "WarehouseItemsDiagnostic",
                             str(root / "neoforge/build/warehouse-validation/fixtures")], cwd=work)
    if result.returncode:
        raise SystemExit(result.returncode)
    result = subprocess.run(["java", "-cp", str(work) + os.pathsep + classpath,
                             "WarehouseTransferDiagnostic"], cwd=work)
    raise SystemExit(result.returncode)
