cmake_minimum_required(VERSION 3.22.1)

project("pulse-emulator")

# QEMU integration - download and compile
include(ExternalProject)

set(QEMU_VERSION "8.2.0")
set(QEMU_URL "https://download.qemu.org/qemu-${QEMU_VERSION}.tar.xz")
set(QEMU_SOURCE_DIR "${CMAKE_BINARY_DIR}/qemu-src")

# Download QEMU source
ExternalProject_Add(
    qemu_download
    URL ${QEMU_URL}
    SOURCE_DIR ${QEMU_SOURCE_DIR}
    CONFIGURE_COMMAND ""
    BUILD_COMMAND ""
    INSTALL_COMMAND ""
)

# Configure QEMU for x86 emulation
set(QEMU_BUILD_DIR "${CMAKE_BINARY_DIR}/qemu-build")

ExternalProject_Add(
    qemu_build
    DEPENDS qemu_download
    SOURCE_DIR ${QEMU_SOURCE_DIR}
    BINARY_DIR ${QEMU_BUILD_DIR}
    CONFIGURE_COMMAND ${QEMU_SOURCE_DIR}/configure
        --target-list=i386-softmmu
        --disable-werror
        --disable-docs
        --disable-linux-user
        --disable-system
        --enable-system
        --disable-user
        --disable-gtk
        --disable-sdl
        --disable-vnc
        --disable-spice
        --disable-virglrenderer
        --enable-opengl
        --audio-drv-list=alsa,oss
        --prefix=${CMAKE_INSTALL_PREFIX}
    BUILD_COMMAND make -j4
    INSTALL_COMMAND make install
)

# Link QEMU libraries
find_library(QEMU_CORE qemu-core PATHS ${QEMU_BUILD_DIR})
find_library(QEMU_I386 qemu-i386 PATHS ${QEMU_BUILD_DIR})

add_library(pulse-emulator SHARED
    src/main/cpp/emulator_bridge.cpp
    src/main/cpp/cpu_emulation.cpp
    src/main/cpp/graphics_bridge.cpp
    src/main/cpp/audio_bridge.cpp
    src/main/cpp/memory_manager.cpp
)

target_link_libraries(pulse-emulator
    ${QEMU_CORE}
    ${QEMU_I386}
    android
    log
    GLESv2
    OpenSLES
)

target_include_directories(pulse-emulator PRIVATE
    ${QEMU_SOURCE_DIR}/include
    ${QEMU_BUILD_DIR}
)
