/*
 * PocketQwen - Native inference bridge
 *
 * JNI bridge between Kotlin and llama.cpp C API.
 * All model loading and generation runs off the main thread via Kotlin coroutines.
 */

#include <jni.h>
#include <string>
#include <cstring>
#include <vector>
#include <mutex>

#include "llama.h"

static pthread_mutex_t g_llama_mutex = PTHREAD_MUTEX_INITIALIZER;

// Native context handle (opaque pointer to llama_context)
static llama_context *g_ctx = nullptr;
static llama_model *g_model = nullptr;
static bool g_model_loaded = false;

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

static void throw_java_exception(JNIEnv *env, const char *msg) {
    jclass exc = env->FindClass("java/lang/RuntimeException");
    if (exc != nullptr) {
        env->ThrowNew(exc, msg);
    }
}

// ---------------------------------------------------------------------------
// JNI EXPORTED FUNCTIONS
// ---------------------------------------------------------------------------

extern "C" JNIEXPORT jboolean JNICALL
Java_com_pocketqwen_app_repository_NativeLlamaRepository_nativeIsAvailable(
        JNIEnv *env, jobject /* thiz */) {
    pthread_mutex_lock(&g_llama_mutex);
    bool available = g_model_loaded && g_model != nullptr && g_ctx != nullptr;
    pthread_mutex_unlock(&g_llama_mutex);
    return static_cast<jboolean>(available);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_pocketqwen_app_repository_NativeLlamaRepository_nativeGetModelStatus(
        JNIEnv *env, jobject /* thiz */) {
    pthread_mutex_lock(&g_llama_mutex);
    const char *status;
    if (g_model_loaded && g_model != nullptr && g_ctx != nullptr) {
        status = "loaded";
    } else if (g_model != nullptr || g_ctx != nullptr) {
        status = "partial";
    } else {
        status = "not_installed";
    }
    pthread_mutex_unlock(&g_llama_mutex);
    return env->NewStringUTF(status);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_pocketqwen_app_repository_NativeLlamaRepository_nativeLoadModel(
        JNIEnv *env, jobject /* thiz */, jstring modelPath) {
    if (modelPath == nullptr) {
        throw_java_exception(env, "model path is null");
        return JNI_FALSE;
    }

    const char *path_cstr = env->GetStringUTFChars(modelPath, nullptr);
    if (path_cstr == nullptr) {
        return JNI_FALSE;
    }

    pthread_mutex_lock(&g_llama_mutex);

    // Release any existing model
    if (g_ctx != nullptr) {
        llama_free(g_ctx);
        g_ctx = nullptr;
    }
    if (g_model != nullptr) {
        llama_free_model(g_model);
        g_model = nullptr;
    }
    g_model_loaded = false;

    // Load model
    llama_model_params model_params = llama_model_default_params();
    g_model = llama_load_model_from_file(path_cstr, model_params);

    if (g_model == nullptr) {
        env->ReleaseStringUTFChars(modelPath, path_cstr);
        pthread_mutex_unlock(&g_llama_mutex);
        throw_java_exception(env, "failed to load model from file");
        return JNI_FALSE;
    }

    // Create context
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 1024;           // Conservative context size for budget phones
    ctx_params.n_threads = 4;          // Conservative thread count
    ctx_params.n_gpu_layers = 0;       // CPU only for maximum compatibility
    ctx_params.seed = 42;
    ctx_params.embedding = false;

    g_ctx = llama_new_context_with_model(g_model, ctx_params);

    if (g_ctx == nullptr) {
        llama_free_model(g_model);
        g_model = nullptr;
        env->ReleaseStringUTFChars(modelPath, path_cstr);
        pthread_mutex_unlock(&g_llama_mutex);
        throw_java_exception(env, "failed to create llama context");
        return JNI_FALSE;
    }

    g_model_loaded = true;
    env->ReleaseStringUTFChars(modelPath, path_cstr);
    pthread_mutex_unlock(&g_llama_mutex);
    return JNI_TRUE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_pocketqwen_app_repository_NativeLlamaRepository_nativeGenerate(
        JNIEnv *env, jobject /* thiz */, jstring prompt) {
    if (prompt == nullptr) {
        throw_java_exception(env, "prompt is null");
        return env->NewStringUTF("");
    }

    const char *prompt_cstr = env->GetStringUTFChars(prompt, nullptr);
    if (prompt_cstr == nullptr) {
        return env->NewStringUTF("");
    }

    pthread_mutex_lock(&g_llama_mutex);

    if (!g_model_loaded || g_model == nullptr || g_ctx == nullptr) {
        env->ReleaseStringUTFChars(prompt, path_cstr);
        pthread_mutex_unlock(&g_llama_mutex);
        throw_java_exception(env, "model not loaded");
        return env->NewStringUTF("");
    }

    // Tokenize input
    std::vector<llama_token> tokens(2048);
    int n_tokens = llama_tokenize(g_model, prompt_cstr, tokens.data(), tokens.size(), true);
    if (n_tokens < 0) {
        env->ReleaseStringUTFChars(prompt, path_cstr);
        pthread_mutex_unlock(&g_llama_mutex);
        throw_java_exception(env, "tokenization failed");
        return env->NewStringUTF("");
    }

    // Generate
    int n_predict = 256;
    llama_token new_token = 0;
    std::string generated_text;

    for (int i = 0; i < n_predict; ++i) {
        llama_token new_token_id = llama_sample_token(g_ctx);
        if (new_token_id == llama_token_eos(g_model)) {
            break;
        }

        char buf[256];
        int n = llama_token_to_text(g_model, new_token_id, buf, sizeof(buf));
        if (n > 0) {
            generated_text.append(buf, n);
        }

        llama_token new_token_arr[1] = {new_token_id};
        llama_decode(g_ctx, &new_token_arr[0], 1, i + n_tokens);
    }

    pthread_mutex_unlock(&g_llama_mutex);
    env->ReleaseStringUTFChars(prompt, path_cstr);

    return env->NewStringUTF(generated_text.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_pocketqwen_app_repository_NativeLlamaRepository_nativeRelease(
        JNIEnv *env, jobject /* thiz */) {
    pthread_mutex_lock(&g_llama_mutex);
    if (g_ctx != nullptr) {
        llama_free(g_ctx);
        g_ctx = nullptr;
    }
    if (g_model != nullptr) {
        llama_free_model(g_model);
        g_model = nullptr;
    }
    g_model_loaded = false;
    pthread_mutex_unlock(&g_llama_mutex);
}

// JNI method registration table
static JNINativeMethod g_methods[] = {
    {"nativeIsAvailable", "()Z", reinterpret_cast<void *>(nativeIsAvailable)},
    {"nativeGetModelStatus", "()Ljava/lang/String;", reinterpret_cast<void *>(nativeGetModelStatus)},
    {"nativeLoadModel", "(Ljava/lang/String;)Z", reinterpret_cast<void *>(nativeLoadModel)},
    {"nativeGenerate", "(Ljava/lang/String;)Ljava/lang/String;", reinterpret_cast<void *>(nativeGenerate)},
    {"nativeRelease", "()V", reinterpret_cast<void *>(nativeRelease)},
};

extern "C" JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    jclass cls = env->FindClass("com/pocketqwen/app/repository/NativeLlamaRepository");
    if (cls == nullptr) {
        return JNI_ERR;
    }

    if (env->RegisterNatives(cls, g_methods, sizeof(g_methods) / sizeof(g_methods[0])) < 0) {
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}
