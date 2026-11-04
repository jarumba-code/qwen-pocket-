# PocketQwen Termux-runtime backend experiment

## Purpose

Create a dedicated experimental Android build that reproduces the useful parts of the working Termux `llama.cpp` runtime while keeping the product UI native Kotlin/Jetpack Compose.

This is **not** an attempt to bundle the Termux Android app, shell, or its entire package manager. Termux is a Linux userland distribution; copying the full distribution into an APK would add substantial size and maintenance cost and would not automatically improve inference. Instead, this build pins and packages the inference engine and only the native runtime dependencies and build options that are both required and legally redistributable.

## Architecture contract

- **Frontend:** Kotlin + Jetpack Compose. No shell or terminal UI is exposed to users.
- **Inference backend:** native C/C++ library built with the Android NDK and CMake, linked into the app and invoked through JNI. It must run in-process; no localhost server, Termux installation, root, or network connection is required.
- **Engine source:** pin `ggml-org/llama.cpp` to the tested reference revision `9125a2ebe8e5a38f0f4232170c134a29ed730b66` until a measured upgrade is approved.
- **Target ABI:** `arm64-v8a` for the first Redmi A5 build. Add other ABIs only after CI and device tests.
- **Build profile:** Release/`-O3`; `GGML_NATIVE=OFF` for Android cross-compilation; experimentally enable OpenMP and LLAMAFILE and disable KleidiAI to match the reference Termux build. Keep optional GPU backends disabled until they are separately implemented and measured. These options are hypotheses to benchmark, not proof of speed improvement.
- **Model:** Qwen2.5-0.5B-Instruct GGUF Q4_K_M, stored in app-private storage after a verified first-run copy. Do not silently fetch model data at runtime.
- **Streaming:** native token callbacks -> Kotlin callback/Flow -> ViewModel -> one assistant message bubble. Append chunks as they arrive; don't wait for the full completion.
- **Persistence:** SQLite in app-private storage. Conversations and messages use separate relational tables, stable IDs, ordering, timestamps, and message status. Persist the user's prompt immediately and checkpoint the assistant's partial text during generation (throttled, e.g. every 250–500 ms and on completion/cancel/error) so a process death does not discard a long response. Never perform database writes on the main thread.
- **Privacy:** prompts and generated tokens remain on-device. No analytics, cloud inference, telemetry payloads, or external server dependency in this experiment.

## Termux parity: what to reuse

Reuse the exact tested `llama.cpp` revision and compare its relevant compile-time configuration against the Termux reference. Reproduce the useful native compiler/runtime settings that are valid for Android NDK. Do **not** copy Termux's Linux executables, glibc-linked binaries, shell commands, or package database into the APK: Android uses Bionic and an Android app sandbox, so Linux binaries built for the Termux userland are not drop-in Android app libraries.

Keep a written record of all CMake cache values and NDK version in CI artifacts. Do not claim parity merely because CMake accepts an option; verify the final cache and runtime logs.

## Required acceptance gates

1. **Build:** CI builds the debug APK and unit tests pass for this branch.
2. **Native smoke test:** load the GGUF model and generate a short response on an ARM64 Android device without network access.
3. **Streaming:** first visible token is delivered before completion; UI remains responsive; cancellation stops generation.
4. **SQLite:** create/reopen/delete conversations; preserve ordering; persist partial response on cancellation; survive process restart; migrate legacy history without data loss.
5. **Resource limits:** record cold model-load time, first-token latency, steady-state tokens/sec, peak memory/RSS, thermal behavior, and crash rate on the Redmi A5. Test repeated generations and low-memory conditions.
6. **Performance comparison:** benchmark this build against the current Termux command on the same phone, same model file/hash, same prompt, context, thread count, and thermal state. Run at least three repetitions and report median values.
7. **Merge gate:** do not merge into the stable branch unless correctness, persistence, stability, and measured performance are acceptable. A successful CI compile alone is insufficient.

## Current repository baseline

The experimental parent already contains a JNI-based `NativeAiRepository`, token callback streaming, and a SQLite-backed `ConversationStore`. Before calling this experiment complete, inspect the ViewModel-to-store wiring: verify that partial streamed text is saved incrementally and that an in-flight assistant message is recovered cleanly after process death. Keep the existing stable branch unchanged.

## Initial build commands

```bash
./gradlew testDebugUnitTest assembleDebug --no-daemon
```

The CI workflow downloads the pinned engine and model. Device validation still requires installing the resulting APK on an ARM64 Android phone and collecting actual runtime measurements.
