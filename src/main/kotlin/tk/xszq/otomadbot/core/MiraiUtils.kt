@file:Suppress("unused")

package tk.xszq.otomadbot.core

import net.mamoe.mirai.console.permission.Permission
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.permission.PermitteeId.Companion.permitteeId
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.events.GroupEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.events.NudgeEvent
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.message.data.toPlainText

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
suspend fun <T> GroupMessageEvent.requireSender(permission: Permission, block: suspend () -> T): T? = when {
    sender.permitteeId.hasPermission(permission) -> block.invoke()
    else -> null
}
suspend fun <T> GroupMessageEvent.requireSenderNot(permission: Permission, block: suspend () -> T): T? = when {
    sender.permitteeId.hasPermission(permission) -> null
    else -> block.invoke()
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
