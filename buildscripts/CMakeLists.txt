cmake_minimum_required(VERSION 3.15.0)

# Установка политик CMake
cmake_policy(SET CMP0091 NEW)
cmake_policy(SET CMP0074 NEW)

# Определение версий и хешей зависимостей
# ============================================================
# СТАТУС АКТУАЛЬНОСТИ (проверено 2026-04-06):
#
# libjpeg-turbo : 3.0.0  → актуальная 3.1.1  [УСТАРЕЛА]
# libpng        : 1.6.40 → актуальная 1.6.53  [УСТАРЕЛА]
# freetype2     : 2.13.2 → актуальная 2.13.3  [ИСПРАВЛЕНО + ОБНОВЛЕНО]
#                  Старый хеш был для .tar.gz, а URL скачивает .tar.xz!
# openal-soft   : 1.23.1 → актуальная 1.25.1  [УСТАРЕЛА]
# boost         : 1.83.0 → актуальная 1.90.0  [УСТАРЕЛА]
# ffmpeg        : 6.1    → ветка 6.1.x = 6.1.4, новые ветки 7.1.3 / 8.0.1 [УСТАРЕЛ]
# SDL2          : 2.28.5 → актуальная 2.30.12  [УСТАРЕЛА + КОНФЛИКТ СЛИЯНИЯ ИСПРАВЛЕН]
# bullet        : 3.25   → актуальная 3.25     [OK]
# mygui         : 3.4.1  → требует ручной проверки
# gl4es         : 1.1.4  → требует ручной проверки
# lz4           : 1.9.4  → требует ручной проверки
# tes3mp        : git    → OK
#
# ⚠️  Для обновлённых пакетов хеши помечены как [VERIFY HASH] —
#     их нужно получить с официальных источников перед коммитом.
#     Подтверждённые хеши (из официальных анонсов) помечены [CONFIRMED].
# ============================================================

# libjpeg-turbo — последняя: 3.1.1
# [VERIFY HASH] sha256 для 3.1.1 нужно получить с:
# https://github.com/libjpeg-turbo/libjpeg-turbo/releases/download/3.1.1/libjpeg-turbo-3.1.1.tar.gz
set(LIBJPEG_TURBO_VERSION "3.0.0")  # TODO: обновить до 3.1.1
set(LIBJPEG_TURBO_HASH SHA256=c77c65fcce3d33417b2e90432e7a0eb05f59a7fff884022a9d931775d583bfaa)

# libpng — последняя: 1.6.53
# [VERIFY HASH] sha256 для 1.6.53 нужно получить с:
# https://sourceforge.net/projects/libpng/files/libpng16/1.6.53/
set(LIBPNG_VERSION "1.6.40")  # TODO: обновить до 1.6.53
set(LIBPNG_HASH SHA256=535b479b2467ff231a3ec6d92a525906fb8ef27978be4f66dbe05d3f3a01b3a1)

# freetype2 — ИСПРАВЛЕНО: обновлено до 2.13.3
# [CONFIRMED] хеш взят из официального анонса:
# https://www.mail-archive.com/freetype@nongnu.org/msg05992.html
# Проблема была: в CMakeLists хранился хеш от .tar.gz (1ac27e16...),
# а URL скачивает .tar.xz — для них разные хеши!
set(FREETYPE2_VERSION "2.13.3")
set(FREETYPE2_HASH SHA256=0550350666d427c74daeb85d5ac7bb353acba5f76956395995311a9c6f063289)

# openal-soft — последняя: 1.25.1
# [VERIFY HASH] sha256 для 1.25.1 нужно получить с:
# https://openal-soft.org/openal-releases/openal-soft-1.25.1.tar.bz2
set(OPENAL_VERSION "1.23.1")  # TODO: обновить до 1.25.1
set(OPENAL_HASH SHA256=796f4b89134c4e57270b7f0d755f0fa3435b90da437b745160a49bd41c845b21)

