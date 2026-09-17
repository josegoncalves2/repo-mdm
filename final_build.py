#!/usr/bin/env python3
import subprocess
import os
import sys
import shutil

os.chdir("C:\\Users\\40446686808\\projetos\\MDM\\repo-mdm\\server-source")

# Add JDK to PATH so mvn script can find java
jdk_bin = "C:\\Users\\40446686808\\projetos\\MDM\\repo-mdm\\.jdk21\\bin"
os.environ["PATH"] = jdk_bin + ";" + os.environ.get("PATH", "")
os.environ["JAVA_HOME"] = "C:\\Users\\40446686808\\projetos\\MDM\\repo-mdm\\.jdk21"

mvn_bat = "C:\\Users\\40446686808\\projetos\\MDM\\repo-mdm\\.maven\\bin\\mvn.bat"

# Use list form to avoid shell injection/blocking
cmd = [mvn_bat, "clean", "package", "-DskipTests"]

# Don't use shell=True
result = subprocess.run(cmd, shell=False)
sys.exit(result.returncode)
