@file:Suppress("unused")
package tk.xszq.otomadbot

import com.github.houbb.opencc4j.util.ZhConverterUtil.toSimple
import com.gitlab.mvysny.konsumexml.anyName
import com.gitlab.mvysny.konsumexml.getValueInt
import com.gitlab.mvysny.konsumexml.konsumeXml
import com.soywiz.korio.util.UUID
import kotlinx.coroutines.*
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.*
import net.mamoe.mirai.event.events.GroupEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.code.MiraiCode.deserializeMiraiCode
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.message.isContextIdenticalWith
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import net.mamoe.mirai.utils.MiraiInternalApi
import org.redundent.kotlin.xml.*
import tk.xszq.otomadbot.api.QNAPApi
import tk.xszq.otomadbot.media.downloadFile
import java.io.File

/* Group message channel */
val groupMessages = GlobalEventChannel // Channel of Groups
    .filter { it is GroupEvent }
    .exceptionHandler { exception ->
        exception.printStackTrace()
    }
/**
 * 挂起当前协程, 等待下一条 [MessageEvent.sender] 和 [MessageEvent.subject] 与 [this] 相同且通过 [筛选][filter] 的 [MessageEvent]
 *
 * 若 [filter] 抛出了一个异常, 本函数会立即抛出这个异常.
 *
 * @param timeoutMillis 超时. 单位为毫秒. `-1` 为不限制
 * @param filter 过滤器. 返回非 null 则代表得到了需要的值. [syncFromEvent] 会返回这个值
 *
 * @see syncFromEvent 实现原理
 */
@JvmSynthetic
suspend inline fun <reified P : MessageEvent> P.nextMessageEvent(
    timeoutMillis: Long = -1,
    priority: EventPriority = EventPriority.MONITOR,
    noinline filter: suspend P.(P) -> Boolean = { true }
): MessageEvent {
    return syncFromEvent<P, P>(timeoutMillis, priority) {
        takeIf { this.isContextIdenticalWith(this@nextMessageEvent) }?.takeIf { filter(it, it) }
    }
}
@Suppress("FINAL_UPPER_BOUND")
@JvmSynthetic
suspend inline fun <reified P : GroupMessageEvent> P.nextGroupMessageEvent(
    timeoutMillis: Long = -1,
    priority: EventPriority = EventPriority.MONITOR,
    noinline filter: suspend P.(P) -> Boolean = { true }
): GroupMessageEvent {
    return syncFromEvent<P, P>(timeoutMillis, priority) {
        takeIf { this.isGroupIdenticalWith(this@nextGroupMessageEvent) }?.takeIf { filter(it, it) }
    }
}

typealias MessageListener<T, R> = @MessageDsl suspend T.(String) -> R
@MessageDsl
internal fun <M : MessageEvent, Ret, R : RR, RR> MessageSubscribersBuilder<M, Ret, R, RR>.content(
    filter: M.(String) -> Boolean,
    onEvent: MessageListener<M, RR>
): Ret =
    subscriber(filter) { onEvent(this, it) }

@MessageDsl
fun <M : MessageEvent, Ret, R : RR, RR> MessageSubscribersBuilder<M, Ret, R, RR>
        .equalsTo(equals: String, ignoreCase: Boolean = false, trim: Boolean = true,
                  onEvent: MessageListener<M, R>): Ret
        = (if (trim) equals.trim() else equals).let { toCheck ->
    content({ toSimple(if (trim) it.trim() else it).equals(toCheck, ignoreCase = ignoreCase) }) {
        onEvent(this, this.message.contentToString())
    }
}