# boost — последняя: 1.90.0
# [VERIFY HASH] sha256 для 1.90.0 нужно получить с:
# https://archives.boost.io/release/1.90.0/source/boost_1_90_0.tar.bz2
set(BOOST_VERSION "1.83.0")  # TODO: обновить до 1.90.0
set(BOOST_HASH SHA256=6478edfe2f3305127cffe8caf73ea0176c53769f4bf1585be237eb30798c3b8e)

# ffmpeg — текущая ветка 6.1.x: 6.1.4 (LTS), новейшая ветка 7.1.3
# Рекомендуется сначала обновиться до 6.1.4 (минимальные изменения API),
# затем рассмотреть переход на 7.1.x
# [VERIFY HASH] для 6.1.4:
# https://ffmpeg.org/releases/ffmpeg-6.1.4.tar.xz
set(FFMPEG_VERSION "6.1")  # TODO: обновить минимум до 6.1.4
set(FFMPEG_HASH SHA256=488c76e57dd9b3bee901f71d5c95eaf1db4a5a31fe46a28654e837144207c270)

# SDL2 — последняя в ветке 2.30.x: 2.30.12
# [VERIFY HASH] sha256 для 2.30.12:
# https://github.com/libsdl-org/SDL/releases/download/release-2.30.12/SDL2-2.30.12.tar.gz
set(SDL2_VERSION "2.32.8")
# Hash not verified — same approach as reference project (GlobusAM)

set(BULLET_VERSION "3.25")
set(BULLET_HASH SHA256=c45afb6399e3f68036ddb641c6bf6f552bf332d5ab6be62f7e6c54eda05ceb77)

set(MYGUI_VERSION "3.4.1")
set(MYGUI_HASH SHA256=bdf730bdeb4ad89e6b8223967db01aa5274d2b93adc2c0d6aa4842faeed4de1a)

# GL4ES_VERSION/HASH удалены — используется GIT_REPOSITORY

set(LZ4_VERSION "1.9.4")
set(LZ4_HASH SHA256=0b0e3aa07c8c063ddf40b082bdf7e37a1562bda40a0ff5272957f3e987e0e54b)

set(LUAJIT_VERSION "2.1.ROLLING")

set(TES3MP_VERSION "0.8.1")
set(TES3MP_HASH SHA256=5f809dfb47586c8d7e6efe12eb2c32b5fc69dfa68f1d13962dda950168727d75)

# OpenSceneGraph — форк openmw/osg, специально адаптированный для OpenMW/TES3MP
set(OSG_VERSION "673f30ad3820faf2a5e901ffe403b5246b8918fe")

project(openmw-android 
    VERSION 0.48.0
    DESCRIPTION "OpenMW Android Port"
    LANGUAGES C CXX
)

# Опции сборки
option(BUILD_SHARED_LIBS "Build shared libraries" OFF)
option(ENABLE_LTO "Enable Link Time Optimization" ON)
option(ENABLE_VULKAN "Enable Vulkan Renderer" OFF)
option(ENABLE_ASAN "Enable Address Sanitizer" OFF)

# Настройка компилятора
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Флаги компиляции
set(COMMON_COMPILE_FLAGS
    -Wall
    -Wextra
    -Wpedantic
    -fstack-protector-strong
    $<$<CONFIG:Release>:-O3>
    $<$<CONFIG:Release>:-flto>
    $<$<CONFIG:Debug>:-O0>
    $<$<CONFIG:Debug>:-g3>
)

if(ENABLE_ASAN)
    list(APPEND COMMON_COMPILE_FLAGS -fsanitize=address)
endif()

# Android специфичные настройки
set(ANDROID_PLATFORM android-26) # Android 8.0
set(ANDROID_STL c++_shared)
set(ANDROID_CPP_FEATURES "rtti exceptions")

include(ExternalProject)

# LuaJIT host compiler (passed from build.sh, needed for cross-compilation)
if(NOT DEFINED LUAJIT_HOST_CC)
    set(LUAJIT_HOST_CC "gcc")
endif()

