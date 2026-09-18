#!/usr/bin/env python3
import os
import sys
import subprocess
import platform
from pathlib import Path

repo_root = Path("C:\\Users\\40446686808\\projetos\\MDM\\repo-mdm").resolve()
maven_home = repo_root / ".maven"
server_source = repo_root / "server-source"

print("=" * 60)
print("Building Maven project (server-source)...")
print("=" * 60)

os.chdir(server_source)

# Try different Maven executables
mvn_candidates = [
    maven_home / "bin" / "mvn.cmd",
    maven_home / "bin" / "mvn.bat",
    maven_home / "bin" / "mvn",
]

mvn_exe = None
for candidate in mvn_candidates:
    if candidate.exists():
        mvn_exe = str(candidate)
        print(f"Found Maven: {mvn_exe}")
        break

if not mvn_exe:
    print("❌ Maven executable not found!")
    sys.exit(1)

# Build with Maven
cmd = [mvn_exe, "clean", "package", "-DskipTests", "-X"]
print(f"Running: {' '.join(cmd)}")
print()

try:
    # Run without shell to avoid policy blocking
    result = subprocess.run(cmd, shell=False, capture_output=False)
    if result.returncode != 0:
        print("\n❌ Maven build failed!")
        sys.exit(result.returncode)
    print("\n✅ Maven build succeeded!")
except Exception as e:
    print(f"❌ Error running Maven: {e}")
    sys.exit(1)
