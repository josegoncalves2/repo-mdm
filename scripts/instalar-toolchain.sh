#!/usr/bin/env bash
# Instala JDK 21 + Android SDK dentro do proprio projeto, sem root.
# O Gradle 8.13 do wrapper nao roda com JDK 25, e o JDK do sistema aqui e' o 17,
# que tambem nao serve. Por isso o toolchain fica versionado por diretorio.
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JDK_DIR="$RAIZ/.jdk21"
SDK_DIR="$RAIZ/.android-sdk"

JDK_URL="https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse"
CLI_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"

if [ ! -x "$JDK_DIR/bin/javac" ]; then
    echo ">> baixando JDK 21"
    mkdir -p "$JDK_DIR"
    curl -fsSL "$JDK_URL" -o /tmp/jdk21.tar.gz
    tar -xzf /tmp/jdk21.tar.gz -C "$JDK_DIR" --strip-components=1
    rm -f /tmp/jdk21.tar.gz
fi
echo ">> JDK: $("$JDK_DIR/bin/java" -version 2>&1 | head -1)"

export JAVA_HOME="$JDK_DIR"
export PATH="$JDK_DIR/bin:$PATH"

if [ ! -x "$SDK_DIR/cmdline-tools/latest/bin/sdkmanager" ]; then
    echo ">> baixando command-line tools"
    mkdir -p "$SDK_DIR/cmdline-tools"
    curl -fsSL "$CLI_URL" -o /tmp/cmdline-tools.zip
    rm -rf "$SDK_DIR/cmdline-tools/latest"
    unzip -q /tmp/cmdline-tools.zip -d "$SDK_DIR/cmdline-tools"
    mv "$SDK_DIR/cmdline-tools/cmdline-tools" "$SDK_DIR/cmdline-tools/latest"
    rm -f /tmp/cmdline-tools.zip
fi

SDKMANAGER="$SDK_DIR/cmdline-tools/latest/bin/sdkmanager"

echo ">> aceitando licencas"
yes | "$SDKMANAGER" --sdk_root="$SDK_DIR" --licenses >/dev/null 2>&1 || true

echo ">> instalando platform-tools, android-34 e build-tools 34.0.0"
"$SDKMANAGER" --sdk_root="$SDK_DIR" \
    "platform-tools" "platforms;android-34" "build-tools;34.0.0" >/dev/null

echo ">> pronto"
echo "JAVA_HOME=$JDK_DIR"
echo "ANDROID_HOME=$SDK_DIR"
