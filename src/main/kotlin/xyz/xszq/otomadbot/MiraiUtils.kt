@file:Suppress("unused")

package xyz.xszq.otomadbot

import com.github.houbb.opencc4j.util.ZhConverterUtil.toSimple
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.mamoe.mirai.console.permission.Permission
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.permission.PermitteeId.Companion.permitteeId
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.event.*
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.data.MarketFace
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.message.data.toPlainText
import net.mamoe.mirai.message.isContextIdenticalWith

@PublishedApi // inline, safe to remove in the future
internal inline fun <reified P : MessageEvent>
        P.createMapper(crossinline filter: suspend P.(P) -> Boolean): suspend (P) -> P? =
    mapper@{ event ->
        if (!event.isContextIdenticalWith(this)) return@mapper null
        if (!filter(event, event)) return@mapper null
        event
    }
@PublishedApi // inline, safe to remove in the future
internal inline fun <reified P : MessageEvent>
        P.createMapperForGroup(crossinline filter: suspend P.(P) -> Boolean): suspend (P) -> P? =
    mapper@{ event ->
        if (event !is GroupMessageEvent) return@mapper null
        if (!event.isGroupIdenticalWith(this as GroupMessageEvent)) return@mapper null
        if (!filter(event, event)) return@mapper null
        event
    }
