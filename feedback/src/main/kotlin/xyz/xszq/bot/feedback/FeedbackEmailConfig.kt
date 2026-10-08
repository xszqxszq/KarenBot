package xyz.xszq.bot.feedback

/**
 * 反馈邮件配置
 */
data class FeedbackEmailConfig(
    val address: String,
    val password: String,
    val smtpHost: String = "smtp.feishu.cn",
    val smtpPort: Int = 465,
    val webUrl: String = "https://docs.karenbot.cn"
)