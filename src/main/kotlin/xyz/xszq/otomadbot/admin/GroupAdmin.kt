package xyz.xszq.otomadbot.admin

import net.mamoe.mirai.console.permission.PermissionService.Companion.cancel
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.permission.PermissionService.Companion.permit
import net.mamoe.mirai.console.permission.PermitteeId.Companion.permitteeId
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.getMember
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.message.nextMessage
import xyz.xszq.OtomadBotCore
import xyz.xszq.events
import xyz.xszq.otomadbot.CommandEvent
import xyz.xszq.otomadbot.CommandModule
import xyz.xszq.otomadbot.CommonCommandWithArgs
import xyz.xszq.otomadbot.admin.Admin.botAdmin
import xyz.xszq.otomadbot.mirai.quoteReply
import xyz.xszq.otomadbot.mirai.startsWithSimple


fun Member.isAdminCommandPermitted() = isOperator() || permitteeId.hasPermission(botAdmin)

object GroupAdmin: CommandModule("", "group_admin") {
    override suspend fun subscribe() {
        events.subscribeMessages {
            startsWithSimple("/禁言") { targetMember, _ ->
                mute.checkAndRun(CommandEvent(listOf(targetMember), this))
            }
            startsWithSimple("/踢") { targetMember, _ ->
                kick.checkAndRun(CommandEvent(listOf(targetMember), this))
            }
        }
        events.subscribeGroupMessages {
            startsWithSimple("功能列表") { moduleName, _ ->
                if (!sender.isAdminCommandPermitted()) // TODO: Change to standard command style
                    return@startsWithSimple
                if (moduleName.isBlank()) {
                    var listText = "bot 包含以下模块：\n"
                    listText += OtomadBotCore.modules.filter { it.name.isNotBlank() }
                        .joinToString("，") { it.name }
                    listText += "。\n如需查询模块下包含哪些功能，请输入“功能列表 模块名”。"
                    quoteReply(listText)
                } else {
                    OtomadBotCore.modules.filter { it.name.isNotBlank() }.find { it.name.lowercase() == moduleName } ?.let { m ->
                        var listText = "${m.name}模块包含以下功能：\n"
                        listText += m.getCommands().joinToString("，") { it.name }
                        listText += "\n如需启用/禁用功能，请输入“启用功能 功能名或模块名”或“禁用功能 功能名或模块名”"
                        quoteReply(listText)
                    } ?: run {
                        quoteReply("未找到该模块。请输入“功能列表”查看模块名。")
                    }
                }
            }
            startsWithSimple("启用功能") { funcName, _ ->
                if (!sender.isAdminCommandPermitted())
                    return@startsWithSimple
                OtomadBotCore.modules.filter { it.name.isNotBlank() }.find { it.name.lowercase() == funcName } ?.let { m ->
                    if (group.permitteeId.hasPermission(m.denyPerm)) kotlin.runCatching {
                        group.permitteeId.cancel(m.denyPerm, false)
                    }
                        .onFailure { it.printStackTrace() }
                    quoteReply("启用成功")
                } ?: run {
                    OtomadBotCore.modules.filter { it.name.isNotBlank() }.map { it.getCommands() }.flatten()
                        .find { it.name.lowercase() == funcName } ?.let { c ->
                            if (!c.defaultEnabled && !group.permitteeId.hasPermission(c.perm))
                                kotlin.runCatching { group.permitteeId.permit(c.perm) }
                                    .onFailure { it.printStackTrace() }
                            if (c.defaultEnabled && group.permitteeId.hasPermission(c.perm))
                                kotlin.runCatching { group.permitteeId.cancel(c.perm, false) }
                                    .onFailure { it.printStackTrace() }
                            quoteReply("启用成功")
                        } ?: run {
                            quoteReply("未找到该功能。请输入“功能列表”查看模块名或功能名")
                        }
                }
            }
            startsWithSimple("禁用功能") { funcName, _ ->
                if (!sender.isAdminCommandPermitted())
                    return@startsWithSimple
                OtomadBotCore.modules.filter { it.name.isNotBlank() }.find { it.name.lowercase() == funcName } ?.let { m ->
                    if (!group.permitteeId.hasPermission(m.denyPerm)) kotlin.runCatching {
                        group.permitteeId.permit(m.denyPerm)
                    }
                        .onFailure { it.printStackTrace() }
                    quoteReply("禁用成功")
                } ?: run {
                    OtomadBotCore.modules.filter { it.name.isNotBlank() }.map { it.getCommands() }.flatten()
                        .find { it.name.lowercase() == funcName }?.let { c ->
                            if (c.defaultEnabled && !group.permitteeId.hasPermission(c.perm))
                                kotlin.runCatching { group.permitteeId.permit(c.perm) }
                                    .onFailure { it.printStackTrace() }
                            if (!c.defaultEnabled && group.permitteeId.hasPermission(c.perm))
                                kotlin.runCatching { group.permitteeId.cancel(c.perm, false) }
                                    .onFailure { it.printStackTrace() }
                            quoteReply("禁用成功")
                        } ?: run {
                            quoteReply("未找到该功能。请输入“功能列表”查看模块名或功能名")
                        }
                }
            }
        }
    }
    val mute = CommonCommandWithArgs("禁言", "mute") {
        val targetMember = args.first()
        val groups = event.bot.groups.filter { group ->
            (group.getMember(event.sender.id) ?.isAdminCommandPermitted() ?: false)
                    && group.contains(targetMember.toLong()) && group.botAsMember.isOperator()
        }
        if (groups.isNotEmpty()) {
            val targetGroup = if (groups.size > 1) {
                var selectHint = "请回复欲操作的群的序号："
                groups.forEachIndexed  { index, it -> selectHint += "\n  $index. ${it.name} (${it.id})" }
                event.quoteReply(selectHint)
                groups.getOrNull(event.nextMessage().content.toInt())
            } else groups.first()
            targetGroup ?.let {
                event.quoteReply("请回复欲禁言的时长（支持的单位：秒/分/时/天，若无单位则默认为分钟，支持禁言1秒~30天）")
                val duration = event.nextMessage().content
                val amount = duration.substring(0,
                    if (duration.last().isDigit()) duration.length else duration.length-1).toInt() *
                        when (duration.last()) {
                            '秒' -> 1
                            '分' -> 60
                            '时' -> 60 * 60
                            '天' -> 24 * 60 * 60
                            else -> 60
                        }
                val member = it.getOrFail(targetMember.toLong())
                member.mute(amount)
                it.sendMessage("请遵守群规哦")
            }
        }
    }
    val kick = CommonCommandWithArgs("踢", "kick") {
        val targetMember = args.first()
        val groups = event.bot.groups.filter { group ->
            (group.getMember(event.sender.id) ?.isAdminCommandPermitted() ?: false)
                    && group.contains(targetMember.toLong()) && group.botAsMember.isOperator()
        }
        if (groups.isNotEmpty()) {
            (if (groups.size > 1) {
                var selectHint = "请回复欲操作的群的序号："
                groups.forEachIndexed  { index, it -> selectHint += "\n  $index. ${it.name} (${it.id})" }
                event.quoteReply(selectHint)
                groups.getOrNull(event.nextMessage().content.toInt())
            } else groups.first()) ?.let { targetGroup ->
                val member = targetGroup.getOrFail(targetMember.toLong())
                event.quoteReply("即将在 ${targetGroup.name} 踢出 ${member.nameCardOrNick} ($targetMember)，确认？(y/n)")
                if (event.nextMessage().content.lowercase().first() == 'y') {
                    member.kick("")
                }
            }
        }
    }
}