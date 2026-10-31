#include <jni.h>
#include <android/log.h>
#include <algorithm>
#include <atomic>
#include <chrono>
#include <limits>
#include <string>
#include <vector>
#include <mutex>
#include "llama.h"

#define LOG_TAG "PocketQwenNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static std::mutex g_mutex;
static std::mutex g_load_mutex;
static llama_model *g_model = nullptr;
static llama_context *g_ctx = nullptr;
static llama_sampler *g_sampler = nullptr;
static bool g_initialized = false;
static std::atomic<bool> g_cancel_requested{false};

static void release_locked() {
    if (g_sampler) { llama_sampler_free(g_sampler); g_sampler = nullptr; }
    if (g_ctx) { llama_free(g_ctx); g_ctx = nullptr; }
    if (g_model) { llama_model_free(g_model); g_model = nullptr; }
    g_initialized = false;
}

// Return the longest prefix that consists of complete, valid UTF-8 code points.
static size_t complete_utf8_prefix(const std::string &s) {
    size_t i = 0, last_complete = 0;
    while (i < s.size()) {
        const unsigned char c = static_cast<unsigned char>(s[i]);
        size_t width = 0;
        if (c <= 0x7F) width = 1;
        else if (c >= 0xC2 && c <= 0xDF) width = 2;
        else if (c >= 0xE0 && c <= 0xEF) width = 3;
        else if (c >= 0xF0 && c <= 0xF4) width = 4;
        else { ++i; last_complete = i; continue; }
        if (i + width > s.size()) break;
        bool valid = true;
        for (size_t j = 1; j < width; ++j) {
            const unsigned char cc = static_cast<unsigned char>(s[i + j]);
            if ((cc & 0xC0) != 0x80) { valid = false; break; }
        }
        if (!valid) { ++i; last_complete = i; continue; }
        if (width == 3) {
            const unsigned char c1 = static_cast<unsigned char>(s[i + 1]);
            if ((c == 0xE0 && c1 < 0xA0) || (c == 0xED && c1 >= 0xA0)) { ++i; last_complete = i; continue; }
        } else if (width == 4) {
            const unsigned char c1 = static_cast<unsigned char>(s[i + 1]);
            if ((c == 0xF0 && c1 < 0x90) || (c == 0xF4 && c1 > 0x8F)) { ++i; last_complete = i; continue; }
        }
        i += width;
        last_complete = i;
    }
    return last_complete;
}

static jstring utf8_to_jstring(JNIEnv *env, const std::string &utf8) {
    std::vector<jchar> utf16;
    utf16.reserve(utf8.size());
    for (size_t i = 0; i < utf8.size();) {
        const unsigned char c = static_cast<unsigned char>(utf8[i]);
        uint32_t cp = 0;
        size_t width = 1;
        if (c <= 0x7F) cp = c;
        else if ((c & 0xE0) == 0xC0 && i + 1 < utf8.size()) {
            cp = ((c & 0x1F) << 6) | (static_cast<unsigned char>(utf8[i + 1]) & 0x3F); width = 2;
        } else if ((c & 0xF0) == 0xE0 && i + 2 < utf8.size()) {
            cp = ((c & 0x0F) << 12) | ((static_cast<unsigned char>(utf8[i + 1]) & 0x3F) << 6) | (static_cast<unsigned char>(utf8[i + 2]) & 0x3F); width = 3;
        } else if ((c & 0xF8) == 0xF0 && i + 3 < utf8.size()) {
            cp = ((c & 0x07) << 18) | ((static_cast<unsigned char>(utf8[i + 1]) & 0x3F) << 12) | ((static_cast<unsigned char>(utf8[i + 2]) & 0x3F) << 6) | (static_cast<unsigned char>(utf8[i + 3]) & 0x3F); width = 4;
        } else { cp = 0xFFFD; }
        i += width;
        if (cp <= 0xFFFF) utf16.push_back(static_cast<jchar>(cp));
        else {
            cp -= 0x10000;
            utf16.push_back(static_cast<jchar>(0xD800 + (cp >> 10)));
            utf16.push_back(static_cast<jchar>(0xDC00 + (cp & 0x3FF)));
        }
    }
    return env->NewString(utf16.data(), static_cast<jsize>(utf16.size()));
}

