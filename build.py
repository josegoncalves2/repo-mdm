#!/usr/bin/env python3
"""
Sistema de Build e Versionamento do HWMDM
===========================================
Uso:
  python3 build.py                         # Build completo (WAR + APK)
  python3 build.py --war-only              # So a WAR
  python3 build.py --apk-only              # So o APK remoto
  python3 build.py --version               # Mostra versao atual
  python3 build.py --bump-version          # Incrementa versao (patch)
  python3 build.py --set-version X.Y.Z     # Define versao especifica
"""
import os, sys, subprocess, hashlib, json
from datetime import datetime

REPO = os.path.dirname(os.path.abspath(__file__))
VERSION_FILE = os.path.join(REPO, "VERSION")
BUILD_LOG = os.path.join(REPO, "dist", "builds.json")
DIST_DIR = os.path.join(REPO, "dist")
os.makedirs(DIST_DIR, exist_ok=True)

def read_version():
    with open(VERSION_FILE) as f:
        return f.read().strip()

def write_version(v):
    with open(VERSION_FILE, "w") as f:
        f.write(v.strip() + "\n")

def bump_version(v):
    p = v.split(".")
    p[-1] = str(int(p[-1]) + 1)
    return ".".join(p)

def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()

def log_build(version, artifacts):
    builds = []
    if os.path.exists(BUILD_LOG):
        with open(BUILD_LOG) as f:
            try: builds = json.load(f)
            except: builds = []
    builds.append({"version": version,
        "timestamp": datetime.utcnow().isoformat() + "Z",
        "artifacts": artifacts})
    with open(BUILD_LOG, "w") as f:
        json.dump(builds, f, indent=2)

def run_cmd(cmd, cwd, desc):
    print("\n[BUILD] {}\n$ {}".format(desc, " ".join(cmd)))
    r = subprocess.run(cmd, cwd=cwd)
    if r.returncode != 0:
        print("[ERROR] {} FALHOU (exit={})".format(desc, r.returncode))
        sys.exit(r.returncode)
    print("[OK] {} concluido".format(desc))

def build_war(version):
    print("\n>>> Compilando WAR v{}...".format(version))
    run_cmd(["mvn", "clean", "package", "-DskipTests"],
            os.path.join(REPO, "server-source"), "Build Maven")
    src = os.path.join(REPO, "server-source", "server", "target", "launcher.war")
    dst = os.path.join(DIST_DIR, "hmdm-v{}.war".format(version))
    if os.path.exists(src):
        import shutil
        shutil.copy2(src, dst)
        shutil.copy2(src, os.path.join(DIST_DIR, "hmdm.war"))
        h = sha256(dst)
        with open(dst + ".sha256", "w") as f:
            f.write("{}  {}\n".format(h, os.path.basename(dst)))
        print("[WAR] {} ({} KB) sha256={}...".format(dst, os.path.getsize(dst)//1024, h[:16]))
        return dst
    print("[ERROR] WAR nao encontrada")
    return None

def build_apk(version):
    print("\n>>> Compilando Remote Agent APK v{}...".format(version))
    gradlew = os.path.join(REPO, "remote-agent", "gradlew")
    os.chmod(gradlew, 0o755)
    run_cmd([gradlew, "assembleRelease"],
            os.path.join(REPO, "remote-agent"), "Build Gradle")
    src = os.path.join(REPO, "remote-agent", "app", "build", "outputs",
                       "apk", "release", "app-release.apk")
    dst = os.path.join(DIST_DIR, "hwmdm-remote-v{}.apk".format(version))
    if os.path.exists(src):
        import shutil
        shutil.copy2(src, dst)
        shutil.copy2(src, os.path.join(DIST_DIR, "hwmdm-remote-latest.apk"))
        h = sha256(dst)
        with open(dst + ".sha256", "w") as f:
            f.write("{}  {}\n".format(h, os.path.basename(dst)))
        print("[APK] {} ({} KB) sha256={}...".format(dst, os.path.getsize(dst)//1024, h[:16]))
        return dst
    print("[ERROR] APK nao encontrado")
    return None

if __name__ == "__main__":
    args = sys.argv[1:]
    v = read_version()
    if "--version" in args:
        print("Versao atual: {}".format(v))
        sys.exit(0)
    if "--bump-version" in args:
        v = bump_version(v)
        write_version(v)
        print("Versao incrementada: {}".format(v))
        sys.exit(0)
    artifacts = []
    if "--apk-only" not in args:
        w = build_war(v)
        if w: artifacts.append({"type": "war", "path": w})
    if "--war-only" not in args:
        a = build_apk(v)
        if a: artifacts.append({"type": "apk-remote", "path": a})
    log_build(v, artifacts)
    print("\n[BUILD] v{} COMPLETO".format(v))
