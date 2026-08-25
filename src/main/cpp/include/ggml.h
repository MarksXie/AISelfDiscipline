#pragma once

#include <stdint.h>
#include <stddef.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

#define GGML_FILE_MAGIC   0x67676d6c // "ggml"
#define GGUF_MAGIC        0x46554747 // "GGUF"
#define GGUF_VERSION      3

enum ggml_status {
    GGML_STATUS_ALLOC_FAILED = -2,
    GGML_STATUS_FAILED = -1,
    GGML_STATUS_SUCCESS = 0,
    GGML_STATUS_ABORTED = 1,
};

void ggml_time_init(void);
int64_t ggml_time_ms(void);
int64_t ggml_time_us(void);

#ifdef __cplusplus
}
#endif
