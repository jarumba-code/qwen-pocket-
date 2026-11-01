# Experiment 3 — performance build report

## Goal
Keep the Kotlin/Jetpack Compose UI and private SQLite history while building an optimized Android NDK/llama.cpp inference backend. This branch is isolated from `main`.

## Changes in this experiment
- CI builds the **Release** variant rather than using the Debug APK as the performance candidate. Native inference speed measured from a debug build is not a fair performance baseline.
- The release APK is signed with the Android debug key only so it can be installed for local testing. This is an experiment artifact, **not a production signing setup**.
- Native inference selects a conservative maximum of four CPU threads (bounded by the runtime-reported CPU count) for both generation and prompt-batch evaluation. This is a hypothesis for a low-memory mobile device, not a proven optimum; compare 2 vs 4 threads on the target phone.
- JNI streaming sends the first generated token immediately, then batches subsequent callbacks in groups of four to reduce JNI/Flow callback overhead while avoiding an artificial four-token delay before the response starts.
- Conversation history persistence now checkpoints on a 750 ms cadence on `Dispatchers.IO`, instead of rewriting the SQLite conversation/message tables on every streamed UI update. This reduces storage work and main-thread blocking during generation.
- Existing native timing logs record model-load time, context initialization, prompt tokenization/prefill, first sample/callback, callback overhead, decode time, and sampled tokens/sec.
- Existing native engine pin, GGUF SHA-256 verification, arm64-v8a ABI, OpenMP setting, and CPU-only backend remain unchanged.

## What this does and does not establish
A successful CI build establishes that the code compiles and the unit tests pass. It does **not** prove a speedup on a Redmi A5. No physical Redmi A5 is attached to CI, so real device throughput, memory pressure, and thermal throttling still require measurement. Do not describe the build as "way faster" until the controlled comparison confirms it.

## Install and benchmark
1. Download the `pocketqwen-experiment-3-release-apk` artifact from the branch's latest successful GitHub Actions run.
2. Install it on the Redmi A5 and verify the exact model file is Qwen2.5-0.5B-Instruct Q4_K_M with SHA-256 `74a4da8c9fdbcd15bd1f6d01d621410d31c6fc00986f5eb687824e7b93d7a9db`.
3. Use the same prompt, generation limit, model hash, context size, device power mode, and thermal starting state in Termux and the APK. Run each test three times after one warm-up, alternating which implementation runs first.
4. Capture `PocketQwenNative` logcat lines. Record TTFT (request start to first callback), generated tokens/sec, total latency, model-load time, and Android memory usage. Ignore model-load time when comparing steady-state generation.
5. Repeat with 2 and 4 inference threads (code change + rebuild) before considering a higher thread count. More threads can hurt on a thermally constrained big.LITTLE phone.
6. Compare the median of the three measured runs. If native tokens/sec is similar but the UI looks slower, profile Compose recomposition and scrolling; if native tokens/sec is lower, investigate thread count, compile flags, memory pressure, and thermal state first.

Example device log capture:
```bash
adb logcat -c
adb logcat -s PocketQwenNative:I
```

## Known limitations / next investigations
- This experiment does not bundle the Termux Linux distribution. It compiles the pinned llama.cpp source for Android's NDK/Bionic ABI; Termux binaries are not generally portable into an Android app sandbox.
- The four-thread cap is a safe test baseline, not a universal optimum.
- Conversation history and prompt-template parity with the Termux command must be held constant in a valid benchmark. If Termux sends prior turns while the app sends only the latest message, compare equivalent single-turn prompts first.
- The UI/database path is separate from native token-generation speed. The main screen now batches full-history SQLite checkpoints off the main thread every 750 ms. This bounds persistence frequency but still rewrites the saved snapshot; if very large histories remain costly, the next step is a differential upsert API for only the changed conversation/message.