static jboolean nativeLoadModel(JNIEnv *env, jobject, jstring path) {
    if (!path) return JNI_FALSE;
    std::lock_guard<std::mutex> loadLock(g_load_mutex);
    const char *p = env->GetStringUTFChars(path, nullptr);
    if (!p) return JNI_FALSE;
    std::lock_guard<std::mutex> lock(g_mutex);
    release_locked();
    llama_backend_init();
    // Record the actual compiled/runtime backend capabilities on-device. This
    // helps verify whether the ARM-optimized kernels are present in the APK.
    const char *systemInfo = llama_print_system_info();
    LOGI("llama_system_info=%s", systemInfo ? systemInfo : "unavailable");
    llama_model_params mp = llama_model_default_params();
    mp.n_gpu_layers = 0;
    LOGI("Loading model; cpu_threads=4 context=1024 batch=128 gpu_layers=0");
    const auto start = std::chrono::steady_clock::now();
    const auto modelLoadStart = std::chrono::steady_clock::now();
    g_model = llama_model_load_from_file(p, mp);
    const auto modelLoadEnd = std::chrono::steady_clock::now();
    env->ReleaseStringUTFChars(path, p);
    if (!g_model) { LOGE("Model load failed; verify GGUF integrity and available RAM"); return JNI_FALSE; }
    llama_context_params cp = llama_context_default_params();
    cp.n_ctx = 1024;
    cp.n_batch = 128;
    cp.n_threads = 4;
    cp.n_threads_batch = 4;
    const auto contextInitStart = std::chrono::steady_clock::now();
    g_ctx = llama_init_from_model(g_model, cp);
    const auto contextInitEnd = std::chrono::steady_clock::now();
    if (!g_ctx) { release_locked(); LOGE("Context creation failed; likely memory pressure"); return JNI_FALSE; }
    g_sampler = llama_sampler_init_greedy();
    if (!g_sampler) { release_locked(); return JNI_FALSE; }
    g_initialized = true;
    const auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::steady_clock::now() - start).count();
    LOGI("Model and context ready model_load_ms=%.3f context_init_ms=%.3f total_init_ms=%lld",
         std::chrono::duration<double, std::milli>(modelLoadEnd - modelLoadStart).count(),
         std::chrono::duration<double, std::milli>(contextInitEnd - contextInitStart).count(),
         static_cast<long long>(ms));
    return JNI_TRUE;
}

static jboolean nativeIsModelAvailable(JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    return g_initialized ? JNI_TRUE : JNI_FALSE;
}

static jstring nativeGetModelStatus(JNIEnv *env, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    return env->NewStringUTF(g_initialized ? "loaded" : "not_loaded");
}

static void nativeCancelGeneration(JNIEnv *, jobject) {
    g_cancel_requested.store(true);
    LOGI("Cancellation requested");
}

