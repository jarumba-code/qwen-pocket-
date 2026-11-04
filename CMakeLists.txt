cmake_minimum_required(VERSION 3.18)

project(PocketQwenNative LANGUAGES C CXX)

# ---- llama.cpp pinned commit ------------------------------------------------
# Pin to a specific commit for reproducibility. Update this SHA when upgrading.
set(LLAMA_COMMIT "d1b2a1e4e9c7a5b3d1e2f3a4b5c6d7e8f9a0b1c2" CACHE STRING "llama.cpp git commit")

set(LLAMA_DIR "${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp")
set(LLAMA_BUILD_DIR "${CMAKE_BINARY_DIR}/llama.cpp")

# Fetch llama.cpp at configure time. Requires network access on the CI runner.
include(FetchContent)
FetchContent_Declare(
    llama_cpp
    GIT_REPOSITORY https://github.com/ggerganov/llama.cpp.git
    GIT_TAG ${LLAMA_COMMIT}
    GIT_SHALLOW TRUE
    GIT_SHALLOW_SUBMODULES TRUE
)
FetchContent_GetProperties(llama_cpp)
if(NOT llama_config_POPULATED)
    message(FATAL_ERROR "Failed to fetch llama.cpp from GitHub. Check network connectivity and the pinned commit ${LLAMA_COMMIT}.")
endif()
FetchContent_Populate(llama_cpp)

# ---- Android NDK configuration ---------------------------------------------
if(NOT ANDROID_NDK)
    message(FATAL_ERROR "ANDROID_NDK is not defined. Set it to your NDK root directory.")
endif()

set(ANDROID_ABI "${ANDROID_ABI}" CACHE STRING "Target Android ABI" FORCE)
if(NOT ANDROID_ABI STREQUAL "arm64-v8a" AND NOT ANDROID_ABI STREQUAL "armeabi-v7a")
    message(FATAL_ERROR "Unsupported ANDROID_ABI '${ANDROID_ABI}'. Use arm64-v8a or armeabi-v7a.")
endif()

set(ANDROID_PLATFORM "android-24" CACHE STRING "Minimum Android API level")

include(android.toolchain.cmake)

# ---- Build llama.cpp native library -----------------------------------------
add_library(llama STATIC
    ${LLAMA_DIR}/ggml.c
    ${LLAMA_DIR}/ggml-alloc.c
    ${LLAMA_DIR}/ggml-quant.c
    ${LLAMA_DIR}/ggml-backend.c
    ${LLAMA_DIR}/llama.cpp
)

target_include_directories(llama PRIVATE
    ${LLAMA_DIR}
)

target_compile_options(llama PRIVATE
    -O3
    -DNDEBUG
    -DCMAKE_BUILD_TYPE=Release
)

target_link_options(llama PRIVATE
    -llog
)

# ---- PocketQwen native library (JNI bridge) ---------------------------------
add_library(pocketqwen_native SHARED
    src/main/cpp/native_inference.cpp
)

target_include_directories(pocketqwen_native PRIVATE
    ${LLAMA_DIR}
    ${ANDROID_NDK}/sources/android/native_app_glue
)

target_link_libraries(pocketqwen_native PRIVATE
    llama
    android
    log
)

# Set visibility to hidden except JNI entry points
set_target_properties(pocketqwen_native PROPERTIES
    CXX_VISIBILITY_PRESET hidden
    CXX_VISIBILITY_PRESET hidden
)

# Output APK-ready libraries
set_target_properties(pocketqwen_native PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/libs/${ANDROID_ABI}"
)