internal fun <M : MessageEvent, Ret, R : RR, RR> MessageSubscribersBuilder<M, Ret, R, RR>.startsWithSimpleImpl(
    prefix: String,
    removePrefix: Boolean = true,
    trim: Boolean = true,
    onEvent: @MessageDsl suspend M.(String, String) -> R
): Ret {
    return if (trim) {
        val toCheck = prefix.trim()
        content({ it.toSimple().toLowerCase().trimStart().startsWith(toCheck) }) {
            if (removePrefix) this.onEvent(this.message.contentToString().toSimple().toLowerCase().substringAfter(toCheck).trim(), this.message.contentToString())
            else onEvent(this, this.message.contentToString().toSimple().toLowerCase().trim(), this.message.contentToString())
        }
    } else content({ it.toSimple().toLowerCase().startsWith(prefix) }) {
        if (removePrefix) this.onEvent(this.message.contentToString().toSimple().toLowerCase().removePrefix(prefix), this.message.contentToString())
        else onEvent(this, this.message.contentToString().toSimple().toLowerCase(), this.message.contentToString())
    }
}
@MessageDsl
fun <M : MessageEvent, Ret, R : RR, RR> MessageSubscribersBuilder<M, Ret, R, RR>.startsWithSimple(
    prefix: String, removePrefix: Boolean = true, trim: Boolean = true,
    onEvent: @MessageDsl suspend M.(String, String) -> R
): Ret = startsWithSimpleImpl(prefix, removePrefix, trim, onEvent)
fun GroupMessageEvent.isGroupIdenticalWith(another: GroupMessageEvent): Boolean {
    return this.group == another.group
}

suspend fun MessageEvent.quoteReply(message: Message): MessageReceipt<Contact> =
    this.subject.sendMessage(this.message.quote() + message)
suspend fun MessageEvent.quoteReply(message: String): MessageReceipt<Contact> = quoteReply(message.toPlainText())
suspend fun GroupMessageEvent.quoteReply(message: Message): MessageReceipt<Group> =
    this.group.sendMessage(this.message.quote() + message)
suspend fun GroupMessageEvent.quoteReply(message: String): MessageReceipt<Group> = quoteReply(message.toPlainText())

@Suppress("UNCHECKED_CAST")
fun Bot.subscribeCommand(groupOnly: Boolean, context: MessageEventSubscribersBuilder.() -> Listener<MessageEvent>) {
    if (groupOnly)
        eventChannel.subscribeGroupMessages(
            listeners=context as GroupMessageSubscribersBuilder.() -> Listener<GroupMessageEvent>)
    else
        eventChannel.subscribeMessages(listeners=context)
}
class QQXMLMessage(private val builder: Builder.() -> Unit) {
    @DslMarker annotation class QQXMLBuilder
    @DslMarker annotation class QQXMLSettingBuilder
    @QQXMLBuilder
    inner class Builder {
        @QQXMLSettingBuilder
        inner class XMLSetting {
            var xmlVersion = 1.0
            var xmlEncoding = "utf-8"
            fun xmlVersion(s: Double) { xmlVersion = s }
            fun xmlEncoding(s: String) { xmlEncoding = s }
        }
        var xmlSetting = XMLSetting()
        var templateID = 12345
        var action = "web"
        var brief = "[XML]"
        var serviceID = 1
        var url = ""
        var layout = 2
        var title = ""
        var summary = ""
        var cover = ""
        fun xmlSetting(builder: XMLSetting.() -> Unit) = xmlSetting.apply(builder)
        fun templateID(s: Int) { templateID = s }
        fun action(s: String) { action = s }
        fun brief(s: String) { brief = s }
        fun serviceID(s: Int) { serviceID = s }
        fun url(s: String) { url = s }
        fun layout(s: Int) { layout = s }
        fun title(s: String) { title = s.escape() }
        fun summary(s: String) { summary = s }
        fun cover(s: String) { cover = s }
    }
    fun build() = Builder().apply(builder).run {
        """<?xml version="${xmlSetting.xmlVersion}" encoding="${xmlSetting.xmlEncoding}"?>""" +
        """<msg templateID="$templateID" action="$action" brief="$brief" serviceID="$serviceID" url="$url">""" +
            """<item layout="$layout">""" +
                """<title>$title</title>""" +
                """<summary>$summary</summary>""" +
                """<picture cover="$cover"/>""" +
            "</item>" +
        "</msg>"
    }
}

