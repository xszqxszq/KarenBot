package xyz.xszq.bot.feedback

/**
 * 反馈类别
 */
enum class FeedbackType(
    val value: String,
    val label: String,
    val icon: String,
    val hint: String
) {
    Bug("bug", "Bug 反馈", "🐛", "请填写你遇到的 Bug"),
    Suggestion("suggestion", "新功能建议", "💡", "请填写你想要的新功能"),
    Abuse("abuse", "举报滥用", "🚨", "请填写要举报的滥用行为"),
    Email("email", "邮箱反馈", "📧", "通过邮件收到的反馈。"),
    Alias("alias", "举报别名", "🏷️", "请填写举报该别名的理由");

    companion object {
        fun of(value: String) = entries.firstOrNull { it.value == value }

        fun nameOf(value: String) = of(value) ?.label ?: value

        fun iconOf(value: String) = of(value) ?.icon ?: "❓"
    }
}