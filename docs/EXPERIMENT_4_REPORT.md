# PocketQwen Experiment 4 — manual test build

## Purpose
This isolated branch changes the inference thread setting from a maximum of four threads to all CPU threads reported by `std::thread::hardware_concurrency()`. It keeps the inference context at **1024 tokens**, matching the requested Termux comparison setting.

## Changes
- Uses the runtime-reported CPU thread count for both `n_threads` and `n_threads_batch`, with a minimum of one if the runtime reports zero.
- Keeps `n_ctx = 1024`.
- Preserves first-token-immediate streaming and the Experiment 3 Kotlin/SQLite persistence improvements.
- Adds a dedicated Release APK artifact named `pocketqwen-experiment-4-release-apk`.

## Important caveats
- Context size is the model's token window, **not** a general Android memory-management setting. A 1024 context can reduce KV-cache memory relative to larger contexts, but it does not control all app memory.
- "All available threads" means the CPU count reported to the app at runtime; it does not guarantee every physical core is continuously busy.
- More threads may improve throughput, or may be slower due to big.LITTLE scheduling, memory bandwidth, and thermal throttling. No speedup is claimed until it is measured on the Redmi A5.
- This is a debug-key-signed experiment Release APK for manual testing only, not a production release.

## Manual test
1. Download the `pocketqwen-experiment-4-release-apk` artifact from the Experiment 4 GitHub Actions run.
2. Install it on the Redmi A5. Keep the same Qwen2.5-0.5B-Instruct Q4_K_M model, prompt, power mode, and starting thermal state as the Termux test.
3. Confirm startup logs include `inference_threads=` and `context=1024`:
   ```sh
   adb logcat -c
   adb logcat -s PocketQwenNative:I
   ```
4. Compare at least three runs against Termux using context 1024 and automatic/all-thread inference. Record first-token latency, sampled tokens/sec, total response time, memory pressure, and whether sustained use causes thermal slowdown.
5. Stop if the app crashes, becomes unstable, or the phone gets excessively hot. Let it cool before repeating.

## Build status
The branch and source changes are committed. CI must finish successfully before an APK artifact is available. A successful build validates compilation/tests, not real-device speed.