@JvmSynthetic
suspend inline fun <reified P : MessageEvent> P.nextMessageEvent(
    timeoutMillis: Long = -1,
    priority: EventPriority = EventPriority.MONITOR,
    noinline filter: suspend P.(P) -> Boolean = { true }
): MessageEvent {
    val mapper: suspend (P) -> P? = createMapper(filter)

    return (if (timeoutMillis == -1L) {
        GlobalEventChannel.syncFromEvent(priority, mapper)
    } else {
        withTimeout(timeoutMillis) {
            GlobalEventChannel.syncFromEvent(priority, mapper)
        }
    })
}
@Suppress("FINAL_UPPER_BOUND")
@JvmSynthetic
suspend inline fun <reified P : GroupMessageEvent> P.nextGroupMessageEvent(
    timeoutMillis: Long = -1,
    priority: EventPriority = EventPriority.MONITOR,
    noinline filter: suspend P.(P) -> Boolean = { true }
): GroupMessageEvent {
    val mapper = createMapperForGroup(filter)

    return (if (timeoutMillis == -1L) {
        GlobalEventChannel.syncFromEvent(priority, mapper)
    } else {
        withTimeout(timeoutMillis) {
            GlobalEventChannel.syncFromEvent(priority, mapper)
        }
    })
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
        .equalsTo(equals: String, ignoreCase: Boolean = true, trim: Boolean = true,
                  onEvent: MessageListener<M, R>): Ret
        = (if (trim) equals.trim() else equals).let { toCheck ->
    content({ toSimple(if (trim) it.trim() else it).equals(toCheck, ignoreCase = ignoreCase) }) {
        onEvent(this, this.message.contentToString())
    }
}

fun String.substringAfterPrefix(start: String): String = substring(start.length)
fun String.substringBeforeSuffix(suffix: String): String = substring(0, suffix.length)

internal fun <M : MessageEvent, Ret, R : RR, RR> MessageSubscribersBuilder<M, Ret, R, RR>.startsWithSimpleImpl(
    prefix: String,
    removePrefix: Boolean = true,
    trim: Boolean = true,
    onEvent: @MessageDsl suspend M.(String, String) -> R
): Ret {
    return if (trim) {
        val toCheck = prefix.trim()
        content({ it.toSimple().lowercase().trimStart().startsWith(toCheck) }) {
            if (removePrefix) onEvent(message.contentToString().toSimple().lowercase().substringAfter(toCheck).trim(),
                message.contentToString().substringAfterPrefix(toCheck).trim())
            else onEvent(this, message.contentToString().toSimple().lowercase().trim(),
                message.contentToString().trim())
        }
    } else content({ it.toSimple().lowercase().startsWith(prefix) }) {
        if (removePrefix) onEvent(message.contentToString().toSimple().lowercase().removePrefix(prefix),
            message.contentToString().substringAfterPrefix(prefix).trim())
        else onEvent(this, message.contentToString().toSimple().lowercase(),
            message.contentToString().trim())
    }
}

internal fun <M : MessageEvent, Ret, R : RR, RR> MessageSubscribersBuilder<M, Ret, R, RR>.endsWithSimpleImpl(
    suffix: String,
    removeSuffix: Boolean = true,
    trim: Boolean = true,
    onEvent: @MessageDsl suspend M.(String, String) -> R
): Ret {
    return if (trim) {
        val toCheck = suffix.trimEnd()
        content({ it.toSimple().lowercase().trimEnd().endsWith(toCheck) }) {
            if (removeSuffix) onEvent(this.message.contentToString().toSimple().lowercase().removeSuffix(toCheck).trim(),
                this.message.contentToString().substringBeforeSuffix(toCheck).trim())
            else onEvent(this, this.message.contentToString().toSimple().lowercase().trim(),
                this.message.contentToString().trim())
        }
    } else content({ it.toSimple().lowercase().endsWith(suffix) }) {
        if (removeSuffix) onEvent(message.contentToString().toSimple().lowercase().removeSuffix(suffix),
            message.contentToString().substringBeforeSuffix(suffix).trim())
        else onEvent(this, message.contentToString().toSimple().lowercase(),
            message.contentToString().trim())
    }

}

/**
 * startsWith对于繁体增加支持的版本
 * M.(简体化且小写后的参数, 原始参数) -> R
 */
@MessageDsl
fun <M : MessageEvent, Ret, R : RR, RR> MessageSubscribersBuilder<M, Ret, R, RR>.startsWithSimple(
    prefix: String, removePrefix: Boolean = true, trim: Boolean = true,
    onEvent: @MessageDsl suspend M.(String, String) -> R
): Ret = startsWithSimpleImpl(prefix, removePrefix, trim, onEvent)

/**
 * endsWith对于繁体增加支持的版本
 * M.(简体化且小写后的参数, 原始参数) -> R
 */
@MessageDsl
fun <M : MessageEvent, Ret, R : RR, RR> MessageSubscribersBuilder<M, Ret, R, RR>.endsWithSimple(
    suffix: String, removeSuffix: Boolean = true, trim: Boolean = true,
    onEvent: @MessageDsl suspend M.(String, String) -> R
): Ret = endsWithSimpleImpl(suffix, removeSuffix, trim, onEvent)


fun GroupMessageEvent.isGroupIdenticalWith(another: GroupMessageEvent): Boolean {
    return this.group == another.group
}

suspend fun <T> GroupEvent.require(permission: Permission, block: suspend () -> T): T? = when {
    group.permitteeId.hasPermission(permission) -> block.invoke()
    else -> null
}
suspend fun <T> GroupEvent.requireNot(permission: Permission, block: suspend () -> T): T? = when {
    group.permitteeId.hasPermission(permission) -> null
    else -> block.invoke()
}
suspend fun <T> NudgeEvent.requireNot(permission: Permission, block: suspend () -> T): T? = when {
    subject is Group && (subject as Group).permitteeId.hasPermission(permission) -> null
    subject is User && (subject as User).permitteeId.hasPermission(permission) -> null
    else -> block.invoke()
}
suspend fun <T> MessageEvent.require(permission: Permission, block: suspend () -> T): T? = when {
    this is GroupMessageEvent -> {
        if (group.permitteeId.hasPermission(permission))
            block.invoke()
        else
            null
    }
    sender.permitteeId.hasPermission(permission) -> block.invoke()
    else -> null
}
suspend fun <T> GroupMessageEvent.require(permission: Permission, block: suspend () -> T): T? = when {
    group.permitteeId.hasPermission(permission) -> block.invoke()
    else -> null
}
suspend fun <T> MessageEvent.requireNot(permission: Permission, block: suspend () -> T): T? = when {
    this is GroupMessageEvent -> {
        if (group.permitteeId.hasPermission(permission))
            null
        else
            block.invoke()
    }
    sender.permitteeId.hasPermission(permission) -> null
    else -> block.invoke()
}
suspend fun <T> GroupMessageEvent.requireNot(permission: Permission, block: suspend () -> T): T? =
    if (group.permitteeId.hasPermission(permission))
        null
    else
        block.invoke()
suspend fun <T> MessageEvent.requireSender(permission: Permission, block: suspend () -> T): T? = when {
    sender.permitteeId.hasPermission(permission) -> block.invoke()
    else -> null
}
suspend fun <T> MessageEvent.requireSenderOr(permission: Permission, bool: Boolean, block: suspend () -> T): T? =
    if (sender.permitteeId.hasPermission(permission) || bool)
        block.invoke()
    else
        null
suspend fun <T> MessageEvent.requireSenderNot(permission: Permission, block: suspend () -> T): T? = when {
    sender.permitteeId.hasPermission(permission) -> null
    else -> block.invoke()
}
suspend fun <T> MessageEvent.requireOr(permission: Permission, bool: Boolean, block: suspend () -> T): T? =
    if (sender.permitteeId.hasPermission(permission) || bool)
        block.invoke()
    else
        null
suspend fun <T> GroupMessageEvent.requireOr(permission: Permission, bool: Boolean, block: suspend () -> T): T? =
    if (group.permitteeId.hasPermission(permission) || bool)
        block.invoke()
    else
        null
suspend fun <T> MessageEvent.requireBotAdmin(block: suspend () -> T): T? = requireSender(AdminEventHandler.botAdmin,
    block)
suspend fun <T> GroupMessageEvent.requireOperator(block: suspend () -> T): T? = requireSenderOr(AdminEventHandler.botAdmin,
    sender.isOperator(), block)
suspend fun <T> MessageEvent.onlyContact(block: suspend () -> T): T? = when (this) {
    is GroupMessageEvent -> block.invoke()
    is FriendMessageEvent -> block.invoke()
    else -> null
}

@Serializable
data class MarketFaceXYData(
    val appData: Map<String, String> = mapOf(), val timestamp: Long = 0L, val data: MarketFaceData
)
@Serializable
data class MarketFaceData(
    val baseInfo: List<MarketFaceBaseInfo> = listOf(), val md5Info: List<MarketFaceMd5Info>
)
@Serializable
data class MarketFaceBaseInfo(
    val name: String = "", val _id: String = "", val id: String = ""
)
@Serializable
data class MarketFaceMd5Info(val name: String, val md5: String)
@Suppress("EXPERIMENTAL_API_USAGE")
suspend fun MarketFace.getAttr() = HttpClient {
    install(ContentNegotiation) {
        json(Json {
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
}
    .get("https://gxh.vip.qq.com/qqshow/admindata/comdata/vipEmoji_item_$id/xydata.json")
    .body<MarketFaceXYData>().data
suspend fun MarketFace.queryUrl(size: Int = 300) = getAttr().md5Info.run {
    val md5 = find { "[${it.name}]" == name } ?.md5 ?: first().md5
    "https://gxh.vip.qq.com/club/item/parcel/item/${md5.subSequence(0, 2)}/$md5/${size}x${size}.png"
}


suspend fun MessageEvent.quoteReply(message: Message): MessageReceipt<Contact> =
    this.subject.sendMessage(this.message.quote() + message)
suspend fun MessageEvent.quoteReply(message: String): MessageReceipt<Contact> = quoteReply(message.toPlainText())
suspend fun GroupMessageEvent.quoteReply(message: Message): MessageReceipt<Group> =
    this.group.sendMessage(this.message.quote() + message)
suspend fun GroupMessageEvent.quoteReply(message: String): MessageReceipt<Group> = quoteReply(message.toPlainText())

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
