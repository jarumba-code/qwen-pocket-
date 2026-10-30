#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <mutex>
#include "llama.h"

#define LOG_TAG "PocketQwenNative"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static std::mutex g_mutex;
static llama_model *g_model = nullptr;
static llama_context *g_ctx = nullptr;
static llama_sampler *g_sampler = nullptr;
static bool g_initialized = false;

static void release_locked() {
    if (g_sampler) { llama_sampler_free(g_sampler); g_sampler = nullptr; }
    if (g_ctx) { llama_free(g_ctx); g_ctx = nullptr; }
    if (g_model) { llama_model_free(g_model); g_model = nullptr; }
    g_initialized = false;
}

static jboolean nativeLoadModel(JNIEnv *env, jobject, jstring path) {
    if (!path) return JNI_FALSE;
    const char *p = env->GetStringUTFChars(path, nullptr);
    if (!p) return JNI_FALSE;
    std::lock_guard<std::mutex> lock(g_mutex);
    release_locked();
    llama_backend_init();
    llama_model_params mp = llama_model_default_params();
    mp.n_gpu_layers = 0;
    g_model = llama_model_load_from_file(p, mp);
    env->ReleaseStringUTFChars(path, p);
    if (!g_model) { LOGE("Model load failed"); return JNI_FALSE; }
    llama_context_params cp = llama_context_default_params();
    cp.n_ctx = 1024;
    cp.n_batch = 256;
    cp.n_threads = 4;
    cp.n_threads_batch = 4;
    g_ctx = llama_init_from_model(g_model, cp);
    if (!g_ctx) { release_locked(); LOGE("Context creation failed"); return JNI_FALSE; }
    g_sampler = llama_sampler_init_greedy();
    if (!g_sampler) { release_locked(); return JNI_FALSE; }
    g_initialized = true;
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

static jstring nativeGenerateResponse(JNIEnv *env, jobject, jstring input) {
    if (!input) return env->NewStringUTF("");
    const char *raw = env->GetStringUTFChars(input, nullptr);
    if (!raw) return env->NewStringUTF("");
    std::string prompt = std::string("<|im_start|>system\nYou are PocketQwen, a helpful offline assistant.\n<|im_end|>\n<|im_start|>user\n") + raw + "\n<|im_end|>\n<|im_start|>assistant\n";
    env->ReleaseStringUTFChars(input, raw);

    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_initialized || !g_model || !g_ctx || !g_sampler) {
        return env->NewStringUTF("The local model is not loaded yet. Please try again.");
    }
    llama_memory_clear(llama_get_memory(g_ctx), true);
    const llama_vocab *vocab = llama_model_get_vocab(g_model);
    std::vector<llama_token> tokens(prompt.size() + 16);
    int32_t count = llama_tokenize(vocab, prompt.c_str(), (int32_t)prompt.size(),
                                   tokens.data(), (int32_t)tokens.size(), true, true);
    if (count < 0) {
        tokens.resize((size_t)(-count));
        count = llama_tokenize(vocab, prompt.c_str(), (int32_t)prompt.size(),
                               tokens.data(), (int32_t)tokens.size(), true, true);
    }
    if (count <= 0 || count > (int32_t)llama_n_ctx(g_ctx) - 2) {
        return env->NewStringUTF("That message is too long for the current local context.");
    }
    tokens.resize((size_t)count);
    llama_batch batch = llama_batch_init((int32_t)tokens.size() + 1, 0, 1);
    for (int32_t i = 0; i < count; ++i) {
        batch.token[i] = tokens[(size_t)i];
        batch.pos[i] = i;
        batch.n_seq_id[i] = 1;
        batch.seq_id[i][0] = 0;
        batch.logits[i] = (i == count - 1);
    }
    batch.n_tokens = count;
    if (llama_decode(g_ctx, batch) != 0) {
        llama_batch_free(batch);
        return env->NewStringUTF("Local inference failed while processing the prompt.");
    }

    std::string output;
    const llama_token eos = llama_vocab_eos(vocab);
    for (int i = 0; i < 160; ++i) {
        llama_token token = llama_sampler_sample(g_sampler, g_ctx, -1);
        if (token == eos || token == llama_vocab_eot(vocab)) break;
        char piece[512];
        int32_t n = llama_token_to_piece(vocab, token, piece, sizeof(piece), 0, true);
        if (n > 0) output.append(piece, (size_t)n);
        batch.token[0] = token;
        batch.pos[0] = count + i;
        batch.n_seq_id[0] = 1;
        batch.seq_id[0][0] = 0;
        batch.logits[0] = true;
        batch.n_tokens = 1;
        if (llama_decode(g_ctx, batch) != 0) break;
    }
    llama_batch_free(batch);
    if (output.empty()) output = "I couldn't generate a response. Please try again.";
    return env->NewStringUTF(output.c_str());
}

static void nativeRelease(JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    release_locked();
}

static JNINativeMethod methods[] = {
    {const_cast<char*>("nativeLoadModel"), const_cast<char*>("(Ljava/lang/String;)Z"), (void*)nativeLoadModel},
    {const_cast<char*>("nativeGenerateResponse"), const_cast<char*>("(Ljava/lang/String;)Ljava/lang/String;"), (void*)nativeGenerateResponse},
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
