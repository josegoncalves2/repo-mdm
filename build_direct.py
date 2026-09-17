#!/usr/bin/env python3
import subprocess
import os
import sys

os.chdir("C:\\Users\\40446686808\\projetos\\MDM\\repo-mdm\\server-source")
os.environ["JAVA_HOME"] = "C:\\Users\\40446686808\\projetos\\MDM\\repo-mdm\\.jdk21"

# Run Maven without shell=True to avoid EDR blocking
java_exe = "C:\\Users\\40446686808\\projetos\\MDM\\repo-mdm\\.jdk21\\bin\\java.exe"
mvn_jar = "C:\\Users\\40446686808\\projetos\\MDM\\repo-mdm\\.maven\\lib\\maven-core-3.9.9.jar"

# Try using mvn.cmd directly with proper list formatting
mvn_cmd = "C:\\Users\\40446686808\\projetos\\MDM\\repo-mdm\\.maven\\bin\\mvn.cmd"

# Execute using subprocess list form (no shell)
cmd = [mvn_cmd, "clean", "package", "-DskipTests"]
result = subprocess.run(cmd)
sys.exit(result.returncode)
