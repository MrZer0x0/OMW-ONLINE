#!/bin/bash

set -e

if [ ! -f "downloads/$NDK_FILE" ]; then
    echo "==> Downloading NDK"
    mkdir -p downloads
    cd downloads
    
    # Обновленный URL для загрузки NDK
    wget "https://dl.google.com/android/repository/android-ndk-${NDK_VERSION}-linux.zip" -O "$NDK_FILE"
    cd ..
fi

echo "==> Checking NDK zip file integrity"
echo "$NDK_HASH downloads/$NDK_FILE" | sha256sum -c

echo "(Extracting, this will take a while...)"
rm -rf ./toolchain
mkdir -p ./toolchain
cd ./toolchain
unzip -q "../downloads/$NDK_FILE"
mv android-ndk-${NDK_VERSION} ndk
cd ..
