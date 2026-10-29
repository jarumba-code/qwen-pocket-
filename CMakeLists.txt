cmake_minimum_required(VERSION 3.22.1)

project(PocketQwenNative LANGUAGES C CXX)

# The source is fetched on CI (or prepared locally) at the full SHA in
# native/llama.cpp.commit. This avoids a recursive/local submodule clone.
set(LLAMA_CPP_DEFAULT_SOURCE_DIR "${CMAKE_CURRENT_LIST_DIR}/third_party/llama.cpp")
set(LLAMA_CPP_SOURCE_DIR "${LLAMA_CPP_DEFAULT_SOURCE_DIR}" CACHE PATH
    "Path to the pinned llama.cpp source checkout")

set(LLAMA_CPP_PIN_FILE "${CMAKE_CURRENT_LIST_DIR}/native/llama.cpp.commit")
if(NOT EXISTS "${LLAMA_CPP_PIN_FILE}")
    message(FATAL_ERROR "Missing llama.cpp pin file: ${LLAMA_CPP_PIN_FILE}")
endif()
file(READ "${LLAMA_CPP_PIN_FILE}" LLAMA_CPP_PIN)
string(STRIP "${LLAMA_CPP_PIN}" LLAMA_CPP_PIN)
string(LENGTH "${LLAMA_CPP_PIN}" LLAMA_CPP_PIN_LENGTH)
if(NOT LLAMA_CPP_PIN_LENGTH EQUAL 40 OR NOT LLAMA_CPP_PIN MATCHES "^[0-9a-f]+$")
    message(FATAL_ERROR "native/llama.cpp.commit must contain one full 40-character lowercase Git SHA.")
endif()

if(NOT EXISTS "${LLAMA_CPP_SOURCE_DIR}/CMakeLists.txt")
    message(FATAL_ERROR
        "Pinned llama.cpp source is missing at '${LLAMA_CPP_SOURCE_DIR}'. "
        "See docs/native-build.md for the exact pinned-source setup. "
        "GitHub Actions fetches the source into RUNNER_TEMP automatically.")
endif()

execute_process(
    COMMAND git -C "${LLAMA_CPP_SOURCE_DIR}" rev-parse HEAD
    RESULT_VARIABLE LLAMA_CPP_GIT_RESULT
    OUTPUT_VARIABLE LLAMA_CPP_CHECKED_OUT_SHA
    OUTPUT_STRIP_TRAILING_WHITESPACE
    ERROR_QUIET
)
if(NOT LLAMA_CPP_GIT_RESULT EQUAL 0)
    message(FATAL_ERROR
        "Cannot verify the llama.cpp checkout at '${LLAMA_CPP_SOURCE_DIR}'. "
        "It must be a Git checkout at the exact SHA in native/llama.cpp.commit.")
endif()
if(NOT LLAMA_CPP_CHECKED_OUT_SHA STREQUAL LLAMA_CPP_PIN)
    message(FATAL_ERROR
        "llama.cpp revision mismatch: expected ${LLAMA_CPP_PIN}, found ${LLAMA_CPP_CHECKED_OUT_SHA}. "
        "Fetch the exact pinned SHA described in docs/native-build.md.")
endif()

if(NOT ANDROID)
    message(FATAL_ERROR "Configure this project with the Android NDK CMake toolchain.")
endif()
if(NOT ANDROID_ABI STREQUAL "arm64-v8a")
    message(FATAL_ERROR "Stage 1 supports only arm64-v8a; received '${ANDROID_ABI}'.")
endif()

message(STATUS "PocketQwen: building llama.cpp ${LLAMA_CPP_PIN} for ${ANDROID_ABI}")

# Keep this first-stage build CPU-only, portable, and focused on libraries.
# Compile the app JNI bridge against headers and targets from the exact pinned revision.
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(BUILD_SHARED_LIBS ON CACHE BOOL "Build shared libraries" FORCE)
set(LLAMA_BUILD_COMMON OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_APP OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_UI OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_MTMD OFF CACHE BOOL "" FORCE)
set(LLAMA_OPENSSL OFF CACHE BOOL "" FORCE)
set(LLAMA_SUBPROCESS OFF CACHE BOOL "" FORCE)
set(LLAMA_CONNECT OFF CACHE BOOL "" FORCE)

set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE OFF CACHE BOOL "" FORCE)
set(GGML_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(GGML_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(GGML_CUDA OFF CACHE BOOL "" FORCE)
set(GGML_METAL OFF CACHE BOOL "" FORCE)
set(GGML_VULKAN OFF CACHE BOOL "" FORCE)
set(GGML_SYCL OFF CACHE BOOL "" FORCE)
set(GGML_OPENVINO OFF CACHE BOOL "" FORCE)
set(GGML_RPC OFF CACHE BOOL "" FORCE)

add_subdirectory("${LLAMA_CPP_SOURCE_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/llama.cpp")


find_library(ANDROID_LOG_LIBRARY log REQUIRED)

add_library(pocketqwen_native SHARED
    "${CMAKE_CURRENT_LIST_DIR}/app/src/main/cpp/native_inference.cpp"
)
target_include_directories(pocketqwen_native PRIVATE
    "${LLAMA_CPP_SOURCE_DIR}/include"
    "${LLAMA_CPP_SOURCE_DIR}/ggml/include"
)
target_compile_features(pocketqwen_native PRIVATE cxx_std_17)
target_compile_options(pocketqwen_native PRIVATE -O2 -fvisibility=hidden)
target_link_libraries(pocketqwen_native PRIVATE llama ${ANDROID_LOG_LIBRARY})
set_target_properties(pocketqwen_native PROPERTIES
    OUTPUT_NAME "pocketqwen_native"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/pocketqwen-jni/${ANDROID_ABI}"
)
