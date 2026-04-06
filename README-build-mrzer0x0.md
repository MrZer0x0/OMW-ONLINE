# Patched builder for MrZer0x0/TES3MP

This archive is based on OMW-ONLINE-master and includes two critical fixes:

1. The corrupted `buildscripts/CMakeLists.txt` was cleaned up.
2. TES3MP source download was switched to `https://github.com/MrZer0x0/TES3MP.git` on branch `Main`.

## Native build

From the repository root:

```bash
cd buildscripts
./build.sh --arch arm64
```

## APK build

From the repository root:

```bash
./gradlew assembleDebug
```

Output APK:

```
app/build/outputs/apk/debug/
```
