package tk.xszq.otomadbot.admin

import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.permission.PermitteeId.Companion.permitteeId
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.getMember
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.message.nextMessage
import tk.xszq.otomadbot.AdminEventHandler
import tk.xszq.otomadbot.quoteReply
import tk.xszq.otomadbot.startsWithSimple

fun Member.isAdminCommandPermitted() = isOperator() || permitteeId.hasPermission(AdminEventHandler.botAdmin)

object GroupAdminCommandHandler: AdminEventHandler() {
    override fun register() {
        GlobalEventChannel.subscribeMessages {
            startsWithSimple("/禁言") { targetMember, _ ->
                val groups = bot.groups.filter { group ->
                    (group.getMember(sender.id) ?.isAdminCommandPermitted() ?: false)
                            && group.contains(targetMember.toLong())
                }
                if (groups.isNotEmpty()) {
                    val targetGroup = if (groups.size > 1) {
                        var selectHint = "请回复欲操作的群的序号："
                        groups.forEachIndexed  { index, it -> selectHint += "\n  $index. ${it.name} (${it.id})" }
                        quoteReply(selectHint)
                        groups.getOrNull(nextMessage().content.toInt())
                    } else groups.first()
                    targetGroup ?.let {
                        quoteReply("请回复欲禁言的时长（支持的单位：秒/分/时/天，若无单位则默认为分钟，支持禁言1秒~30天）")
                        val duration = nextMessage().content
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
                        it.sendMessage("由于 ${member.nameCardOrNick} (${member.id}) 的发言经检测违反了群规，特此处以禁言")
                    }
                }
            }
        }
        super.register()
    }
}