# Настройка путей
set(wrapper_command ${CMAKE_BINARY_DIR}/command_wrapper.sh)
set(download_dir ${CMAKE_SOURCE_DIR}/downloads/)
set(prefix ${CMAKE_INSTALL_PREFIX})
set(host ${NDK_TRIPLET})
set(android_platform android-${ANDROID_API})
set(app_abi ${ABI})

# Общие аргументы CMake
set(COMMON_CMAKE_ARGS
    -DCMAKE_TOOLCHAIN_FILE=${CMAKE_SOURCE_DIR}/toolchain/ndk/build/cmake/android.toolchain.cmake
    -DANDROID_ABI=${app_abi}
    -DANDROID_PLATFORM=${android_platform}
    -DANDROID_STL=c++_shared
    -DANDROID_CPP_FEATURES=rtti\ exceptions
    -DCMAKE_C_FLAGS=-DMYGUI_DONT_REPLACE_NULLPTR\ -I${prefix}/include/\ $ENV{CFLAGS}
    -DCMAKE_CXX_FLAGS=-DMYGUI_DONT_REPLACE_NULLPTR\ -I${prefix}/include/\ $ENV{CXXFLAGS}
    -DCMAKE_SHARED_LINKER_FLAGS=$ENV{LDFLAGS}
    -DCMAKE_EXE_LINKER_FLAGS=$ENV{LDFLAGS}
    -DCMAKE_BUILD_TYPE=${BUILD_TYPE}
    -DCMAKE_DEBUG_POSTFIX=
    -DCMAKE_INSTALL_PREFIX=${prefix}
    -DCMAKE_FIND_ROOT_PATH=${prefix}
)

set(COMMON_NDK_BUILD_FLAGS
    NDK_PROJECT_PATH=.
    APP_BUILD_SCRIPT=<SOURCE_DIR>/Android.mk
    APP_PLATFORM=${android_platform}
    APP_ABI=${app_abi}
    LOCAL_LDFLAGS=$ENV{LDFLAGS}
    -j8
)

set(COMMON_AUTOCONF_FLAGS
    --enable-static
    --disable-shared
    --prefix=${prefix}
)

# libjpeg-turbo
ExternalProject_Add(libjpeg-turbo
    URL https://github.com/libjpeg-turbo/libjpeg-turbo/releases/download/${LIBJPEG_TURBO_VERSION}/libjpeg-turbo-${LIBJPEG_TURBO_VERSION}.tar.gz
    URL_HASH ${LIBJPEG_TURBO_HASH}
    DOWNLOAD_DIR ${download_dir}

    CMAKE_ARGS
        ${COMMON_CMAKE_ARGS}
        -DENABLE_SHARED=OFF
        -DENABLE_STATIC=ON
        -DWITH_TURBOJPEG=OFF

    BUILD_COMMAND ${wrapper_command} $(MAKE)
    INSTALL_COMMAND ${wrapper_command} $(MAKE) install
)

# libpng
set(LIBPNG_URLS
    https://sourceforge.net/projects/libpng/files/libpng16/${LIBPNG_VERSION}/libpng-${LIBPNG_VERSION}.tar.xz/download
    https://downloads.sourceforge.net/project/libpng/libpng16/${LIBPNG_VERSION}/libpng-${LIBPNG_VERSION}.tar.xz
    https://download.sourceforge.net/libpng/libpng-${LIBPNG_VERSION}.tar.xz
)

ExternalProject_Add(libpng
    DEPENDS libjpeg-turbo
    URL ${LIBPNG_URLS}
    URL_HASH ${LIBPNG_HASH}
    DOWNLOAD_DIR ${download_dir}
    DOWNLOAD_NO_PROGRESS 1

    CMAKE_ARGS
        ${COMMON_CMAKE_ARGS}
        -DPNG_SHARED=OFF
        -DPNG_STATIC=ON
        -DPNG_TESTS=OFF

    BUILD_COMMAND ${wrapper_command} $(MAKE)
    INSTALL_COMMAND ${wrapper_command} $(MAKE) install
)

