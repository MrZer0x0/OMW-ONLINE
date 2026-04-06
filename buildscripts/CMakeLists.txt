cmake_minimum_required(VERSION 3.15.0)

# Установка политик CMake
cmake_policy(SET CMP0091 NEW)
cmake_policy(SET CMP0074 NEW)

# Определение версий и хешей зависимостей
set(LIBJPEG_TURBO_VERSION "3.0.0")
set(LIBJPEG_TURBO_HASH SHA256=c77c65fcce3d33417b2e90432e7a0eb05f59a7fff884022a9d931775d583bfaa)

set(LIBPNG_VERSION "1.6.40")
set(LIBPNG_HASH SHA256=8f720b363aa08683c9bf2a563236f45313af2c55d542b5481ae17dd8d183bb42)

set(FREETYPE2_VERSION "2.13.2")
set(FREETYPE2_HASH SHA256=1ac27e16c134a7f2ccea177faba19801131116fd682efc1f5737037c5db224b5)

set(OPENAL_VERSION "1.23.1")
set(OPENAL_HASH SHA256=796f4b89134c4e57270b7f0d755f0fa3435b90da437b745160a49bd41c845b21)

set(BOOST_VERSION "1.83.0")
set(BOOST_HASH SHA256=6478edfe2f3305127cffe8caf73ea0176c53769f4bf1585be237eb30798c3b8e)

set(FFMPEG_VERSION "6.1")
set(FFMPEG_HASH SHA256=488c76e57dd9b3bee901f71d5c95eaf1db4a5a31fe46a28654e837144207c270)

set(SDL2_VERSION "2.28.5")
set(SDL2_HASH SHA256=332cb37d0be20cb9541739c61f79bae5a477427d79ae85e352089afdaf6666e4)

set(BULLET_VERSION "3.25")
set(BULLET_HASH SHA256=c45afb6399e3f68036ddb641c6e474b5f54f21c91350c11dbf91f861b0d6da61)

set(MYGUI_VERSION "3.4.1")
set(MYGUI_HASH SHA256=072d5dd2e0636fa717dc856a573a451b3f6ae8c8da927c7ebf26d5c41cd5f4d3)

set(GL4ES_VERSION "1.1.4")
set(GL4ES_HASH SHA256=b565e717c7d192e936bda25f3cb90ad8db398af56414ec08294b6716574c1a6d)

set(LZ4_VERSION "1.9.4")
set(LZ4_HASH SHA256=0b0e3aa07c8c063ddf40b082bdf7e37a1562bda40a0ff5272957f3e987e0e54b)

set(TES3MP_VERSION "0.8.1")
set(TES3MP_HASH SHA256=5f809dfb47586c8d7e6efe12eb2c32b5fc69dfa68f1d13962dda950168727d75)

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
ExternalProject_Add(libpng
    DEPENDS libjpeg-turbo
    URL https://download.sourceforge.net/libpng/libpng-${LIBPNG_VERSION}.tar.xz
    URL_HASH ${LIBPNG_HASH}
    DOWNLOAD_DIR ${download_dir}

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
ExternalProject_Add(sdl2
    URL https://github.com/libsdl-org/SDL/releases/download/release-${SDL2_VERSION}/SDL2-${SDL2_VERSION}.tar.gz
    URL_HASH ${SDL2_HASH}
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
        -DMYGUI_RENDERSYSTEM=ON
        -DMYGUI_BUILD_DEMOS=OFF
        -DMYGUI_BUILD_TOOLS=OFF
        -DMYGUI_BUILD_PLUGINS=OFF
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

    BUILD_COMMAND ${wrapper_command} $(MAKE)
    INSTALL_COMMAND ${wrapper_command} $(MAKE) install
)

# GL4ES
ExternalProject_Add(gl4es
    URL https://github.com/ptitSeb/gl4es/archive/${GL4ES_VERSION}.tar.gz
    URL_HASH ${GL4ES_HASH}
    DOWNLOAD_DIR ${download_dir}

    CONFIGURE_COMMAND ""
    
    BUILD_COMMAND ${wrapper_command} ndk-build
    ${COMMON_NDK_BUILD_FLAGS}

    INSTALL_COMMAND mkdir -p ${prefix}/lib/
    COMMAND cp libs/${app_abi}/libGL.so ${prefix}/lib/
    COMMAND cp -r <SOURCE_DIR>/include ${prefix}/
)

# OpenMW/TES3MP
set(OPENMW_COMMON_ARGS
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
    -DOPENMW_UNITY_BUILD=ON
    -DOPENMW_LTO_BUILD=${ENABLE_LTO}
    $<$<BOOL:${ENABLE_VULKAN}>:-DOPENMW_USE_VULKAN=ON>
)

ExternalProject_Add(tes3mp
    DEPENDS boost openal mygui ffmpeg sdl2 bullet lz4 gl4es

    GIT_REPOSITORY https://github.com/MrZer0x0/TES3MP.git
    GIT_TAG Main
    GIT_SHALLOW TRUE
    UPDATE_DISCONNECTED TRUE
    DOWNLOAD_DIR ${download_dir}

    CMAKE_ARGS
        ${COMMON_CMAKE_ARGS}
        ${OPENMW_COMMON_ARGS}

    BUILD_COMMAND ${wrapper_command} $(MAKE)
    INSTALL_COMMAND ""
)