static void nativeGenerateResponse(JNIEnv *env, jobject, jstring input, jobject callback) {
    if (!input || !callback) return;
    const char *raw = env->GetStringUTFChars(input, nullptr);
    if (!raw) return;
    std::string prompt = std::string("<|im_start|>system\nYou are PocketQwen, a helpful offline assistant. Be clear and concise.\n<|im_end|>\n<|im_start|>user\n") + raw + "\n<|im_end|>\n<|im_start|>assistant\n";
    env->ReleaseStringUTFChars(input, raw);
    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_initialized || !g_model || !g_ctx || !g_sampler) {
        jclass ex = env->FindClass("java/lang/IllegalStateException");
        if (ex) env->ThrowNew(ex, "Local model is not loaded. Check model status and retry.");
        return;
    }
    g_cancel_requested.store(false);
    const auto requestStart = std::chrono::steady_clock::now();
    llama_memory_clear(llama_get_memory(g_ctx), true);
    const llama_vocab *vocab = llama_model_get_vocab(g_model);
    if (prompt.size() > static_cast<size_t>(std::numeric_limits<int32_t>::max())) {
        jclass ex = env->FindClass("java/lang/IllegalArgumentException");
        if (ex) env->ThrowNew(ex, "Message is too long to tokenize for the local context window.");
        LOGE("Prompt rejected: UTF-8 bytes exceed the llama.cpp tokenization limit");
        return;
    }
    std::vector<llama_token> tokens(prompt.size() + 32);
    const auto tokenizeStart = std::chrono::steady_clock::now();
    int32_t count = llama_tokenize(vocab, prompt.c_str(), static_cast<int32_t>(prompt.size()), tokens.data(), static_cast<int32_t>(tokens.size()), true, true);
    if (count == std::numeric_limits<int32_t>::min()) {
        jclass ex = env->FindClass("java/lang/IllegalArgumentException");
        if (ex) env->ThrowNew(ex, "Message is too large to tokenize for the local context window.");
        LOGE("Prompt tokenization overflowed llama.cpp's int32 token count");
        return;
    }
    if (count < 0) {
        tokens.resize(static_cast<size_t>(-count));
        count = llama_tokenize(vocab, prompt.c_str(), static_cast<int32_t>(prompt.size()), tokens.data(), static_cast<int32_t>(tokens.size()), true, true);
    }
    const auto tokenizeEnd = std::chrono::steady_clock::now();
    if (count <= 0 || count > static_cast<int32_t>(llama_n_ctx(g_ctx)) - 162) {
        jclass ex = env->FindClass("java/lang/IllegalArgumentException");
        if (ex) env->ThrowNew(ex, "Message is too long for the local context window. Please shorten it.");
        LOGE("Prompt rejected: tokens=%d context=%u", count, llama_n_ctx(g_ctx));
        return;
    }
    const auto promptBatchStart = std::chrono::steady_clock::now();
    const int32_t promptBatchLimit = static_cast<int32_t>(llama_n_batch(g_ctx));
    if (promptBatchLimit <= 0) {
        LOGE("Invalid llama.cpp batch limit: %d", promptBatchLimit);
        jclass ex = env->FindClass("java/lang/IllegalStateException");
        if (ex) env->ThrowNew(ex, "llama.cpp returned an invalid prompt batch size.");
        return;
    }
    const int32_t batchCapacity = std::min(promptBatchLimit, count);
    tokens.resize(static_cast<size_t>(count));
    llama_batch batch = llama_batch_init(batchCapacity, 0, 1);
    double promptBatchMs = 0.0;
    double promptDecodeMs = 0.0;
    for (int32_t offset = 0; offset < count;) {
        const int32_t chunkCount = std::min(promptBatchLimit, count - offset);
        const auto batchFillStart = std::chrono::steady_clock::now();
        for (int32_t i = 0; i < chunkCount; ++i) {
            const int32_t tokenIndex = offset + i;
            batch.token[i] = tokens[static_cast<size_t>(tokenIndex)];
            batch.pos[i] = tokenIndex;
            batch.n_seq_id[i] = 1;
            batch.seq_id[i][0] = 0;
            batch.logits[i] = (tokenIndex == count - 1);
        }
        batch.n_tokens = chunkCount;
        const auto decodeStart = std::chrono::steady_clock::now();
        promptBatchMs += std::chrono::duration<double, std::milli>(decodeStart - batchFillStart).count();
        const int decodeResult = llama_decode(g_ctx, batch);
        promptDecodeMs += std::chrono::duration<double, std::milli>(
            std::chrono::steady_clock::now() - decodeStart).count();
        if (decodeResult != 0) {
            llama_batch_free(batch);
            LOGE("Prompt decode failed at token %d/%d (result=%d)", offset, count, decodeResult);
            jclass ex = env->FindClass("java/lang/IllegalStateException");
            if (ex) env->ThrowNew(ex, "llama.cpp failed while evaluating the prompt.");
            return;
        }
        offset += chunkCount;
    }
    promptBatchMs += std::chrono::duration<double, std::milli>(
        promptBatchStart - tokenizeEnd).count();

    jclass callbackClass = env->GetObjectClass(callback);
    if (!callbackClass || env->ExceptionCheck()) {
        llama_batch_free(batch);
        LOGE("Could not resolve token callback class");
        return;
    }
    jmethodID onToken = env->GetMethodID(callbackClass, "onToken", "(Ljava/lang/String;)V");
    if (!onToken || env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        llama_batch_free(batch);
        LOGE("Could not resolve token callback method");
        return;
    }
    env->DeleteLocalRef(callbackClass);
    const llama_token eos = llama_vocab_eos(vocab);
    int position = count;
    int sampledTokens = 0;
    int decodedTokens = 0;
    int callbackCount = 0;
    std::string pendingUtf8;
    std::string callbackText;
    bool callbackFailed = false;
    double sampleMs = 0.0;
    double decodeMs = 0.0;
    double callbackMs = 0.0;
    double firstSampleMs = -1.0;
    double firstCallbackMs = -1.0;
    const auto generationStart = std::chrono::steady_clock::now();
    auto flushCallback = [&]() {
        if (callbackText.empty()) return true;
        const auto callbackStart = std::chrono::steady_clock::now();
        jstring chunk = utf8_to_jstring(env, callbackText);
        if (chunk) {
            env->CallVoidMethod(callback, onToken, chunk);
            env->DeleteLocalRef(chunk);
            ++callbackCount;
            if (firstCallbackMs < 0.0) {
                firstCallbackMs = std::chrono::duration<double, std::milli>(
                    std::chrono::steady_clock::now() - requestStart).count();
            }
        }
        callbackText.clear();
        callbackMs += std::chrono::duration<double, std::milli>(
            std::chrono::steady_clock::now() - callbackStart).count();
        if (env->ExceptionCheck()) {
            LOGE("Exception from Kotlin token callback; stopping generation");
            env->ExceptionDescribe();
            env->ExceptionClear();
            return false;
        }
        return true;
    };
    LOGI("Inference started prompt_tokens=%d prompt_prepare_ms=%.3f tokenize_ms=%.3f prompt_batch_ms=%.3f prompt_decode_ms=%.3f",
         count,
         std::chrono::duration<double, std::milli>(tokenizeStart - requestStart).count(),
         std::chrono::duration<double, std::milli>(tokenizeEnd - tokenizeStart).count(),
         promptBatchMs, promptDecodeMs);
    for (int i = 0; i < 160 && !g_cancel_requested.load(); ++i) {
        const auto sampleStart = std::chrono::steady_clock::now();
        llama_token token = llama_sampler_sample(g_sampler, g_ctx, -1);
        sampleMs += std::chrono::duration<double, std::milli>(
            std::chrono::steady_clock::now() - sampleStart).count();
        if (token == eos || token == llama_vocab_eot(vocab)) break;
        if (firstSampleMs < 0.0) {
            firstSampleMs = std::chrono::duration<double, std::milli>(
                std::chrono::steady_clock::now() - requestStart).count();
        }
        ++sampledTokens;
        char piece[512];
        int32_t n = llama_token_to_piece(vocab, token, piece, sizeof(piece), 0, true);
        if (n > 0) {
            pendingUtf8.append(piece, static_cast<size_t>(n));
            const size_t complete = complete_utf8_prefix(pendingUtf8);
            if (complete > 0) {
                callbackText.append(pendingUtf8, 0, complete);
                pendingUtf8.erase(0, complete);
            }
        }
        // Amortize JNI/Kotlin Flow overhead while keeping streamed output responsive.
        if (sampledTokens % 4 == 0 && !flushCallback()) callbackFailed = true;
        if (callbackFailed) break;
        batch.token[0] = token; batch.pos[0] = position++;
        batch.n_seq_id[0] = 1; batch.seq_id[0][0] = 0; batch.logits[0] = true; batch.n_tokens = 1;
        const auto decodeStart = std::chrono::steady_clock::now();
        const int decodeResult = llama_decode(g_ctx, batch);
        decodeMs += std::chrono::duration<double, std::milli>(
            std::chrono::steady_clock::now() - decodeStart).count();
        if (decodeResult != 0) { LOGE("Decode failed at generated token %d", i); break; }
        ++decodedTokens;
    }
    if (!callbackFailed && !flushCallback()) callbackFailed = true;
    llama_batch_free(batch);
    const auto end = std::chrono::steady_clock::now();
    const double totalMs = std::chrono::duration<double, std::milli>(end - generationStart).count();
    const double responseMs = std::chrono::duration<double, std::milli>(end - requestStart).count();
    const double tps = totalMs > 0 ? sampledTokens * 1000.0 / totalMs : 0.0;
    LOGI("Inference finished sampled_tokens=%d decoded_tokens=%d first_sample_ms=%.3f first_callback_ms=%.3f native_response_ms=%.3f generation_ms=%.3f sample_ms=%.3f decode_ms=%.3f callback_ms=%.3f callback_count=%d sampled_tokens_per_second=%.2f cancelled=%d callback_error=%d",
         sampledTokens, decodedTokens, firstSampleMs, firstCallbackMs, responseMs, totalMs,
         sampleMs, decodeMs, callbackMs, callbackCount, tps,
         g_cancel_requested.load() ? 1 : 0, callbackFailed ? 1 : 0);
}

