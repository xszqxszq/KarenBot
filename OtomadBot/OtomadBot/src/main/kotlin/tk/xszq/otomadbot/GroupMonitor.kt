@file:Suppress("UNUSED")
package tk.xszq.otomadbot

import io.ktor.util.collections.*
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.Face
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.anyIsInstance
import net.mamoe.mirai.message.data.content
import tk.xszq.otomadbot.api.PyApi
import tk.xszq.otomadbot.api.Sentiment

data class AnalysedContent(val text: GroupMessageEvent, var sentiment: Sentiment? = null) {
    override fun equals(other: Any?): Boolean = when(other) {
        is AnalysedContent -> text.message.content == other.text.message.content
        is GroupMessageEvent -> text.message.content == other.message.content
        else -> false
    }
    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + (sentiment?.hashCode() ?: 0)
        return result
    }
}

@Suppress("MemberVisibilityCanBePrivate", "EXPERIMENTAL_API_USAGE")
class GroupMonitor(val bot: Bot, val group: Long, var hint: String = configMain.sentiment.hint) {
    var maxInQueue = configMain.sentiment.maxInQueue
    var scale = configMain.sentiment.scale
    var threshold = configMain.sentiment.threshold
    private var contentQueue = ConcurrentList<AnalysedContent>()
    fun clear() = contentQueue.clear()
    fun clearSentiment() = contentQueue.forEach {
        it.sentiment = null
    }
    fun refresh() {
        contentQueue.removeIf{
            System.currentTimeMillis() - it.text.time * 1000L <= configMain.cooldown["group"]!!.toLong()
        }
        while (contentQueue.size > maxInQueue)
            contentQueue.removeFirst()
    }
    suspend fun insert(text: GroupMessageEvent) {
        refresh()
        when {
            text.message.anyIsInstance<Face>() -> {
                text.message.filterIsInstance<Face>().forEach {
                    if (it.id in arrayOf(0, 19, 13))
                        contentQueue.add(AnalysedContent(text, Sentiment(false, 0.8)))
                }
            }
            text.message.anyIsInstance<PlainText>() && contentQueue.none { it.equals(text) } -> contentQueue
                .add(AnalysedContent(text, PyApi().getSentiment(text.message.content)))
            else -> pass
        }
        refresh()
    }
    fun getNegativeSum(): Double {
        refresh()
        var sum = 0.0
        contentQueue.forEach { content ->
            content.sentiment?.let { sum += if (!it.positive) it.value * scale else -(1-it.value) }
        }
        return sum
    }
    suspend fun routineCheck() {
        val nSum = getNegativeSum()
        bot.logger.debug(nSum.toString())
        if (nSum > threshold * scale) {
            bot.getGroupOrFail(group).sendMessage(hint)
            clearSentiment()
        }
    }
}

val groupMonitors = hashMapOf<Long, GroupMonitor>()