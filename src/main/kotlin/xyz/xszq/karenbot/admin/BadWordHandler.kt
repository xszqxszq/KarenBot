package xyz.xszq.karenbot.admin

import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.permission.PermissionId
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.permission.PermitteeId.Companion.permitteeId
import net.mamoe.mirai.contact.getMemberOrFail
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.MessageSource.Key.recall
import net.mamoe.mirai.message.nextMessage
import xyz.xszq.KarenBot
import xyz.xszq.events
import xyz.xszq.karenbot.CommandModule
import xyz.xszq.karenbot.GroupCommand
import xyz.xszq.karenbot.ImageCommand
import xyz.xszq.karenbot.SafeYamlConfig
import xyz.xszq.karenbot.api.PythonApi
import xyz.xszq.karenbot.image.ImageMatcher
import xyz.xszq.karenbot.mirai.quoteReply
import xyz.xszq.karenbot.mirai.startsWithSimple

@Serializable
class BadWordConfigData(
    val rules: MutableList<BadWordConfigItem> = mutableListOf()
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

object BadWordConfig: SafeYamlConfig<BadWordConfigData>(KarenBot, "badword", BadWordConfigData())

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
        //TODO
        events.subscribeGroupMessages {
            startsWithSimple("添加过滤规则") { type, _ ->
                if (!sender.isAdminCommandPermitted()) // TODO: Change to standard command style
                    return@startsWithSimple

                val typeId = name2type(type) ?: run {
                    quoteReply("使用方法：\n\t添加过滤规则 规则类型\n目前支持的类型：关键词，正则，图片关键词")
                    return@startsWithSimple
                }

                quoteReply("请回复要过滤的消息的规则：")
                val rule = nextMessage().content

                quoteReply("请确认以下过滤规则（回复y确认，n取消）：\n\t类别：${type2name(typeId)}\n\t规则：${rule}")

                if (nextMessage().content.trim().lowercase() == "y") {
                    BadWordConfig.data.rules.add(BadWordConfigItem(typeId, RECALL, rule, group.id, 0, ""))
                    BadWordConfig.save()
                    quoteReply("添加成功。")
                }
            }
            startsWithSimple("删除过滤规则") { _, _ ->
                if (!sender.isAdminCommandPermitted())
                    return@startsWithSimple
                val rules = BadWordConfig.data.rules.filter { it.group == group.id }
                if (rules.isEmpty()) {
                    quoteReply("当前无过滤规则。")
                    return@startsWithSimple
                }

                quoteReply(buildString {
                    append("请选择要删除的规则：")
                    rules.forEachIndexed { index, rule ->
                        append("\n\t${index}. ${type2name(rule.type)}: ${rule.content}")
                    }
                })

                val id = nextMessage().content.toInt()
                if (id in rules.indices) {
                    quoteReply("即将删除以下规则（回复y确认，n取消）：\n" +
                            "\t${id}. ${type2name(rules[id].type)}: ${rules[id].content}")
                    if (nextMessage().content.trim().lowercase() == "y") {
                        BadWordConfig.data.rules.removeIf {
                            it.type == rules[id].type && it.group == rules[id].group && it.content == rules[id]
                                .content && it.time == rules[id].time && it.reply == rules[id].reply
                        }
                        BadWordConfig.save()
                        quoteReply("删除成功。")
                    }
                }
            }
        }
    }
    fun type2name(rule: Int) = when (rule) {
        KEYWORD -> "关键词"
        QQFACE -> "QQ表情"
        REGEX -> "正则"
        IMG -> "图片匹配"
        OCR -> "图片关键词"
        else -> null
    }
    fun name2type(name: String) = when(name) {
        "关键词" -> KEYWORD
        "QQ表情" -> QQFACE
        "正则" -> REGEX
        "图片匹配" -> IMG
        "图片关键词" -> OCR
        else -> null
    }
    val text = GroupCommand("", "text") {
        if (sender.permitteeId.hasPermission(special))
            return@GroupCommand
        val adminBot = if (group.botAsMember.isOperator()) bot else null
        val target =
            adminBot?.buildMessageSource(MessageSourceKind.GROUP) {
                sender(message.source.fromId)
                target(message.source.targetId)
                messagesFrom(source)
                metadata(message.source)
            }?.toMessageChain()
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
                    RECALL -> if (adminBot != null) {
                        target ?.recall()
                        if (rule.reply != "-")
                            group.sendMessage(rule.reply.ifEmpty { "请遵守群规哦" })
                    }
                    MUTE -> if (group.botAsMember.isOperator()) {
                        target ?.recall()
                        sender.mute(rule.time)
                        if (rule.reply != "-")
                            group.sendMessage(rule.reply.ifEmpty { "请遵守群规哦" })
                    }
                    KICK -> if (group.botAsMember.isOperator()) {
                        target ?.recall()
                        group.getMemberOrFail(sender.id).kick(rule.reply)
                        if (rule.reply != "-")
                            group.sendMessage(rule.reply.ifEmpty { "请遵守群规哦" })
                    }
                }
            }
        }
    }
    val image = ImageCommand("", "image") { img ->
        if (sender.permitteeId.hasPermission(special) || img == null)
            return@ImageCommand
        BadWordConfig.data.rules.filter {
            it.status != DISABLED && (it.group == ALLGROUP || it.group == group.id)
        }.forEach { rule ->
            val matched = when (rule.type) {
                IMG -> img.any { ImageMatcher.matchImage(rule.content, it) }
                OCR -> img.any { Regex(rule.content).matches(PythonApi.getOCR(it)!!.lowercase()) }
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
}
