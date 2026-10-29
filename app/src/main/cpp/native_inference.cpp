#include <jni.h>
#include <android/log.h>

#include "llama.h"

#include <cstdint>
#include <limits>
#include <memory>
#include <mutex>
#include <stdexcept>
#include <string>
#include <vector>

namespace {

constexpr const char * kLogTag = "PocketQwenNative";
constexpr uint32_t kContextSize = 1024;
constexpr int32_t kMaxPromptTokens = 832;
constexpr int32_t kMaxNewTokens = 160;

std::mutex g_engine_mutex;
std::once_flag g_backend_once;
llama_model * g_model = nullptr;
llama_context * g_context = nullptr;
const llama_vocab * g_vocab = nullptr;

void throw_state(JNIEnv * env, const char * message) {
    jclass exception_class = env->FindClass("java/lang/IllegalStateException");
    if (exception_class != nullptr) {
        env->ThrowNew(exception_class, message);
        env->DeleteLocalRef(exception_class);
    }
}

void append_utf8(std::string & output, uint32_t cp) {
    if (cp <= 0x7f) {
        output.push_back(static_cast<char>(cp));
    } else if (cp <= 0x7ff) {
        output.push_back(static_cast<char>(0xc0 | (cp >> 6)));
        output.push_back(static_cast<char>(0x80 | (cp & 0x3f)));
    } else if (cp <= 0xffff) {
        output.push_back(static_cast<char>(0xe0 | (cp >> 12)));
        output.push_back(static_cast<char>(0x80 | ((cp >> 6) & 0x3f)));
        output.push_back(static_cast<char>(0x80 | (cp & 0x3f)));
    } else {
        output.push_back(static_cast<char>(0xf0 | (cp >> 18)));
        output.push_back(static_cast<char>(0x80 | ((cp >> 12) & 0x3f)));
        output.push_back(static_cast<char>(0x80 | ((cp >> 6) & 0x3f)));
        output.push_back(static_cast<char>(0x80 | (cp & 0x3f)));
    }
}

std::string from_java_string(JNIEnv * env, jstring value) {
    if (value == nullptr) {
        return {};
    }
    const jsize length = env->GetStringLength(value);
    const jchar * chars = env->GetStringChars(value, nullptr);
    if (chars == nullptr) {
        throw std::runtime_error("Unable to read Java string.");
    }

    std::string output;
    output.reserve(static_cast<size_t>(length) * 3);
    for (jsize i = 0; i < length; ++i) {
        uint32_t cp = chars[i];
        if (cp >= 0xd800 && cp <= 0xdbff && i + 1 < length) {
            const uint32_t low = chars[i + 1];
            if (low >= 0xdc00 && low <= 0xdfff) {
                cp = 0x10000 + ((cp - 0xd800) << 10) + (low - 0xdc00);
                ++i;
            } else {
                cp = 0xfffd;
            }
        } else if (cp >= 0xd800 && cp <= 0xdfff) {
            cp = 0xfffd;
        }
        append_utf8(output, cp);
    }
    env->ReleaseStringChars(value, chars);
    return output;
}

uint32_t next_utf8_codepoint(const std::string & input, size_t & index) {
    const auto * bytes = reinterpret_cast<const unsigned char *>(input.data());
    const size_t length = input.size();
    const unsigned char first = bytes[index];
    if (first <= 0x7f) {
        ++index;
        return first;
    }

    uint32_t cp = 0;
    size_t count = 0;
    if (first >= 0xc2 && first <= 0xdf) {
        cp = first & 0x1f;
        count = 2;
    } else if (first >= 0xe0 && first <= 0xef) {
        cp = first & 0x0f;
        count = 3;
    } else if (first >= 0xf0 && first <= 0xf4) {
        cp = first & 0x07;
        count = 4;
    } else {
        ++index;
        return 0xfffd;
    }

    if (index + count > length) {
        ++index;
        return 0xfffd;
    }

    for (size_t i = 1; i < count; ++i) {
        const unsigned char next = bytes[index + i];
        if ((next & 0xc0) != 0x80) {
            ++index;
            return 0xfffd;
        }
        cp = (cp << 6) | (next & 0x3f);
    }

    if ((count == 3 && cp < 0x800) ||
        (count == 4 && cp < 0x10000) ||
        cp > 0x10ffff ||
        (cp >= 0xd800 && cp <= 0xdfff)) {
        ++index;
        return 0xfffd;
    }

    index += count;
    return cp;
}

jstring to_java_string(JNIEnv * env, const std::string & input) {
    std::vector<jchar> output;
    output.reserve(input.size());

    size_t index = 0;
    while (index < input.size()) {
        const uint32_t cp = next_utf8_codepoint(input, index);
        if (cp <= 0xffff) {
            output.push_back(static_cast<jchar>(cp));
        } else {
            const uint32_t value = cp - 0x10000;
            output.push_back(static_cast<jchar>(0xd800 + (value >> 10)));
            output.push_back(static_cast<jchar>(0xdc00 + (value & 0x3ff)));
        }
    }

    if (output.size() > static_cast<size_t>(std::numeric_limits<jsize>::max())) {
        throw_state(env, "Generated response is too large to return to Android.");
        return nullptr;
    }
    return env->NewString(output.data(), static_cast<jsize>(output.size()));
}

void free_engine_locked() {
    if (g_context != nullptr) {
        llama_free(g_context);
        g_context = nullptr;
    }
    if (g_model != nullptr) {
        llama_model_free(g_model);
        g_model = nullptr;
    }
    g_vocab = nullptr;
}

bool set_batch_tokens(
        llama_batch_ext * batch,
        const llama_token * tokens,
        int32_t count,
        llama_pos start_position) {
    if (batch == nullptr || tokens == nullptr || count <= 0) {
        return false;
    }

    llama_batch_ext_clear(batch);
    for (int32_t i = 0; i < count; ++i) {
        const int32_t index = llama_batch_ext_add_token(batch, 0, tokens[i]);
        if (index < 0) {
            return false;
        }
        const llama_pos position = start_position + i;
        if (!llama_batch_ext_set_pos(batch, index, &position)) {
            return false;
        }
    }
    return llama_batch_ext_set_output_logits(batch, count - 1, true);
}

std::string token_to_utf8(const llama_vocab * vocab, llama_token token) {
    std::vector<char> buffer(256);
    int32_t length = llama_token_to_piece(
        vocab, token, buffer.data(), static_cast<int32_t>(buffer.size()), 0, false);

    if (length < 0) {
        const int32_t required = -length;
        if (required <= 0 || required > 1024 * 1024) {
            throw std::runtime_error("The model returned an invalid token piece.");
        }
        buffer.resize(static_cast<size_t>(required));
        length = llama_token_to_piece(
            vocab, token, buffer.data(), static_cast<int32_t>(buffer.size()), 0, false);
    }

    if (length < 0) {
        throw std::runtime_error("Unable to decode a generated token.");
    }
    return std::string(buffer.data(), static_cast<size_t>(length));
}

} // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_com_pocketqwen_app_repository_LlamaRepository_nativeLoadModel(
        JNIEnv * env, jobject /* thiz */, jstring model_path_value) {
    try {
        const std::string model_path = from_java_string(env, model_path_value);
        if (model_path.empty()) {
            throw_state(env, "Choose a local GGUF model file first.");
            return JNI_FALSE;
        }

        std::lock_guard<std::mutex> lock(g_engine_mutex);
        std::call_once(g_backend_once, [] { llama_backend_init(); });

        // Release the previous context before loading a new model to reduce peak RAM.
        free_engine_locked();

        llama_model_params model_params = llama_model_default_params();
        model_params.n_gpu_layers = 0; // portable CPU-only inference

        llama_model * model = llama_model_load_from_file(model_path.c_str(), model_params);
        if (model == nullptr) {
            __android_log_print(ANDROID_LOG_ERROR, kLogTag, "Could not load GGUF model.");
            throw_state(env, "llama.cpp could not load this GGUF model. Check the file and available RAM.");
            return JNI_FALSE;
        }

        llama_context_params context_params = llama_context_default_params();
        context_params.n_ctx = kContextSize;
        context_params.n_batch = kContextSize;
        context_params.n_ubatch = 256;
        context_params.n_threads = 4;
        context_params.n_threads_batch = 4;
        context_params.no_perf = true;

        llama_context * context = llama_init_from_model(model, context_params);
        if (context == nullptr) {
            llama_model_free(model);
            throw_state(env, "The model loaded, but llama.cpp could not create an inference context. Try a smaller model.");
            return JNI_FALSE;
        }

        g_model = model;
        g_context = context;
        g_vocab = llama_model_get_vocab(model);
        __android_log_print(ANDROID_LOG_INFO, kLogTag, "Loaded local GGUF model.");
        return JNI_TRUE;
    } catch (const std::exception & error) {
        throw_state(env, error.what());
        return JNI_FALSE;
    } catch (...) {
        throw_state(env, "Unexpected error while loading the local model.");
        return JNI_FALSE;
    }
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_pocketqwen_app_repository_LlamaRepository_nativeIsLoaded(
        JNIEnv * /* env */, jobject /* thiz */) {
    std::lock_guard<std::mutex> lock(g_engine_mutex);
    return (g_model != nullptr && g_context != nullptr && g_vocab != nullptr)
        ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_pocketqwen_app_repository_LlamaRepository_nativeRelease(
        JNIEnv * /* env */, jobject /* thiz */) {
    std::lock_guard<std::mutex> lock(g_engine_mutex);
    free_engine_locked();
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_pocketqwen_app_repository_LlamaRepository_nativeGenerate(
        JNIEnv * env, jobject /* thiz */, jstring message_value) {
    try {
        const std::string user_message = from_java_string(env, message_value);
        if (user_message.empty()) {
            throw_state(env, "Enter a message before generating a response.");
            return nullptr;
        }
        if (user_message.size() > 4096) {
            throw_state(env, "This first version supports messages up to 4,096 UTF-8 bytes.");
            return nullptr;
        }

        std::lock_guard<std::mutex> lock(g_engine_mutex);
        if (g_model == nullptr || g_context == nullptr || g_vocab == nullptr) {
            throw_state(env, "No local model is loaded. Open Settings and select a .gguf file.");
            return nullptr;
        }

        // This first version responds to the current message only; prior conversation
        // turns are not yet included in the native prompt.
        const std::string prompt =
            "<|im_start|>system\n"
            "You are PocketQwen, a helpful private on-device assistant. "
            "Be accurate, clear, and concise.\n<|im_end|>\n"
            "<|im_start|>user\n" + user_message +
            "\n<|im_end|>\n<|im_start|>assistant\n";

        const int32_t prompt_length = static_cast<int32_t>(prompt.size());
        const int32_t required_tokens = -llama_tokenize(
            g_vocab, prompt.c_str(), prompt_length, nullptr, 0, true, true);
        if (required_tokens <= 0 || required_tokens > kMaxPromptTokens) {
            throw_state(env, "This message is too long for the current 1,024-token context. Try a shorter prompt.");
            return nullptr;
        }

        std::vector<llama_token> prompt_tokens(static_cast<size_t>(required_tokens));
        const int32_t token_count = llama_tokenize(
            g_vocab, prompt.c_str(), prompt_length, prompt_tokens.data(),
            static_cast<int32_t>(prompt_tokens.size()), true, true);
        if (token_count < 0 || token_count != required_tokens) {
            throw_state(env, "Failed to tokenize the prompt for this model.");
            return nullptr;
        }

        llama_memory_clear(llama_get_memory(g_context), true);

        using BatchPtr = std::unique_ptr<llama_batch_ext, decltype(&llama_batch_ext_free)>;
        BatchPtr batch(llama_batch_ext_init(g_context), &llama_batch_ext_free);
        if (!batch) {
            throw_state(env, "Unable to allocate the llama.cpp token batch.");
            return nullptr;
        }
        if (!set_batch_tokens(batch.get(), prompt_tokens.data(), token_count, 0)) {
            throw_state(env, "Unable to prepare the input tokens for inference.");
            return nullptr;
        }

        const int32_t prompt_status = llama_process(
            g_context, LLAMA_PROCESS_TYPE_DECODE, batch.get());
        if (prompt_status != 0) {
            __android_log_print(ANDROID_LOG_ERROR, kLogTag, "Prompt evaluation failed: %d", prompt_status);
            throw_state(env, "The model could not evaluate this prompt. Try a shorter message or smaller model.");
            return nullptr;
        }

        llama_sampler * sampler_raw = llama_sampler_chain_init(llama_sampler_chain_default_params());
        if (sampler_raw == nullptr) {
            throw_state(env, "Unable to initialize the sampler.");
            return nullptr;
        }
        using SamplerPtr = std::unique_ptr<llama_sampler, decltype(&llama_sampler_free)>;
        SamplerPtr sampler(sampler_raw, &llama_sampler_free);
        llama_sampler_chain_add(sampler.get(), llama_sampler_init_top_k(40));
        llama_sampler_chain_add(sampler.get(), llama_sampler_init_top_p(0.9f, 1));
        llama_sampler_chain_add(sampler.get(), llama_sampler_init_temp(0.7f));
        llama_sampler_chain_add(sampler.get(), llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

        std::string response;
        int32_t position = 0;
        int32_t tokens_in_batch = token_count;
        int32_t generated_tokens = 0;

        while (generated_tokens < kMaxNewTokens &&
               position + tokens_in_batch < token_count + kMaxNewTokens) {
            const int32_t status = llama_process(
                g_context, LLAMA_PROCESS_TYPE_DECODE, batch.get());
            if (status != 0) {
                __android_log_print(ANDROID_LOG_ERROR, kLogTag, "Token evaluation failed: %d", status);
                throw_state(env, "Local inference stopped unexpectedly. Try a shorter prompt.");
                return nullptr;
            }

            position += tokens_in_batch;
            const llama_token next_token = llama_sampler_sample(sampler.get(), g_context, -1);
            if (llama_vocab_is_eog(g_vocab, next_token)) {
                break;
            }

            response += token_to_utf8(g_vocab, next_token);
            if (!set_batch_tokens(batch.get(), &next_token, 1, position)) {
                throw_state(env, "Unable to prepare the next generated token.");
                return nullptr;
            }
            tokens_in_batch = 1;
            ++generated_tokens;
        }

        if (response.empty()) {
            throw_state(env, "The model returned an empty response. Try another prompt.");
            return nullptr;
        }
        return to_java_string(env, response);
    } catch (const std::exception & error) {
        throw_state(env, error.what());
        return nullptr;
    } catch (...) {
        throw_state(env, "Unexpected error during local inference.");
        return nullptr;
    }
}
