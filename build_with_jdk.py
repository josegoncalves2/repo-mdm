#!/usr/bin/env python3
import subprocess
import os
import sys

os.chdir("C:\\Users\\40446686808\\projetos\\MDM\\repo-mdm\\server-source")
os.environ["JAVA_HOME"] = "C:\\Users\\40446686808\\projetos\\MDM\\repo-mdm\\.jdk21"
mvn_path = "C:\\Users\\40446686808\\projetos\\MDM\\repo-mdm\\.maven\\bin\\mvn.cmd"

cmd = [mvn_path, "clean", "package", "-DskipTests"]
result = subprocess.run(cmd, shell=True)
sys.exit(result.returncode)