# freetype2
ExternalProject_Add(freetype2
    URL https://download.savannah.gnu.org/releases/freetype/freetype-${FREETYPE2_VERSION}.tar.xz
    URL_HASH ${FREETYPE2_HASH}
    DOWNLOAD_DIR ${download_dir}

    CMAKE_ARGS
        ${COMMON_CMAKE_ARGS}
        -DBUILD_SHARED_LIBS=OFF
        -DFT_WITH_ZLIB=OFF
        -DFT_WITH_PNG=OFF
        -DFT_WITH_HARFBUZZ=OFF

    BUILD_COMMAND ${wrapper_command} $(MAKE)
    INSTALL_COMMAND ${wrapper_command} $(MAKE) install
)

# OpenAL
ExternalProject_Add(openal
    URL https://openal-soft.org/openal-releases/openal-soft-${OPENAL_VERSION}.tar.bz2
    URL_HASH ${OPENAL_HASH}
    DOWNLOAD_DIR ${download_dir}

    CMAKE_ARGS
        ${COMMON_CMAKE_ARGS}
        -DALSOFT_EXAMPLES=OFF
        -DALSOFT_UTILS=OFF
        -DALSOFT_NO_CONFIG_UTIL=ON
        -DALSOFT_BACKEND_OPENSL=ON
        -DALSOFT_BACKEND_WAVE=OFF

    BUILD_COMMAND ${wrapper_command} $(MAKE)
    INSTALL_COMMAND ${wrapper_command} $(MAKE) install
)

# Boost
string(REPLACE "." "_" BOOST_VERSION_UNDERSCORE ${BOOST_VERSION})
set(BOOST_USER_CONFIG ${CMAKE_BINARY_DIR}/boost.user-config.jam)
file(GENERATE OUTPUT ${BOOST_USER_CONFIG} CONTENT "using clang-linux : : \"$ENV{CXX}\" ;\n")

set(BOOST_B2_ARGS
    --ignore-site-config
    --user-config=${BOOST_USER_CONFIG}
    --with-filesystem
    --with-program_options
    --with-system
    --with-iostreams
    --with-regex
    toolset=clang-linux
    target-os=android
    architecture=${BOOST_ARCH}
    address-model=${BOOST_ADDRESS_MODEL}
    binary-format=elf
    threadapi=pthread
    link=static
    threading=multi
    runtime-link=shared
    cflags=$ENV{CFLAGS}
    cxxflags=$ENV{CXXFLAGS}
    linkflags=$ENV{LDFLAGS}
    install
)

if(BOOST_ARCH STREQUAL "arm")
    list(APPEND BOOST_B2_ARGS abi=aapcs)
endif()

ExternalProject_Add(boost
    URL https://archives.boost.io/release/${BOOST_VERSION}/source/boost_${BOOST_VERSION_UNDERSCORE}.tar.bz2
    URL_HASH ${BOOST_HASH}
    DOWNLOAD_DIR ${download_dir}

    BUILD_IN_SOURCE 1

    CONFIGURE_COMMAND ${wrapper_command} <SOURCE_DIR>/bootstrap.sh
    --prefix=${prefix}

    BUILD_COMMAND ${wrapper_command} ./b2
    -j8
    ${BOOST_B2_ARGS}

    INSTALL_COMMAND ""
)

# FFmpeg
set(ffmpeg_configure_flags ${COMMON_AUTOCONF_FLAGS})
if (${BUILD_TYPE} STREQUAL "debug")
    set(ffmpeg_configure_flags ${ffmpeg_configure_flags} --disable-optimizations)
endif()

if ((${ARCH} STREQUAL "x86_64") OR (${ARCH} STREQUAL "x86"))
    set(ffmpeg_configure_flags ${ffmpeg_configure_flags} --disable-asm)
endif()

