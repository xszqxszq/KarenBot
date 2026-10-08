package xyz.xszq.bot.feedback.service

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeFormat
import xyz.xszq.bot.feedback.FeedbackEmailConfig
import xyz.xszq.bot.feedback.FeedbackTicket
import xyz.xszq.bot.feedback.FeedbackType
import java.util.Properties

/**
 * 反馈邮件发送
 */
class FeedbackEmailer(
    private val config: FeedbackEmailConfig
) {
    private val logger = KotlinLogging.logger {}
    private val session: Session = Session.getInstance(Properties().apply {
        this["mail.smtp.auth"] = "true"
        this["mail.smtp.ssl.enable"] = "true"
        this["mail.smtp.host"] = config.smtpHost
        this["mail.smtp.port"] = config.smtpPort.toString()
        this["mail.smtp.connectiontimeout"] = "10000"
        this["mail.smtp.timeout"] = "20000"
        this["mail.smtp.writetimeout"] = "20000"
    })

    /**
     * 向工单用户发送处理结果
     *
     * @return 已发出邮件的 Message-ID
     */
    suspend fun reply(ticket: FeedbackTicket): String? {
        if (ticket.email.isBlank())
            return null
        val nonce = ticket.id.replace("-", "").take(12) +
            System.currentTimeMillis().toString().takeLast(5)
        val domain = config.address.substringAfter("@")
        val messageId = "<karenbot.$nonce@$domain>"
        return withContext(Dispatchers.IO) {
            runCatching {
                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(config.address, "可怜BOT反馈"))
                    setRecipients(
                        Message.RecipientType.TO,
                        InternetAddress.parse(ticket.email)
                    )
                    subject = "[可怜BOT] 反馈结果 #${ticket.id.take(8)}"
                    setHeader("Message-ID", messageId)
                    ticket.emailMessageId ?.let {
                        setHeader("In-Reply-To", it)
                        setHeader("References", it)
                    }
                    setHeader("Auto-Submitted", "auto-replied")
                    setText(bodyOf(ticket), Charsets.UTF_8.name())
                    saveChanges()
                }
                Transport.send(message, config.address, config.password)
                messageId
            }.onFailure { e ->
                logger.warn {
                    "[反馈] 向 ${ticket.email} 发送结果失败 ${e.message}"
                }
            }.getOrNull()
        }
    }

    private fun bodyOf(ticket: FeedbackTicket) = buildString {
        appendLine("您在可怜BOT提交的反馈已有结果：")
        appendLine()
        appendLine("反馈类型：${FeedbackType.nameOf(ticket.type)}")
        appendLine("提交时间：${ticket.createdAt.format(EMAIL_DATE)}")
        appendLine("反馈编号：${ticket.id.take(8)}")
        appendLine()
        appendLine("您提交的反馈内容：")
        appendLine(ticket.body.trim())
        appendLine()
        appendLine("管理员回复如下：")
        appendLine(ticket.reply.trim())
        appendLine()
        appendLine("此邮件由系统自动发送，无需回复。")
        appendLine(config.webUrl)
    }.trim()

    private companion object {
        val EMAIL_DATE: DateTimeFormat<LocalDateTime> = LocalDateTime.Format {
            year()
            chars("-")
            monthNumber()
            chars("-")
            dayOfMonth()
        }
    }
}