#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import subprocess
import os
import sys
import shutil

repo_root = "C:\\Users\\40446686808\\projetos\\MDM\\repo-mdm"
build_log = os.path.join(repo_root, "build.log")

def log_msg(msg):
    print(msg)
    with open(build_log, 'a', encoding='utf-8') as f:
        f.write(msg + "\n")

# Clear previous log
with open(build_log, 'w', encoding='utf-8') as f:
    f.write("")

log_msg("=" * 60)
log_msg("MDM Project Build Script")
log_msg("=" * 60)

# Check Maven availability
log_msg("\n[INFO] Checking Maven...")
mvn_path = os.path.join(repo_root, ".maven\\bin\\mvn.cmd")
if os.path.exists(mvn_path):
    log_msg("[OK] Maven found at: " + mvn_path)
    log_msg("[INFO] Starting Maven build (server-source)...")
    os.chdir(os.path.join(repo_root, "server-source"))
    cmd = [mvn_path, "clean", "package", "-DskipTests"]
    result = subprocess.run(cmd, shell=True, capture_output=False)
    if result.returncode == 0:
        log_msg("[OK] Maven build completed!")
    else:
        log_msg("[ERROR] Maven build failed (exit code: " + str(result.returncode) + ")")
        log_msg("[NOTE] This may be due to EDR/security policy blocking Maven execution")
else:
    log_msg("[WARNING] Maven not found at: " + mvn_path)

# Check Docker availability
log_msg("\n[INFO] Checking Docker...")
docker_path = shutil.which("docker")
if docker_path:
    log_msg("[OK] Docker found at: " + docker_path)
    log_msg("[INFO] Building WebFilter Docker image...")
    os.chdir(os.path.join(repo_root, "webfilter-dns"))
    cmd = ["docker", "build", "-t", "webfilter-dns:latest", "-f", "Dockerfile", ".."]
    result = subprocess.run(cmd, shell=True)
    if result.returncode == 0:
        log_msg("[OK] WebFilter Docker build completed!")
    else:
        log_msg("[ERROR] WebFilter Docker build failed (exit code: " + str(result.returncode) + ")")
else:
    log_msg("[WARNING] Docker not found in PATH")
    log_msg("[INFO] WebFilter Docker image cannot be built without Docker")

log_msg("\n" + "=" * 60)
log_msg("[INFO] Build script completed. Check build.log for details.")
log_msg("=" * 60)
print("\nLog saved to: " + build_log)
