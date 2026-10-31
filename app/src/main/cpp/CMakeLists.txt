cmake_minimum_required(VERSION 3.22.1)
project(PocketQwenNative LANGUAGES C CXX)

if(NOT EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp/CMakeLists.txt")
    message(FATAL_ERROR "llama.cpp source missing. CI must checkout ggml-org/llama.cpp into app/src/main/cpp/llama.cpp before Gradle build.")
endif()

set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_COMMON OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE) # Cross-compile for Android; never tune to the CI host CPU.
set(GGML_CPU_KLEIDIAI ON CACHE BOOL "" FORCE) # Optional runtime-dispatched ARM kernels for arm64-v8a.
set(LLAMA_OPENSSL OFF CACHE BOOL "" FORCE) # Keep the Android native build independent of OpenSSL.
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_VULKAN OFF CACHE BOOL "" FORCE) # GPU backend is a separate opt-in change; CPU remains the safe baseline.
set(GGML_LLAMAFILE OFF CACHE BOOL "" FORCE)
add_subdirectory(llama.cpp)

add_library(pocketqwen_native SHARED native_inference.cpp)
target_compile_features(pocketqwen_native PRIVATE cxx_std_17)
target_compile_options(pocketqwen_native PRIVATE -O3)
target_link_libraries(pocketqwen_native PRIVATE llama android log)
