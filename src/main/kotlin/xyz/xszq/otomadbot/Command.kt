@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xszq.otomadbot

import net.mamoe.mirai.console.permission.Permission
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.permission.PermitteeId.Companion.permitteeId
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.AbstractEvent
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.events.*
import xyz.xszq.otomadbot.image.ImageReceivedEvent
import xyz.xszq.otomadbot.kotlin.Args
import kotlin.properties.Delegates

open class Command<T: Event>(
    open val name: String, open val permName: String, open val defaultEnabled: Boolean = true,
    open val checkSender: Boolean = false, open val block: suspend T.()->Unit
) {
    lateinit var parent: CommandModule
    var perm by Delegates.notNull<Permission>()
    var hint: String = ""
    fun hasPerm(event: Event, perm: Permission = this.perm): Boolean = when {
        event is GroupEvent && !checkSender -> event.group.permitteeId.hasPermission(perm)
        event is MessageEvent -> event.sender.permitteeId.hasPermission(perm)
        event is FriendEvent -> event.friend.permitteeId.hasPermission(perm)
        event is UserEvent -> event.user.permitteeId.hasPermission(perm)
        event is NudgeEvent && event.subject is Group -> (event.subject as Group).permitteeId.hasPermission(perm)
        event is ImageReceivedEvent -> if (event.event is GroupMessageEvent) event.event.group.permitteeId
            .hasPermission(perm) else event.event.sender.permitteeId.hasPermission(perm)
        event is CommandEvent<*> -> hasPerm(event.event)
        else -> true
    }
    fun noPerm(event: Event, perm: Permission = this.perm): Boolean = when {
        event is GroupEvent && !checkSender -> !event.group.permitteeId.hasPermission(perm)
        event is MessageEvent -> !event.sender.permitteeId.hasPermission(perm)
        event is FriendEvent -> !event.friend.permitteeId.hasPermission(perm)
        event is UserEvent -> !event.user.permitteeId.hasPermission(perm)
        event is NudgeEvent && event.subject is Group -> !(event.subject as Group).permitteeId.hasPermission(perm)
        event is ImageReceivedEvent -> if (event.event is GroupMessageEvent) !event.event.group.permitteeId
            .hasPermission(perm) else !event.event.sender.permitteeId.hasPermission(perm)
        event is CommandEvent<*> -> noPerm(event.event)
        else -> true
    }
    suspend fun checkAndRun(event: T) {
        if (noPerm(event, parent.denyPerm) &&
            ((defaultEnabled && noPerm(event)) || (!defaultEnabled && hasPerm(event))))
            block.invoke(event)
    }
}


typealias CommonCommand = Command<MessageEvent>
typealias GroupCommand = Command<GroupMessageEvent>
typealias CommonCommandWithArgs = Command<CommandEvent<MessageEvent>>
typealias GroupCommandWithArgs = Command<CommandEvent<GroupMessageEvent>>

class CommandEvent<T: MessageEvent>(
    val args: Args,
    val event: T
): AbstractEvent()