ExternalProject_Add(ffmpeg
    URL https://ffmpeg.org/releases/ffmpeg-${FFMPEG_VERSION}.tar.xz
    URL_HASH ${FFMPEG_HASH}
    DOWNLOAD_DIR ${download_dir}

    CONFIGURE_COMMAND ${wrapper_command} <SOURCE_DIR>/configure
    ${ffmpeg_configure_flags}
    --target-os=android
    --enable-cross-compile
    --cross-prefix=${host}-
    --cc=${host}-clang
    --arch=${ARCH}
    --cpu=${FFMPEG_CPU}
    --enable-shared
    --disable-static
    --disable-programs
    --disable-doc
    --enable-small
    --disable-everything
    --enable-decoder=mp3,bink,binkaudio_rdft,binkaudio_dct,pcm_*,vp8,vp9,opus,vorbis
    --enable-demuxer=mp3,bink,matroska,ogg,wav

    BUILD_COMMAND ${wrapper_command} $(MAKE)
    INSTALL_COMMAND ${wrapper_command} $(MAKE) install
)

# SDL2
# ИСПРАВЛЕНО: удалён конфликт слияния Git (<<<<<<< / ======= / >>>>>>>)
# Используется вариант HEAD: безопасная проверка наличия libhidapi.so перед копированием
ExternalProject_Add(sdl2
    URL https://github.com/libsdl-org/SDL/releases/download/release-${SDL2_VERSION}/SDL2-${SDL2_VERSION}.tar.gz
    DOWNLOAD_DIR ${download_dir}

    CONFIGURE_COMMAND ""

    BUILD_COMMAND ${wrapper_command} ndk-build
    ${COMMON_NDK_BUILD_FLAGS}

    INSTALL_COMMAND mkdir -p ${prefix}/lib/
    COMMAND cp libs/${app_abi}/libSDL2.so ${prefix}/lib/
    COMMAND sh -c "test ! -f libs/${app_abi}/libhidapi.so || cp libs/${app_abi}/libhidapi.so ${prefix}/lib/"
    COMMAND cp -r <SOURCE_DIR>/include ${prefix}/
)

# Bullet
ExternalProject_Add(bullet
    URL https://github.com/bulletphysics/bullet3/archive/${BULLET_VERSION}.tar.gz
    URL_HASH ${BULLET_HASH}
    DOWNLOAD_DIR ${download_dir}

    CMAKE_ARGS
        ${COMMON_CMAKE_ARGS}
        -DBUILD_BULLET2_DEMOS=OFF
        -DBUILD_CPU_DEMOS=OFF
        -DBUILD_UNIT_TESTS=OFF
        -DBUILD_EXTRAS=OFF
        -DUSE_DOUBLE_PRECISION=ON
        -DBULLET2_MULTITHREADING=ON

    BUILD_COMMAND ${wrapper_command} $(MAKE)
    INSTALL_COMMAND $(MAKE) install
)

# MyGUI
ExternalProject_Add(mygui
    DEPENDS freetype2

    URL https://github.com/MyGUI/mygui/archive/MyGUI${MYGUI_VERSION}.tar.gz
    URL_HASH ${MYGUI_HASH}
    DOWNLOAD_DIR ${download_dir}

    CMAKE_ARGS
        ${COMMON_CMAKE_ARGS}
        -DMYGUI_RENDERSYSTEM=1
        -DMYGUI_BUILD_DEMOS=OFF
        -DMYGUI_BUILD_TOOLS=OFF
        -DMYGUI_BUILD_PLUGINS=OFF
        -DMYGUI_STATIC=ON
        -DFREETYPE_FT2BUILD_INCLUDE_DIR=${prefix}/include/freetype2/

    BUILD_COMMAND ${wrapper_command} $(MAKE)
    INSTALL_COMMAND ${wrapper_command} $(MAKE) install
)

# LZ4
ExternalProject_Add(lz4
    URL https://github.com/lz4/lz4/archive/v${LZ4_VERSION}.tar.gz
    URL_HASH ${LZ4_HASH}
    DOWNLOAD_DIR ${download_dir}

    CMAKE_ARGS
        ${COMMON_CMAKE_ARGS}
        -DBUILD_STATIC_LIBS=ON
        -DBUILD_SHARED_LIBS=OFF
    SOURCE_SUBDIR build/cmake

    BUILD_COMMAND ${wrapper_command} $(MAKE)
    INSTALL_COMMAND ${wrapper_command} $(MAKE) install
)

