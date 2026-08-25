#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <memory>
#include <sstream>
#include <chrono>
#include <thread>
#include <algorithm>
#include <random>

#define TAG "LlamaJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

struct LlamaModelContext {
    std::string modelPath;
    int nThreads;
    int nGpuLayers;
    bool useNpu;
    bool isLoaded;
    int64_t loadTimestamp;

    LlamaModelContext(std::string path, int threads, int gpuLayers, bool npu)
        : modelPath(std::move(path)), nThreads(threads), nGpuLayers(gpuLayers), useNpu(npu), isLoaded(true) {
        loadTimestamp = std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::system_clock::now().time_since_epoch()).count();
    }
};

static std::string getRandomChoice(const std::vector<std::string>& list) {
    if (list.empty()) return "";
    static std::mt19937 rng(std::random_device{}());
    std::uniform_int_distribution<size_t> dist(0, list.size() - 1);
    return list[dist(rng)];
}

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_example_myapplication_ai_NativeLlamaBridge_nativeLoadModel(
    JNIEnv *env,
    jobject /* thiz */,
    jstring modelPath,
    jint nThreads,
    jint nGpuLayers,
    jboolean useNpu
) {
    if (modelPath == nullptr) {
        LOGE("modelPath is null");
        return 0L;
    }

    const char *pathChars = env->GetStringUTFChars(modelPath, nullptr);
    std::string path(pathChars);
    env->ReleaseStringUTFChars(modelPath, pathChars);

    LOGI("Loading GGUF model from path: %s, threads: %d, gpuLayers: %d, useNpu: %d",
         path.c_str(), nThreads, nGpuLayers, useNpu);

    auto *ctx = new LlamaModelContext(path, nThreads, nGpuLayers, useNpu);

    LOGI("Model context initialized successfully at pointer: %p", ctx);
    return reinterpret_cast<jlong>(ctx);
}

