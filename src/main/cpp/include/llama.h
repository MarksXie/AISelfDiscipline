#pragma once

#include <stdint.h>
#include <stdbool.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

// GGML Types
typedef int32_t ggml_type;

struct ggml_context;
struct ggml_tensor;

// LLAMA Types
typedef int32_t llama_pos;
typedef int32_t llama_token;
typedef int32_t llama_seq_id;

struct llama_model;
struct llama_context;
struct llama_sampler;
struct llama_grammar;

struct llama_model_params {
    int32_t n_gpu_layers;
    int32_t main_gpu;
    const float * tensor_split;
    bool vocab_only;
    bool use_mmap;
    bool use_mlock;
    bool check_tensors;
};

struct llama_context_params {
    uint32_t n_ctx;
    uint32_t n_batch;
    uint32_t n_ubatch;
    uint32_t n_seq_max;
    int32_t  n_threads;
    int32_t  n_threads_batch;
    int8_t   rope_scaling_type;
    float    rope_freq_base;
    float    rope_freq_scale;
    float    yarn_ext_factor;
    float    yarn_attn_factor;
    float    yarn_beta_fast;
    float    yarn_beta_slow;
    uint32_t yarn_orig_ctx;
    float    defrag_thold;
    bool     embeddings;
    bool     offload_kqv;
    bool     flash_attn;
};

struct llama_token_data {
    llama_token id;
    float logit;
    float p;
};

struct llama_token_data_array {
    struct llama_token_data * data;
    size_t size;
    int64_t selected;
    bool sorted;
};

// Functions
struct llama_model_params llama_model_default_params(void);
struct llama_context_params llama_context_default_params(void);

void llama_backend_init(void);
void llama_backend_free(void);

struct llama_model * llama_load_model_from_file(const char * path_model, struct llama_model_params params);
void llama_free_model(struct llama_model * model);

struct llama_context * llama_new_context_with_model(struct llama_model * model, struct llama_context_params params);
void llama_free(struct llama_context * ctx);

int32_t llama_n_ctx(const struct llama_context * ctx);
int32_t llama_n_vocab(const struct llama_model * model);

int32_t llama_tokenize(
    const struct llama_model * model,
    const char * text,
    int32_t text_len,
    llama_token * tokens,
    int32_t n_tokens_max,
    bool add_special,
    bool parse_special
);

int32_t llama_token_to_piece(
    const struct llama_model * model,
    llama_token token,
    char * buf,
    int32_t length,
    int32_t lstrip,
    bool special
);

llama_token llama_token_bos(const struct llama_model * model);
llama_token llama_token_eos(const struct llama_model * model);
llama_token llama_token_eot(const struct llama_model * model);

#ifdef __cplusplus
}
#endif
