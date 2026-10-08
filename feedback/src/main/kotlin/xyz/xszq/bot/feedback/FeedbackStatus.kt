package xyz.xszq.bot.feedback

/**
 * 工单处理状态
 */
enum class FeedbackStatus(
    val value: String,
    val label: String,
    val icon: String
) {
    Open("open", "待处理", "🕒"),
    Solved("solved", "已解决", "✅"),
    Closed("closed", "已关闭", "🔒");

    companion object {
        fun iconOf(value: String) =
            entries.firstOrNull { it.value == value } ?.icon ?: "❓"

        fun nameOf(value: String) =
            entries.firstOrNull { it.value == value } ?.label ?: value
    }
}