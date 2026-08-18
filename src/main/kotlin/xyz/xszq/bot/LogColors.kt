package xyz.xszq.bot

import ch.qos.logback.classic.pattern.ClassicConverter
import ch.qos.logback.classic.spi.ILoggingEvent
import io.github.oshai.kotlinlogging.KotlinLogging

val recvGroupAtLogger = KotlinLogging.logger("bot.recv.group.at")
val recvGroupLogger = KotlinLogging.logger("bot.recv.group")
val recvGroupBotLogger = KotlinLogging.logger("bot.recv.group.bot")
val recvC2CLogger = KotlinLogging.logger("bot.recv.c2c")
val sendGroupLogger = KotlinLogging.logger("bot.send.group")
val sendC2CLogger = KotlinLogging.logger("bot.send.c2c")
val eventLogger = KotlinLogging.logger("bot.event")

val errorLogger = KotlinLogging.logger("bot.error")

private const val ESC = '\u001B'
private const val ANSI_RESET = "$ESC[0m"

class LogColorConverter : ClassicConverter() {
    override fun convert(event: ILoggingEvent): String {
        val text = "%-5s %s".format(event.level.levelStr, event.formattedMessage)
        val color = when (event.loggerName) {
            "bot.recv.group.at" -> "1;36"
            "bot.recv.group" -> "38;5;159"
            "bot.recv.group.bot" -> "90"
            "bot.recv.c2c" -> "38;5;208"
            "bot.send.group" -> "92"
            "bot.send.c2c" -> "38;5;215"
            "bot.event" -> "95"
            "bot.error" -> "1;31"
            else -> return text
        }
        return "$ESC[${color}m$text$ANSI_RESET"
    }
}