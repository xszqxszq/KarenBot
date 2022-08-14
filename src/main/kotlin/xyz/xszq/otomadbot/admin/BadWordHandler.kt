package xyz.xszq.otomadbot.admin

import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.permission.PermissionId
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.permission.PermitteeId.Companion.permitteeId
import net.mamoe.mirai.contact.getMemberOrFail
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.Face
import net.mamoe.mirai.message.data.MessageSource.Key.recall
import xyz.xszq.OtomadBotCore
import xyz.xszq.events
import xyz.xszq.otomadbot.Command
import xyz.xszq.otomadbot.CommandModule
import xyz.xszq.otomadbot.GroupCommand
import xyz.xszq.otomadbot.SafeYamlConfig
import xyz.xszq.otomadbot.image.ChineseOCRLite
import xyz.xszq.otomadbot.image.ImageMatcher
import xyz.xszq.otomadbot.image.ImageReceivedEvent
import xyz.xszq.otomadbot.mirai.quoteReply

@Serializable
class BadWordConfigData(
    val rules: Array<BadWordConfigItem> = arrayOf()
)

@Serializable
class BadWordConfigItem(
    val type: Int,
    val status: Int,
    val content: String,
    val group: Long,
    val time: Int = 0,
    val reply: String
)

object BadWordConfig: SafeYamlConfig<BadWordConfigData>(OtomadBotCore, "badword", BadWordConfigData())

// TODO: Implement this
object BadWordHandler: CommandModule("不良词汇控制", "badword") {
    val special by lazy {
        PermissionService.INSTANCE.register(
            PermissionId("otm", "badword.special"), "特权")
    }
    const val ALLGROUP = -1L
    // Const for types
    const val KEYWORD = 0
    const val QQFACE = 1
    const val REGEX = 2
    const val IMG = 3
    const val OCR = 4
    // Const for status
    const val DISABLED = 0
    const val WARNING = 1
    const val RECALL = 2
    const val MUTE = 3
    const val KICK = 4
    override suspend fun subscribe() {
        events.subscribeAlways<GroupMessageEvent> {
            text.checkAndRun(this)
        }
        events.subscribeAlways<ImageReceivedEvent> {
            image.checkAndRun(this)
        }
        //TODO
//        events.subscribeGroupMessages {
//            startsWithSimple("屏蔽设置") { rawArgs, _ ->
//
//            }
//        }
    }
    val text = GroupCommand("", "text") {
        if (sender.permitteeId.hasPermission(special))
            return@GroupCommand
        BadWordConfig.data.rules.filter {
            it.status != DISABLED && (it.group == ALLGROUP || it.group == this.group.id)
        }.forEach { rule ->
            val matched = when (rule.type) {
                KEYWORD -> message.contentToString().lowercase().contains(rule.content)
                QQFACE -> Face(rule.content.toInt()) in message
                REGEX -> Regex(rule.content).matches(message.contentToString().lowercase())
                else -> false
            }
            if (matched) {
                when (rule.status) {
                    WARNING -> quoteReply(rule.reply.ifEmpty { "请注意发言哦" })
                    RECALL -> if (group.botAsMember.isOperator()) {
                        message.recall()
                        if (rule.reply != "-")
                            group.sendMessage(rule.reply.ifEmpty { "请遵守群规哦" })
                    }
                    MUTE -> if (group.botAsMember.isOperator()) {
                        message.recall()
                        sender.mute(rule.time)
                        if (rule.reply != "-")
                            group.sendMessage(rule.reply.ifEmpty { "请遵守群规哦" })
                    }
                    KICK -> if (group.botAsMember.isOperator()) {
                        message.recall()
                        group.getMemberOrFail(sender.id).kick(rule.reply)
                        if (rule.reply != "-")
                            group.sendMessage(rule.reply.ifEmpty { "请遵守群规哦" })
                    }
                }
            }
        }
    }
    val image = Command<ImageReceivedEvent>("", "image") {
        if (event.sender.permitteeId.hasPermission(special) || event !is GroupMessageEvent)
            return@Command
        BadWordConfig.data.rules.filter {
            it.status != DISABLED && (it.group == ALLGROUP || it.group == event.group.id)
        }.forEach { rule ->
            val matched = when (rule.type) {
                IMG -> img.any { ImageMatcher.matchImage(rule.content, it) }
                OCR -> img.any { Regex(rule.content).matches(ChineseOCRLite.ocr(it.absolutePath).lowercase()) }
                else -> false
            }
            if (matched) {
                when (rule.status) {
                    WARNING -> event.quoteReply(rule.reply.ifEmpty { "请注意发言哦" })
                    RECALL -> if (event.group.botAsMember.isOperator()) {
                        event.message.recall()
                        if (rule.reply != "-")
                            event.group.sendMessage(rule.reply.ifEmpty { "请遵守群规哦" })
                    }
                    MUTE -> if (event.group.botAsMember.isOperator()) {
                        event.message.recall()
                        event.sender.mute(rule.time)
                        if (rule.reply != "-")
                            event.group.sendMessage(rule.reply.ifEmpty { "请遵守群规哦" })
                    }
                    KICK -> if (event.group.botAsMember.isOperator()) {
                        event.message.recall()
                        event.group.getMemberOrFail(event.sender.id).kick(rule.reply)
                        if (rule.reply != "-")
                            event.group.sendMessage(rule.reply.ifEmpty { "请遵守群规哦" })
                    }
                }
            }
        }
    }
}