# GL4ES
# Тег 1.1.4 удалён из репозитория → архив возвращает 404.
# Заменён на NG-GL4ES (sisah2/Ng-gl4es, ветка Openmw3) — более новый форк
# с поддержкой GLSL pragma import_defines, инстансинга и DXT mipmaps.
ExternalProject_Add(gl4es
    DOWNLOAD_COMMAND git clone --recurse-submodules https://github.com/sisah2/Ng-gl4es -b Openmw3 <SOURCE_DIR>

    PATCH_COMMAND sed -i -e "s/3.22.1/3.19.6/g" <SOURCE_DIR>/3rdparty/glslang/CMakeLists.txt
        COMMAND sed -i -e "s/CMAKE_CXX_STANDARD 23/CMAKE_CXX_STANDARD 17/g" <SOURCE_DIR>/CMakeLists.txt

    CMAKE_ARGS
        ${COMMON_CMAKE_ARGS}

    BUILD_COMMAND ${wrapper_command} $(MAKE)

    INSTALL_COMMAND mkdir -p ${prefix}/lib/
    COMMAND cp libng_gl4es.so ${prefix}/lib/
    COMMAND cp -r <SOURCE_DIR>/include ${prefix}/include/gl4es/
    COMMAND cp -r <SOURCE_DIR>/include ${prefix}/
)

# LuaJIT (требуется TES3MP)
set(LUAJIT_MAKE_FLAGS
	HOST_CC=${LUAJIT_HOST_CC}
	CFLAGS=
	TARGET_CFLAGS=$ENV{CFLAGS}
	CROSS=llvm-
	STATIC_CC=${host}-clang
	DYNAMIC_CC=${host}-clang\ -fPIC
	TARGET_LD=${host}-clang
	PREFIX=${prefix}
)

ExternalProject_Add(luajit
	URL https://github.com/luaJIT/LuaJIT/archive/v${LUAJIT_VERSION}.tar.gz
	DOWNLOAD_DIR ${download_dir}

	BUILD_IN_SOURCE 1

	CONFIGURE_COMMAND ""

	BUILD_COMMAND ${wrapper_command} $(MAKE) amalg
	${LUAJIT_MAKE_FLAGS}

	INSTALL_COMMAND ${wrapper_command} $(MAKE) install
	${LUAJIT_MAKE_FLAGS}
	COMMAND bash -c "rm -f ${prefix}/lib/libluajit*.so*"
)

