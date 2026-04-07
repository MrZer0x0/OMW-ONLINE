#!/bin/bash

set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

source ./include/version.sh

NDK_PREBUILT="$DIR/../toolchain/ndk/toolchains/llvm/prebuilt/linux-x86_64"

if [[ ! -d toolchain ]]; then
	mkdir -p toolchain

	echo "(Extracting, this will take a while...)"
	unzip -q downloads/$NDK_FILE -d toolchain/
	mv toolchain/android-ndk-* toolchain/ndk/

	if [[ $CCACHE = "true" ]]; then
		echo "==> Patching common toolchain for ccache support"

		pushd toolchain/ndk/toolchains/llvm/prebuilt/linux-x86_64/bin

		mv "clang" "clangX"
		mv "clang++" "clangX++"
		cp "$DIR/../patches/clang-ccache.sh" "clang"
		cp "$DIR/../patches/clang++-ccache.sh" "clang++"
		chmod +x "clang"
		chmod +x "clang++"

		popd
	fi
fi

pushd toolchain

if [[ ! -d $ARCH ]]; then
	echo "==> Setting up standalone-compatible toolchain for $ARCH (NDK r23+)"

	mkdir -p $ARCH/bin

	# Symlink all prebuilt llvm tools into $ARCH/bin
	# NDK r23+ dropped make_standalone_toolchain.py — we wire clang directly
	ln -sf $NDK_PREBUILT/bin $ARCH/bin_ndk

	# Create NDK_TRIPLET-clang / clang++ wrappers pointing to API-versioned binaries
	CLANG_BIN="$NDK_PREBUILT/bin"

	# Primary compilers
	ln -sf $CLANG_BIN/${NDK_TRIPLET}${ANDROID_API}-clang     $ARCH/bin/${NDK_TRIPLET}-clang
	ln -sf $CLANG_BIN/${NDK_TRIPLET}${ANDROID_API}-clang++   $ARCH/bin/${NDK_TRIPLET}-clang++

	# Aliases gcc -> clang so old autoconf scripts work
	ln -sf ${NDK_TRIPLET}-clang   $ARCH/bin/${NDK_TRIPLET}-gcc
	ln -sf ${NDK_TRIPLET}-clang++ $ARCH/bin/${NDK_TRIPLET}-g++

	# ar, ranlib, strip, nm, objdump — all from llvm
	for TOOL in ar ranlib strip nm objdump objcopy readelf; do
		if [[ -f $CLANG_BIN/llvm-$TOOL ]]; then
			ln -sf $CLANG_BIN/llvm-$TOOL $ARCH/bin/${NDK_TRIPLET}-$TOOL
		fi
	done

	# llvm-* tools needed by boost and cmake (no prefix variants)
	for TOOL in ar ranlib strip nm; do
		if [[ -f $CLANG_BIN/llvm-$TOOL ]]; then
			ln -sf $CLANG_BIN/llvm-$TOOL $ARCH/bin/llvm-$TOOL
		fi
	done

	# ld.lld -> ld
	if [[ -f $CLANG_BIN/ld.lld ]]; then
		ln -sf $CLANG_BIN/ld.lld $ARCH/bin/${NDK_TRIPLET}-ld
		ln -sf $CLANG_BIN/ld.lld $ARCH/bin/ld.lld
	fi

	# Sysroot symlink (needed for libc++_shared.so lookup in build.sh)
	ln -sf $NDK_PREBUILT/sysroot $ARCH/sysroot

	# gas-preprocessor for ffmpeg (legacy, kept for compatibility)
	cp ../patches/gas-preprocessor.pl $ARCH/bin/ 2>/dev/null || true

	if [[ $CCACHE = "true" ]]; then
		echo "==> Patching '$ARCH' toolchain for ccache support"
		pushd $ARCH/bin/
		sed -i "s|\`dirname \$0\`/clang|ccache \\0|" "${NDK_TRIPLET}-clang"   2>/dev/null || true
		sed -i "s|\`dirname \$0\`/clang|ccache \\0|" "${NDK_TRIPLET}-clang++" 2>/dev/null || true
		popd
	fi
fi

popd
