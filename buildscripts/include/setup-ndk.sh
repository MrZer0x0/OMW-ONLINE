#!/bin/bash

set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

source ./include/version.sh

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

	LLVM_BIN="$(pwd)/ndk/toolchains/llvm/prebuilt/linux-x86_64/bin"
	LLVM_SYSROOT="$(pwd)/ndk/toolchains/llvm/prebuilt/linux-x86_64/sysroot"

	mkdir -p $ARCH/bin

	# NDK r23+: wrapper scripts call `$dirname/clang` so we must have
	# the real clang binary accessible in the same bin/ directory.
	# Symlink the actual clang/clang++ executables (not the API-versioned wrappers).
	ln -sf $LLVM_BIN/clang   $ARCH/bin/clang
	ln -sf $LLVM_BIN/clang++ $ARCH/bin/clang++

	# Create triplet-specific wrappers that forward to clang with the right --target
	TARGET_TRIPLE="${NDK_TRIPLET}${ANDROID_API}"

	cat > $ARCH/bin/${NDK_TRIPLET}-clang << WRAPPER
#!/bin/bash
exec "\$(dirname \$0)/clang" --target=${TARGET_TRIPLE} "\$@"
WRAPPER
	chmod +x $ARCH/bin/${NDK_TRIPLET}-clang

	cat > $ARCH/bin/${NDK_TRIPLET}-clang++ << WRAPPER
#!/bin/bash
exec "\$(dirname \$0)/clang++" --target=${TARGET_TRIPLE} "\$@"
WRAPPER
	chmod +x $ARCH/bin/${NDK_TRIPLET}-clang++

	# gcc aliases so old autoconf scripts work
	ln -sf ${NDK_TRIPLET}-clang   $ARCH/bin/${NDK_TRIPLET}-gcc
	ln -sf ${NDK_TRIPLET}-clang++ $ARCH/bin/${NDK_TRIPLET}-g++

	# llvm tool symlinks (with and without triplet prefix)
	for TOOL in ar ranlib strip nm objdump objcopy readelf; do
		[[ -f $LLVM_BIN/llvm-$TOOL ]] && ln -sf $LLVM_BIN/llvm-$TOOL $ARCH/bin/${NDK_TRIPLET}-$TOOL || true
		[[ -f $LLVM_BIN/llvm-$TOOL ]] && ln -sf $LLVM_BIN/llvm-$TOOL $ARCH/bin/llvm-$TOOL        || true
	done

	# ld.lld
	[[ -f $LLVM_BIN/ld.lld ]] && ln -sf $LLVM_BIN/ld.lld $ARCH/bin/${NDK_TRIPLET}-ld || true
	[[ -f $LLVM_BIN/ld.lld ]] && ln -sf $LLVM_BIN/ld.lld $ARCH/bin/ld.lld            || true

	# Sysroot symlink — needed by build.sh to find libc++_shared.so
	ln -sf $LLVM_SYSROOT $ARCH/sysroot

	# gas-preprocessor for ffmpeg (kept for compatibility)
	cp ../patches/gas-preprocessor.pl $ARCH/bin/ 2>/dev/null || true
fi

popd