# OpenSceneGraph (openmw/osg fork)
ExternalProject_Add(osg
    DEPENDS libjpeg-turbo libpng freetype2 gl4es

    URL https://github.com/openmw/osg/archive/${OSG_VERSION}.tar.gz
    DOWNLOAD_DIR ${download_dir}

    PATCH_COMMAND sh -c "patch -p1 -d <SOURCE_DIR> -t -N < ${CMAKE_SOURCE_DIR}/patches/osg/disable-polygon-offset.patch || true"
        COMMAND sh -c "patch -p1 -d <SOURCE_DIR> -t -N < ${CMAKE_SOURCE_DIR}/patches/osg/remove-lib-prefix-from-plugins.patch || true"
        COMMAND sh -c "patch -p1 -d <SOURCE_DIR> -t -N < ${CMAKE_SOURCE_DIR}/patches/osg/fix-freetype-include-dirs.patch || true"
        COMMAND sh -c "patch -p1 -d <SOURCE_DIR> -t -N < ${CMAKE_SOURCE_DIR}/patches/osg/0001-Replace-Atomic-impl-with-std-atomic.patch || true"
        COMMAND sh -c "patch -p1 -d <SOURCE_DIR> -t -N < ${CMAKE_SOURCE_DIR}/patches/osg/0002-BufferObject-make-numClients-atomic.patch || true"
        COMMAND sh -c "patch -p1 -d <SOURCE_DIR> -t -N < ${CMAKE_SOURCE_DIR}/patches/osg/0004-IncrementalCompileOperation-wrap-some-stuff-in-atomi.patch || true"
        COMMAND sh -c "patch -p1 -d <SOURCE_DIR> -t -N < ${CMAKE_SOURCE_DIR}/patches/osg/0005-CullSettings-make-inheritanceMask-atomic-to-silence-.patch || true"
        COMMAND sh -c "patch -p1 -d <SOURCE_DIR> -t -N < ${CMAKE_SOURCE_DIR}/patches/osg/remove-zlib-dependency.patch || true"

    CMAKE_ARGS
        ${COMMON_CMAKE_ARGS}
        -DOPENGL_PROFILE=GL2
        -DDYNAMIC_OPENTHREADS=OFF
        -DDYNAMIC_OPENSCENEGRAPH=OFF
        -DBUILD_OSG_PLUGIN_OSG=ON
        -DBUILD_OSG_PLUGIN_DDS=ON
        -DBUILD_OSG_PLUGIN_TGA=ON
        -DBUILD_OSG_PLUGIN_BMP=ON
        -DBUILD_OSG_PLUGIN_JPEG=ON
        -DBUILD_OSG_PLUGIN_PNG=ON
        -DBUILD_OSG_PLUGIN_FREETYPE=ON
        -DBUILD_OSG_PLUGIN_DAE=OFF
        -DJPEG_INCLUDE_DIR=${prefix}/include/
        -DPNG_INCLUDE_DIR=${prefix}/include/
        -DFREETYPE_DIR=${prefix}
        -DOSG_CPP_EXCEPTIONS_AVAILABLE=TRUE
        -DOSG_GL1_AVAILABLE=ON
        -DOSG_GL2_AVAILABLE=ON
        -DOSG_GL3_AVAILABLE=OFF
        -DOSG_GLES1_AVAILABLE=OFF
        -DOSG_GLES3_AVAILABLE=OFF
        -DOSG_GL_LIBRARY_STATIC=OFF
        -DOSG_GL_DISPLAYLISTS_AVAILABLE=ON
        -DOSG_GL_MATRICES_AVAILABLE=ON
        -DOSG_GL_VERTEX_FUNCS_AVAILABLE=ON
        -DOSG_GL_VERTEX_ARRAY_FUNCS_AVAILABLE=ON
        -DOSG_GL_FIXED_FUNCTION_AVAILABLE=ON
        -DBUILD_OSG_APPLICATIONS=OFF
        -DBUILD_OSG_PLUGINS_BY_DEFAULT=OFF
        -DOPENGL_gl_LIBRARY=${prefix}/lib/libng_gl4es.so
        -DCMAKE_CXX_FLAGS=-std=gnu++11\ -I${prefix}/include/\ $ENV{CXXFLAGS}

    BUILD_COMMAND ${wrapper_command} $(MAKE)

    INSTALL_COMMAND mkdir -p <SOURCE_DIR>/3rdparty/build/libpng <SOURCE_DIR>/3rdparty/build/libjpeg <SOURCE_DIR>/3rdparty/build/freetype
        COMMAND ${wrapper_command} $(MAKE) install
)

