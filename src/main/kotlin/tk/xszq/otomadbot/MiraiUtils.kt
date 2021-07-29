@file:Suppress("unused")

package tk.xszq.otomadbot

import com.github.houbb.opencc4j.util.ZhConverterUtil.toSimple
import net.mamoe.mirai.console.permission.Permission
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.permission.PermitteeId.Companion.permitteeId
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.MessageDsl
import net.mamoe.mirai.event.MessageSubscribersBuilder
import net.mamoe.mirai.event.events.GroupEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.events.NudgeEvent
import net.mamoe.mirai.event.syncFromEvent
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.message.data.toPlainText
import net.mamoe.mirai.message.isContextIdenticalWith

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
        content({ it.toSimple().lowercase().trimStart().startsWith(toCheck) }) {
            if (removePrefix) this.onEvent(this.message.contentToString().toSimple().lowercase().substringAfter(toCheck).trim(), this.message.contentToString())
            else onEvent(this, this.message.contentToString().toSimple().lowercase().trim(), this.message.contentToString())
        }
    } else content({ it.toSimple().lowercase().startsWith(prefix) }) {
        if (removePrefix) this.onEvent(this.message.contentToString().toSimple().lowercase().removePrefix(prefix), this.message.contentToString())
        else onEvent(this, this.message.contentToString().toSimple().lowercase(), this.message.contentToString())
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
suspend fun <T> GroupMessageEvent.requireSender(permission: Permission, block: suspend () -> T): T? = when {
    sender.permitteeId.hasPermission(permission) -> block.invoke()
    else -> null
}
suspend fun <T> GroupMessageEvent.requireSenderNot(permission: Permission, block: suspend () -> T): T? = when {
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
