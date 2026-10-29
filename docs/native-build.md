# PocketQwen native build (stage 1)

## Pinned dependency

The required upstream llama.cpp revision is stored in `native/llama.cpp.commit`.
It is a full 40-character commit SHA from the official repository
[ggml-org/llama.cpp](https://github.com/ggml-org/llama.cpp).

Do not replace this with `master`, a floating tag, or a fabricated hash. To
upgrade llama.cpp, choose a real upstream commit, update the pin, then verify
the Android native build in GitHub Actions.

## CI behavior

The Android workflow initializes a small Git repository under the runner's
temporary directory and fetches only the pinned commit with `--depth=1`.
It retries the fetch at most three times for transient connection failures and
fails if the commit cannot be fetched or if the checked-out SHA differs from
the pin. It does not use a Git submodule or silently ignore checkout errors.

The workflow cross-compiles the upstream CPU libraries for Android
`arm64-v8a` with the Android NDK and CMake. It disables optional executables,
tests, server components, OpenMP, llamafile and OpenSSL. The resulting shared
libraries are staged into `app/src/main/jniLibs/arm64-v8a` in the CI workspace
so Gradle packages the libraries into the debug APK. Generated native binaries
are not committed to Git.

## Local native build

A full local native build requires a checkout of the exact pinned source.
Because the source directory is intentionally not tracked by this repository,
the root CMake file fails with an actionable message when it is missing.

In PowerShell, from the project root, use a **new, empty** source directory:

```powershell
$commit = (Get-Content .\native\llama.cpp.commit -Raw).Trim()
$source = Join-Path $PWD "third_party\llama.cpp"
if (Test-Path $source) {
    throw "Source directory already exists; inspect it before removing or reusing it."
}
New-Item -ItemType Directory -Force (Split-Path $source) | Out-Null
git init $source
git -C $source remote add origin https://github.com/ggml-org/llama.cpp.git
git -C $source fetch --depth=1 origin $commit
git -C $source checkout --detach FETCH_HEAD
git -C $source rev-parse HEAD
```

The reported SHA must match the text in `native/llama.cpp.commit`. If your
connection resets, stop and use the remote GitHub Actions build rather than
repeatedly deleting Git metadata.

## Current scope and limitation

The workflow fetches the pinned source and builds its CPU shared libraries plus
PocketQwen's JNI bridge for arm64-v8a. The APK contains those libraries and the
Kotlin repository exposes a local GGUF model picker. The app copies selected
GGUFs to app-private storage and remembers the selected path.

Real inference is connected through the pinned llama.cpp C API. The initial
implementation uses a conservative 1,024-token context and up to 160 generated
tokens; each request resets the native KV memory and currently uses the current
user message only (it does not yet feed earlier chat turns back into the model).
The model weights are not bundled into the APK. Device testing with an actual
GGUF file is still required before claiming validated performance on a Redmi A5.
