package com.example.myapplication.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// 温润奶咖与陶土色系语义 Token (Warm Light & Terracotta)
// ==========================================

val WarmBackground = Color(0xFFFAF7F2)          // 燕麦暖白底色
val WarmSurface = Color(0xFFFFFFFF)             // 纯净暖白卡片
val WarmSurfaceContainer = Color(0xFFF4EFEB)    // 柔杏奶咖次级容器
val WarmSurfaceContainerHigh = Color(0xFFEAE2DA)// 强调容器背景
val WarmBorder = Color(0xFFE8E0D5)             // 浅暖微弱边框 (1dp)
val WarmBorderLight = Color(0xFFF0EAE2)        // 极淡分隔线

val TerracottaPrimary = Color(0xFFC25E3E)      // 陶土暖红棕 (主色)
val TerracottaPrimaryContainer = Color(0xFFFBECE6) // 浅陶土柔粉
val OnTerracottaPrimary = Color(0xFFFFFFFF)    // 主色文字
val OnTerracottaContainer = Color(0xFF7A2E16)  // 深陶土文字

val CaramelSecondary = Color(0xFF8D5B3E)       // 焦糖暖咖 (辅助色)
val CaramelSecondaryContainer = Color(0xFFF5ECE4) // 浅焦糖背景

val SageGreen = Color(0xFF5C8A60)              // 鼠尾草柔绿 (成功/放行/开启)
val SageGreenContainer = Color(0xFFEBF3EB)     // 浅鼠尾草背景
val OnSageGreen = Color(0xFFFFFFFF)
val OnSageGreenContainer = Color(0xFF2B522E)

val AmberWarm = Color(0xFFD9822B)              // 暖琥珀 (警示/关注/提示)
val AmberWarmContainer = Color(0xFFFDF3E7)     // 浅暖琥珀背景
val OnAmberWarmContainer = Color(0xFF78450E)

val CoralDanger = Color(0xFFD34545)            // 柔珊瑚红 (驳回/抵制/危险)
val CoralDangerContainer = Color(0xFFFCEAEA)   // 浅珊瑚红背景
val OnCoralDangerContainer = Color(0xFF751C1C)

val WarmTextPrimary = Color(0xFF2C2420)        // 深暖炭咖 (主标题/正文)
val WarmTextSecondary = Color(0xFF70645C)      // 暖灰咖 (次要信息)
val WarmTextMuted = Color(0xFF9E928A)          // 浅咖灰 (时间/提示)

// ==========================================
// 兼容旧语义常量 (重定向至温润奶咖色系)
// ==========================================
val CyberCyan = TerracottaPrimary
val NeonGreen = SageGreen
val ElectricPurple = CaramelSecondary
val WarningRed = CoralDanger

val DarkBg = WarmBackground
val CardBg = WarmSurface
val SurfaceDark = WarmSurfaceContainer
val BorderDark = WarmBorder

val TextPrimary = WarmTextPrimary
val TextSecondary = WarmTextSecondary
val TextMuted = WarmTextMuted