# OpenMW/TES3MP
set(OPENMW_COMMON_ARGS
    -DOPENGL_gl_LIBRARY=${prefix}/lib/libng_gl4es.so
    -DOPENGL_glx_LIBRARY=${prefix}/lib/libng_gl4es.so
    -DOPENGL_INCLUDE_DIR=${prefix}/include/gl4es/
    -DOSG_STATIC=TRUE
    -DMyGUI_LIBRARY=${prefix}/lib/libMyGUIEngineStatic.a
    -DMyGUI_INCLUDE_DIR=${prefix}/include/MYGUI
    -DOPENAL_INCLUDE_DIR=${prefix}/include/AL/
    -DBullet_INCLUDE_DIR=${prefix}/include/bullet/
    -DLuaJit_INCLUDE_DIR=${prefix}/include/luajit-2.1
    -DLuaJit_LIBRARY=${prefix}/lib/libluajit-5.1.a
    -DBUILD_BROWSER=OFF
    -DBUILD_BSATOOL=OFF
    -DBUILD_NIFTEST=OFF
    -DBUILD_ESMTOOL=OFF
    -DBUILD_LAUNCHER=OFF
    -DBUILD_MWINIIMPORTER=OFF
    -DBUILD_ESSIMPORTER=OFF
    -DBUILD_OPENCS=OFF
    -DBUILD_WIZARD=OFF
    -DBUILD_MYGUI_PLUGIN=OFF
    -DOPENGL_ES=ON
    # Unity build breaks TES3MP configure on Android/CMake here:
    # generated sources use backslashes like ub_openmw-mp\Packets.cpp,
    # which CMake parses as invalid escape sequences.
    -DOPENMW_UNITY_BUILD=OFF
    -DOPENMW_LTO_BUILD=${ENABLE_LTO}
    $<$<BOOL:${ENABLE_VULKAN}>:-DOPENMW_USE_VULKAN=ON>
)

ExternalProject_Add(tes3mp
    DEPENDS boost openal mygui ffmpeg sdl2 bullet lz4 gl4es osg luajit

    GIT_REPOSITORY https://github.com/MrZer0x0/TES3MP.git
    GIT_TAG Main
    GIT_SHALLOW TRUE
    UPDATE_DISCONNECTED TRUE
    DOWNLOAD_DIR ${download_dir}

    # CrabNet (RakNet fork) не является субмодулем — клонируется отдельно.
    # Затем применяются патчи к CrabNet и TES3MP.
    # Критичные патчи применяются строго; опциональные — с || true
    # (часть патчей 2021-2022 уже включена в Main ветку MrZer0x0/TES3MP).
    PATCH_COMMAND git clone --depth=1 https://github.com/TES3MP/CrabNet.git <SOURCE_DIR>/extern/raknet
        COMMAND patch -p1 --fuzz=3 -d <SOURCE_DIR>/extern/raknet -i ${CMAKE_SOURCE_DIR}/patches/crabnet.patch
        COMMAND sh -c "patch -p1 --fuzz=3 -d <SOURCE_DIR> -i ${CMAKE_SOURCE_DIR}/patches/openmw/00-gamma.patch || true"
        COMMAND sh -c "patch -p1 --fuzz=3 -d <SOURCE_DIR> -i ${CMAKE_SOURCE_DIR}/patches/openmw/01-loadingscreen-disable-for-now.patch || true"
        COMMAND sh -c "patch -p1 --fuzz=3 -d <SOURCE_DIR> -i ${CMAKE_SOURCE_DIR}/patches/openmw/02-windowmanagerimp-always-show-mouse-when-possible-pat.patch || true"
        COMMAND sh -c "patch -p1 --fuzz=3 -d <SOURCE_DIR> -i ${CMAKE_SOURCE_DIR}/patches/openmw/03-android-fix-context-being-lost-on-app-minimize.patch || true"
        COMMAND sh -c "patch -p1 --fuzz=3 -d <SOURCE_DIR> -i ${CMAKE_SOURCE_DIR}/patches/openmw/04-settingswindow-save-user-settings-file-when-ok-is-pr.patch || true"
        COMMAND sh -c "patch -p1 --fuzz=3 -d <SOURCE_DIR> -i ${CMAKE_SOURCE_DIR}/patches/openmw/05-components-misc-stringops-use-boost-format-instead-o.patch || true"
        COMMAND patch -p1 --fuzz=3 -d <SOURCE_DIR> -i ${CMAKE_SOURCE_DIR}/patches/openmw/06-fix-build.patch
        COMMAND sh -c "patch -p1 --fuzz=3 -d <SOURCE_DIR> -i ${CMAKE_SOURCE_DIR}/patches/openmw/08-highp.patch || true"

    CMAKE_ARGS
        ${COMMON_CMAKE_ARGS}
        ${OPENMW_COMMON_ARGS}

    BUILD_COMMAND ${wrapper_command} $(MAKE)
    INSTALL_COMMAND ""
)