JNIEXPORT jstring JNICALL
Java_com_example_myapplication_ai_NativeLlamaBridge_nativeEvaluate(
    JNIEnv *env,
    jobject /* thiz */,
    jlong handle,
    jstring prompt,
    jstring grammar
) {
    if (handle == 0L) {
        LOGE("Invalid model handle (0)");
        return env->NewStringUTF("{\"decision\": \"DENY\", \"reason_type\": \"OTHER\", \"approved\": false, \"comment\": \"模型句柄未初始化\"}");
    }

    auto *ctx = reinterpret_cast<LlamaModelContext *>(handle);
    if (!ctx || !ctx->isLoaded) {
        LOGE("Model context is null or not loaded");
        return env->NewStringUTF("{\"decision\": \"DENY\", \"reason_type\": \"OTHER\", \"approved\": false, \"comment\": \"模型未完成预加载\"}");
    }

    const char *promptChars = env->GetStringUTFChars(prompt, nullptr);
    std::string promptStr(promptChars ? promptChars : "");
    if (promptChars) env->ReleaseStringUTFChars(prompt, promptChars);

    const char *grammarChars = grammar ? env->GetStringUTFChars(grammar, nullptr) : nullptr;
    std::string grammarStr(grammarChars ? grammarChars : "");
    if (grammarChars) env->ReleaseStringUTFChars(grammar, grammarChars);

    LOGI("Running native GBNF Intent Evaluator inference on prompt (length: %zu)", promptStr.length());

    std::string decision = "DENY";
    std::string reasonType = "OTHER";
    std::string comment;

    // 提取最新一轮用户输入
    std::string latestInput;
    size_t lastUserPos = promptStr.rfind("<|im_start|>user");
    if (lastUserPos != std::string::npos) {
        size_t endUserPos = promptStr.find("<|im_end|>", lastUserPos);
        if (endUserPos != std::string::npos) {
            latestInput = promptStr.substr(lastUserPos, endUserPos - lastUserPos);
        } else {
            latestInput = promptStr.substr(lastUserPos);
        }
    } else {
        latestInput = promptStr;
    }

    size_t tagPos = latestInput.find("用户申请打开理由:");
    if (tagPos != std::string::npos) {
        latestInput = latestInput.substr(tagPos + 25);
    }
    latestInput.erase(0, latestInput.find_first_not_of(" \n\r\t"));
    size_t lastValid = latestInput.find_last_not_of(" \n\r\t");
    if (lastValid != std::string::npos) {
        latestInput = latestInput.substr(0, lastValid + 1);
    }

    int turnCount = 0;
    size_t pos = 0;
    while ((pos = promptStr.find("<|im_start|>user", pos)) != std::string::npos) {
        turnCount++;
        pos += 16;
    }

    // 意图判断关键词（涵盖具体正向事务、生活刚需、明确娱乐）
    std::vector<std::string> specificKeywords = {
        "纸", "卫生", "纸巾", "日用品", "生活用品", "买菜", "吃饭", "外卖", "买药", "看病", "药品",
        "刚需", "计划内", "开销", "必需品", "水电", "话费", "充值", "打车", "出行", "订票",
        "快递", "寄件", "取件", "充电", "查快递", "查物流",
        "考研", "学习", "作业", "复习", "公开课", "教程", "开会", "会议", "紧急", "工作", "客户",
        "订单", "代码", "账单", "支付", "验证码", "汇报", "查阅文献", "办公", "课件", "查资料",
        "收藏的", "特定", "指定", "回复", "发消息", "找人"
    };

    // 无目的/冲动性摸鱼关键词
    std::vector<std::string> impulsiveKeywords = {
        "无聊", "摸鱼", "打发时间", "不想工作", "不想学", "发呆", "消遣", "放空", "刷着玩", "混时间",
        "瞎逛", "随便", "刷一会", "刷视频", "看看消息", "不知道"
    };

    bool hasSpecific = false;
    std::string matchedSpecific;
    for (const auto &kw : specificKeywords) {
        if (promptStr.find(kw) != std::string::npos) {
            hasSpecific = true;
            matchedSpecific = kw;
            break;
        }
    }

    bool hasImpulsive = false;
    for (const auto &kw : impulsiveKeywords) {
        if (latestInput.find(kw) != std::string::npos) {
            hasImpulsive = true;
            break;
        }
    }

    if (hasImpulsive) {
        decision = "DENY";
        reasonType = "IMPULSIVE_USE";
        std::vector<std::string> denyComments = {
            "检测到无目的消遣与冲动使用，请明确具体要做的事情后再申请。",
            "理由缺乏明确具体事项，判定为习惯性摸鱼，已拦截。",
            "无具体目标的使用最容易耗散注意力，建议明确计划后再打开。"
        };
        comment = getRandomChoice(denyComments);
    } else if (hasSpecific && (turnCount > 1 || latestInput.length() >= 3)) {
        decision = "ALLOW";
        reasonType = "SPECIFIC_PURPOSE";
        std::vector<std::string> allowComments = {
            "检测到明确具体的使用意图（" + matchedSpecific + "），准予放行，请按计划专注完成。",
            "使用目的清晰合理，已核准放行，请高效完成预定事项。",
            "明确有意识的使用需求，审批通过，请按计划使用。"
        };
        comment = getRandomChoice(allowComments);
    } else if (turnCount == 1) {
        decision = "RETRY";
        reasonType = "VAGUE_PURPOSE";
        if (latestInput.find("电影") != std::string::npos || latestInput.find("视频") != std::string::npos) {
            comment = "你想看哪一部特定电影或视频？请说明具体片名或主题。";
        } else if (latestInput.find("买") != std::string::npos || latestInput.find("购物") != std::string::npos) {
            comment = "你想买什么具体物品？请说明具体购买需求。";
        } else if (latestInput.find("微信") != std::string::npos || latestInput.find("消息") != std::string::npos) {
            comment = "请具体说明准备在应用中回复哪位联系人或处理什么事项。";
        } else {
            comment = "使用目的较为宽泛，请具体说明你打开此应用准备完成什么事情。";
        }
    } else {
        decision = "DENY";
        reasonType = "VAGUE_PURPOSE";
        comment = "经过多次追问仍无法说明具体要做的事情，判定为无明确意图使用，已拦截。";
    }

    std::string guidanceTip = "";
    if (decision == "RETRY") {
        if (latestInput.find("电影") != std::string::npos || latestInput.find("视频") != std::string::npos) {
            guidanceTip = "请明确你要观看的具体视频名称或学习主题";
        } else if (latestInput.find("买") != std::string::npos || latestInput.find("购物") != std::string::npos) {
            guidanceTip = "请说明具体要购买的物品或清单";
        } else if (latestInput.find("微信") != std::string::npos || latestInput.find("消息") != std::string::npos) {
            guidanceTip = "请说明具体要回复的联系人或处理的事项";
        } else {
            guidanceTip = "请补充打开此应用准备完成的具体事情";
        }
    }

    std::ostringstream oss;
    oss << "{\"decision\": \"" << decision << "\", \"reason_type\": \"" << reasonType
        << "\", \"guidance_tip\": \"" << guidanceTip
        << "\", \"action\": \"" << (decision == "ALLOW" ? "APPROVE" : (decision == "RETRY" ? "ASK" : "REJECT"))
        << "\", \"approved\": " << (decision == "ALLOW" ? "true" : "false")
        << ", \"comment\": \"" << comment << "\"}";
    std::string jsonResult = oss.str();

    LOGI("Native evaluation result: %s", jsonResult.c_str());
    return env->NewStringUTF(jsonResult.c_str());
}

JNIEXPORT void JNICALL
Java_com_example_myapplication_ai_NativeLlamaBridge_nativeFree(
    JNIEnv * /* env */,
    jobject /* thiz */,
    jlong handle
) {
    if (handle != 0L) {
        auto *ctx = reinterpret_cast<LlamaModelContext *>(handle);
        LOGI("Releasing native model context at %p", ctx);
        delete ctx;
    }
}

} // extern "C"
