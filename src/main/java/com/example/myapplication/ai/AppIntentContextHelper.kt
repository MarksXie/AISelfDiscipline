package com.example.myapplication.ai

import com.example.myapplication.data.model.AppRuleProfile

object AppIntentContextHelper {

    data class AppMetadata(
        val category: String,
        val allowedExamples: List<String>,
        val lowQualityExamples: List<String>
    )

    fun getAppMetadata(appName: String): AppMetadata {
        val name = appName.lowercase()
        return when {
            name.contains("bilibili") || name.contains("b站") || name.contains("哔哩") ||
                    name.contains("爱奇艺") || name.contains("腾讯视频") || name.contains("优酷") ||
                    name.contains("youtube") || name.contains("netflix") || name.contains("视频") ||
                    name.contains("抖音") || name.contains("快手") || name.contains("微视") -> {
                AppMetadata(
                    category = "长/短视频与多媒体娱乐",
                    allowedExamples = listOf(
                        "观看指定收藏的特定视频/电影/纪录片",
                        "继续观看特定课程或专业技术教程",
                        "搜索某项明确主题的视频资料",
                        "回复某条指定评论或通知"
                    ),
                    lowQualityExamples = listOf(
                        "无聊随便刷刷推荐流",
                        "消遣打发时间",
                        "不知道看啥随便看看",
                        "无意识连续划短视频"
                    )
                )
            }
            name.contains("微信") || name.contains("qq") || name.contains("聊天") ||
                    name.contains("telegram") || name.contains("whatsapp") -> {
                AppMetadata(
                    category = "即时通讯与社交",
                    allowedExamples = listOf(
                        "回复特定联系人（如客户/同事/家人）的重要消息",
                        "加入特定群聊讨论某项具体事务",
                        "处理工作群消息或接收重要文件",
                        "使用微信支付或扫描二维码"
                    ),
                    lowQualityExamples = listOf(
                        "无聊刷朋友圈或看群聊灌水",
                        "看看有没有新消息（无特定对象）",
                        "随便翻翻聊天列表"
                    )
                )
            }
            name.contains("淘宝") || name.contains("京东") || name.contains("拼多多") ||
                    name.contains("美团") || name.contains("饿了么") || name.contains("闲鱼") ||
                    name.contains("得物") || name.contains("1688") || name.contains("购物") -> {
                AppMetadata(
                    category = "电商购物与本地生活",
                    allowedExamples = listOf(
                        "购买特定清单物品（如买卫生纸、买药、买菜）",
                        "查询某件在途包裹的快递物流进度",
                        "按计划订餐或点外卖",
                        "处理某笔特定售后或客服咨询"
                    ),
                    lowQualityExamples = listOf(
                        "无聊随便逛逛看看有啥买",
                        "无目的刷直播带货或推荐商品流",
                        "消遣比价打发时间"
                    )
                )
            }
            name.contains("知乎") || name.contains("小红书") || name.contains("微博") ||
                    name.contains("贴吧") || name.contains("twitter") || name.contains("x") -> {
                AppMetadata(
                    category = "内容社区与社交资讯",
                    allowedExamples = listOf(
                        "搜索某项特定问题或攻略（如查旅游路线/装机教程）",
                        "查看某位博主的特定最新分析文章",
                        "发布某项具体的求助或内容"
                    ),
                    lowQualityExamples = listOf(
                        "无聊刷热门信息流与八卦热搜",
                        "随便看看消遣",
                        "打发碎片时间"
                    )
                )
            }
            name.contains("会议") || name.contains("钉钉") || name.contains("飞书") ||
                    name.contains("企业微信") || name.contains("wps") || name.contains("文档") ||
                    name.contains("邮箱") || name.contains("github") -> {
                AppMetadata(
                    category = "协同办公与生产力工具",
                    allowedExamples = listOf(
                        "参加指定的线上会议或投屏演示",
                        "查收/回复特定工作邮件或审批工单",
                        "编辑特定文档或提交代码审查"
                    ),
                    lowQualityExamples = listOf(
                        "无具体任务打开挂机"
                    )
                )
            }
            name.contains("地图") || name.contains("导航") || name.contains("滴滴") ||
                    name.contains("12306") || name.contains("打车") || name.contains("高德") -> {
                AppMetadata(
                    category = "出行交通与地图导航",
                    allowedExamples = listOf(
                        "导航至具体目的地",
                        "立即叫车出行",
                        "购买或改签指定车次机票"
                    ),
                    lowQualityExamples = listOf(
                        "随便看地图消遣"
                    )
                )
            }
            else -> {
                AppMetadata(
                    category = "综合应用",
                    allowedExamples = listOf(
                        "处理明确且具体的任务事项",
                        "查阅特定目标信息或执行计划内操作"
                    ),
                    lowQualityExamples = listOf(
                        "无目的闲逛",
                        "习惯性打开",
                        "无聊打发时间"
                    )
                )
            }
        }
    }

    fun getDefaultRuleProfile(packageName: String, appName: String): AppRuleProfile {
        val meta = getAppMetadata(appName)
        return AppRuleProfile(
            packageName = packageName,
            appName = appName,
            allowedRules = meta.allowedExamples,
            forbiddenRules = meta.lowQualityExamples,
            isCustom = false
        )
    }

    fun buildAppRulePrompt(profile: AppRuleProfile): String {
        val allowedStr = if (profile.allowedRules.isNotEmpty()) {
            profile.allowedRules.joinToString("\n") { "  - $it" }
        } else {
            "  - 具有具体、明确且有意识的使用目的"
        }
        val forbiddenStr = if (profile.forbiddenRules.isNotEmpty()) {
            profile.forbiddenRules.joinToString("\n") { "  - $it" }
        } else {
            "  - 无目的刷流、消遣打发时间、习惯性打开"
        }

        return """
【目标 App「${profile.appName}」专属自律判定规则】：
1. 允许使用的正向目的（应当 ALLOW）：
$allowedStr

2. 严禁放行的低质量消遣（应当 DENY 或 RETRY 追问）：
$forbiddenStr
""".trimIndent()
    }

    fun buildDynamicContextPrompt(appName: String): String {
        val defaultProfile = getDefaultRuleProfile("", appName)
        return buildAppRulePrompt(defaultProfile)
    }
}