static void nativeRelease(JNIEnv *, jobject) {
    g_cancel_requested.store(true);
    std::lock_guard<std::mutex> lock(g_mutex);
    release_locked();
    LOGI("Native model released");
}

static JNINativeMethod methods[] = {
    {const_cast<char*>("nativeLoadModel"), const_cast<char*>("(Ljava/lang/String;)Z"), (void*)nativeLoadModel},
    {const_cast<char*>("nativeGenerateResponse"), const_cast<char*>("(Ljava/lang/String;Lcom/pocketqwen/app/repository/TokenCallback;)V"), (void*)nativeGenerateResponse},
    {const_cast<char*>("nativeCancelGeneration"), const_cast<char*>("()V"), (void*)nativeCancelGeneration},
    {const_cast<char*>("nativeIsModelAvailable"), const_cast<char*>("()Z"), (void*)nativeIsModelAvailable},
    {const_cast<char*>("nativeGetModelStatus"), const_cast<char*>("()Ljava/lang/String;"), (void*)nativeGetModelStatus},
    {const_cast<char*>("nativeRelease"), const_cast<char*>("()V"), (void*)nativeRelease}
};

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *) {
    JNIEnv *env = nullptr;
    if (vm->GetEnv((void **)&env, JNI_VERSION_1_6) != JNI_OK) return JNI_ERR;
    jclass cls = env->FindClass("com/pocketqwen/app/repository/NativeAiRepository");
    if (!cls) return JNI_ERR;
    if (env->RegisterNatives(cls, methods, sizeof(methods) / sizeof(methods[0])) != JNI_OK) return JNI_ERR;
    return JNI_VERSION_1_6;
}
