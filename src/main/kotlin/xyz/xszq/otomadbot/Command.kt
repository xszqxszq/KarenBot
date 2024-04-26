@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xszq.otomadbot

import net.mamoe.mirai.console.permission.Permission
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.permission.PermitteeId.Companion.permitteeId
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.events.*
import xyz.xszq.otomadbot.kotlin.Args
import java.io.File
import kotlin.properties.Delegates

open class Command<T: Event, A: Any>(
    open val name: String, open val permName: String, open val defaultEnabled: Boolean = true,
    open val checkSender: Boolean = false, open val block: suspend T.(A?)->Unit
) {
    lateinit var parent: CommandModule
    var perm by Delegates.notNull<Permission>()
    fun hasPerm(event: Event, perm: Permission = this.perm): Boolean = when {
        event is GroupEvent && !checkSender -> event.group.permitteeId.hasPermission(perm)
        event is MessageEvent -> event.sender.permitteeId.hasPermission(perm)
        event is FriendEvent -> event.friend.permitteeId.hasPermission(perm)
        event is UserEvent -> event.user.permitteeId.hasPermission(perm)
        event is NudgeEvent && event.subject is Group -> (event.subject as Group).permitteeId.hasPermission(perm)
        else -> true
    }
    fun noPerm(event: Event, perm: Permission = this.perm): Boolean = when {
        event is GroupEvent && !checkSender -> !event.group.permitteeId.hasPermission(perm)
        event is MessageEvent -> !event.sender.permitteeId.hasPermission(perm)
        event is FriendEvent -> !event.friend.permitteeId.hasPermission(perm)
        event is UserEvent -> !event.user.permitteeId.hasPermission(perm)
        event is NudgeEvent && event.subject is Group -> !(event.subject as Group).permitteeId.hasPermission(perm)
        else -> true
    }
    suspend fun checkAndRun(event: T) {
        if (noPerm(event, parent.denyPerm) &&
            ((defaultEnabled && noPerm(event)) || (!defaultEnabled && hasPerm(event))))
            block.invoke(event, null)
    }
    suspend fun checkAndRun(event: T, arg: A) {
        if (noPerm(event, parent.denyPerm) &&
            ((defaultEnabled && noPerm(event)) || (!defaultEnabled && (hasPerm(event, parent.allowPerm) || hasPerm(event)))))
            block.invoke(event, arg)
    }
}


typealias CommonCommand = Command<MessageEvent, Nothing>
typealias GroupCommand = Command<GroupMessageEvent, Nothing>
typealias CommonCommandWithArg = Command<MessageEvent, String>
typealias GroupCommandWithArg = Command<GroupMessageEvent, String>
typealias CommandWithType<T> = Command<T, Nothing>
typealias CommonCommandWithArgOf<T> = Command<MessageEvent, T>
typealias GroupCommandWithArgOf<T> = Command<GroupMessageEvent, T>
typealias CommonCommandWithArgs = Command<MessageEvent, Args>
typealias GroupCommandWithArgs = Command<GroupMessageEvent, Args>
typealias ImageCommand = GroupCommandWithArgOf<List<File>>