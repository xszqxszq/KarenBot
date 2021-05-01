@file:Suppress("unused")
package tk.xszq.otomadbot

import kotlinx.coroutines.currentCoroutineContext
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.event.subscribeMessages
import tk.xszq.otomadbot.database.require
import tk.xszq.otomadbot.database.requireMemberOrAdmin
import tk.xszq.otomadbot.database.requireSender
import kotlin.coroutines.Continuation

annotation class Command(val command: String, val subPermission: String, val groupOnly: Boolean = false,
                         val keepRaw: Boolean = false)
annotation class CommandSingleArg(val command: String, val subPermission: String, val groupOnly: Boolean = false)
annotation class CommandEqualsTo(val command: String, val subPermission: String, val groupOnly: Boolean = false)
annotation class CommandMatching(val regex: String, val subPermission: String, val groupOnly: Boolean = false)
annotation class CommandFinding(val regex: String, val subPermission: String, val groupOnly: Boolean = false)
annotation class AdminCommand
annotation class AdminOrGroupAdminCommand(val command: String)
typealias Args = List<String>

open class CommandUtils(val rootPermission: String)

fun Bot.registerCommands(utils: CommandUtils) {
    utils.javaClass.methods.forEach { command ->
        command.annotations.forEach { annotation ->
            when (annotation) {
                is Command -> subscribeCommand(annotation.groupOnly) {
                    if (annotation.keepRaw) startsWith(annotation.command, true) { raw ->
                        require(utils.rootPermission.concatDot(annotation.subPermission)) {
                            command.invoke(utils, raw.toArgsList(), this,
                                Continuation<Unit>(currentCoroutineContext()) {})
                        }
                    }
                    else startsWithSimple(annotation.command, true) { raw, _ ->
                        require(utils.rootPermission.concatDot(annotation.subPermission)) {
                            command.invoke(utils, raw.toArgsList(), this,
                                Continuation<Unit>(currentCoroutineContext()) {})
                        }
                    }
                }
                is CommandEqualsTo -> subscribeCommand(annotation.groupOnly) {
                    equalsTo(annotation.command) {
                        require(utils.rootPermission.concatDot(annotation.subPermission)) {
                            command.invoke(utils, this,
                                Continuation<Unit>(currentCoroutineContext()) {})
                        }
                    }
                }
                is CommandMatching -> subscribeCommand(annotation.groupOnly) {
                    matching(regex[annotation.regex]!!) { matching ->
                        require(utils.rootPermission.concatDot(command.name)) {
                            command.invoke(utils, matching, this,
                                Continuation<Unit>(currentCoroutineContext()) {})
                        }
                    }
                }
                is CommandFinding -> subscribeCommand(annotation.groupOnly) {
                    finding(regex[annotation.regex]!!) { finding ->
                        require(utils.rootPermission.concatDot(command.name)) {
                            command.invoke(utils, finding, this,
                                Continuation<Unit>(currentCoroutineContext()) {})
                        }
                    }
                }
                is CommandSingleArg -> subscribeCommand(annotation.groupOnly) {
                    startsWithSimple(annotation.command, removePrefix = true, trim = true) { _, rawArg ->
                        require(utils.rootPermission.concatDot(annotation.subPermission)) {
                            command.invoke(utils, rawArg.substringAfter(" ", "").trim(), this,
                                Continuation<Unit>(currentCoroutineContext()) {})
                        }
                    }
                }
                is AdminCommand -> eventChannel.subscribeMessages {
                    startsWith(".${command.name}", true) { raw ->
                        requireSender(utils.rootPermission.concatDot(command.name)) {
                            command.invoke(utils, raw.toArgsList(), this,
                                Continuation<Unit>(currentCoroutineContext()) {})
                        }
                    }
                }
                is AdminOrGroupAdminCommand -> eventChannel.subscribeGroupMessages {
                    startsWithSimple(annotation.command.ifBlank {".${command.name}"}, true) { raw, _ ->
                        requireMemberOrAdmin(utils.rootPermission.concatDot(command.name)) {
                            command.invoke(utils, raw.toArgsList(), this,
                                Continuation<Unit>(currentCoroutineContext()) {})
                        } ?: run {
                            quoteReply("权限不足")
                        }
                    }
                }
            }
        }
    }
}