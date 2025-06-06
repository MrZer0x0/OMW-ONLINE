#!/bin/bash

set -e

source ./include/version.sh

mkdir -p downloads

pushd downloads

if [[ -f $NDK_FILE ]]; then
	# We've already downloaded it
	exit 0
fi

curl --http1.1 "https://dl.google.com/android/repository/android-ndk-${NDK_VERSION}-linux.zip" > $NDK_FILE

popd