suspend fun MessageChain.toXML(): String {
    val result = xml("xml", version = XmlVersion.V10)
    forEach { msg ->
        when (msg) {
            is PlainText -> result.addNode(node("pre") {
                -msg.content
            })
            is Face -> result.addNode(node("face") {
                attribute("id", msg.id)
            })
            is FlashImage -> {
                val img = downloadFile(msg.image.queryUrl(), UUID.randomUUID().toString(),
                    path="$pathPrefix/image/message")
                result.addNode(node("img") {
                    attribute("type", "flash")
                    attribute("src", img.absolutePath)
                })
            }
            is Image -> {
                val img = downloadFile(msg.queryUrl(), UUID.randomUUID().toString(),
                    path="$pathPrefix/image/message")
                result.addNode(node("img") {
                    attribute("type", "normal")
                    attribute("src", img.absolutePath)
                })
            }
            is Voice -> {
                val voice = downloadFile(msg.url!!, UUID.randomUUID().toString(),
                    path="$pathPrefix/voice/message")
                result.addNode(node("audio") {
                    attribute("src", voice.absolutePath)
                })
            }
            is At -> result.addNode(node("at") {
                attribute("target", msg.target)
            })
            is AtAll -> result.addNode(node("atall"))
            is FileMessage -> result.addNode(node("file") {
                -msg.serializeToMiraiCode()
            })
            is ForwardMessage -> result.addNode(node("forward") {
                -msg.toString()
            })
            is MusicShare -> result.addNode(node("music") {
                -msg.serializeToMiraiCode()
            })
            is LightApp -> result.addNode(node("lightapp") {
                "pre" {
                    -msg.content
                }
            })
            is ServiceMessage -> result.addNode(node("sxml") {
                "pre" {
                    -msg.content
                }
            })
            is MarketFace -> result.addNode(node("pre") { // Unsupported yet
                -msg.toString()
            })
            else -> {}
        }
    }
    return result.toString(PrintOptions(
        singleLineTextElements = true,
        useSelfClosingTags = true
    ))
}
// TODO: Make this be working
@Suppress("EXPERIMENTAL_API_USAGE")
suspend fun String.xmlToMessageChain(subject: Contact): MessageChain {
    val result = mutableListOf<Message>()
    konsumeXml().children(anyName) {
        when (localName) {
            "pre" -> result.add(text().toPlainText())
            "face" -> result.add(Face(attributes.getValueInt("id")))
            "img" -> runBlocking {
                val image = File(attributes.getValue("src")).uploadAsImage(subject)
                when (attributes.getValue("type")) {
                    "normal" -> result.add(image)
                    "flash" -> result.add(FlashImage.from(image))
                    else -> pass
                }
            }
            "audio" -> runBlocking {
                if (subject is Group) {
                    result.add(subject.uploadVoice(File(attributes.getValue("src")).toExternalResource()))
                } else {
                    result.add("[语音]".toPlainText())
                }
            }
            "at" -> result.add(At(attributes.getValue("target").toLong()))
            "atall" -> result.add(AtAll)
            "file" -> result.add(deserializeMiraiCode(text()))
            "forward" -> result.add(deserializeMiraiCode(text()))
            "music" -> result.add(deserializeMiraiCode(text()))
            "lightapp" -> children("pre") { result.add(LightApp(text())) }
            "sxml" -> children("pre") { result.add(SimpleServiceMessage(60, text())) }
        }
        pass
    }
    return result.toMessageChain()
}

class MiraiMonitor(val bot: Bot) {
    @MiraiInternalApi
    @Suppress("EXPERIMENTAL_API_USAGE")
    suspend fun launch() {
        GlobalScope.launch gsl@{
            while (true) {
                if (!bot.isOnline && !debugMode) {
                    delay(15000)
                    if (!bot.isOnline)
                        QNAPApi().restart(configMain.qnap.dockerId)
                }
                delay(180000)
            }
        }
    }
}