package xyz.xszq.otomadbot.text

import com.beust.jcommander.Strings.startsWith
import com.soywiz.kds.iterators.fastForEach
import kotlinx.serialization.Serializable
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.message.nextMessage
import xyz.xszq.*
import xyz.xszq.otomadbot.image.ImageReceivedEvent
import xyz.xszq.events
import xyz.xszq.otomadbot.*
import xyz.xszq.otomadbot.admin.isAdminCommandPermitted
import xyz.xszq.otomadbot.image.ChineseOCRLite
import xyz.xszq.otomadbot.kotlin.pass
import xyz.xszq.otomadbot.kotlin.toArgsList
import xyz.xszq.otomadbot.kotlin.toSimple
import xyz.xszq.otomadbot.mirai.quoteReply
import xyz.xszq.otomadbot.text.AutoReplyRule.Companion.getNameFromType
import xyz.xszq.otomadbot.text.AutoReplyRule.Companion.getTypeFromName


enum class ReplyRuleType(val type: Byte) {
    INCLUDE(0),
    EQUAL(1),
    REGEX(2),
    ANY(3),
    ALL(4),
    PIC_INCLUDE(-1),
    PIC_ALL(-2),
    PIC_ANY(-3)
}
@Serializable
class AutoReplyRule(
    val id: Int,
    val name: String = "",
    val rule: String,
    val type: ReplyRuleType,
    val group: Long,
    val reply: String,
    val creator: Long,
    val createTime: Long
) {
    companion object {
        val idNameMap = listOf(
            Pair(ReplyRuleType.PIC_INCLUDE, "图片包含"),
            Pair(ReplyRuleType.PIC_ALL, "图片全含"),
            Pair(ReplyRuleType.PIC_ANY, "图片任含"),
            Pair(ReplyRuleType.INCLUDE, "包含"),
            Pair(ReplyRuleType.EQUAL, "全等"),
            Pair(ReplyRuleType.REGEX, "正则"),
            Pair(ReplyRuleType.ANY, "任含"),
            Pair(ReplyRuleType.ALL, "全含"),
        )
        fun getNameFromType(type: ReplyRuleType) = idNameMap.find { it.first == type } ?.second
        fun getTypeFromName(name: String) = idNameMap.find { it.second == name } ?.first
    }
}
@Serializable
class AutoReplyData {
    var nextId = 1
    val rules = mutableMapOf<Int, AutoReplyRule>()
}
object AutoReplyConfig: SafeYamlConfig<AutoReplyData>(OtomadBotCore, "reply", AutoReplyData())
object AutoReplyHandler: CommandModule("自动回复", "reply") {
    fun matchText(msg: String, group: Long): String? {
        AutoReplyConfig.data.rules.values.filter { it.group == -1L || it.group == group }.fastForEach { rule ->
            val matched = when (rule.type) {
                ReplyRuleType.INCLUDE -> rule.rule.lowercase() in msg
                ReplyRuleType.EQUAL -> rule.rule.lowercase() == msg
                ReplyRuleType.REGEX -> Regex(rule.rule.lowercase()).find(msg) != null
                ReplyRuleType.ANY -> {
                    var matched = false
                    rule.rule.lowercase().split(",").fastForEach { if (it in msg) { matched = true } }
                    matched
                }
                ReplyRuleType.ALL -> {
                    var matched = true
                    val keywords = rule.rule.lowercase().split(",")
                    keywords.fastForEach { if (it !in msg) { matched = false } }
                    keywords.isNotEmpty() && matched

                }
                else -> false
            }
            if (matched)
                return rule.reply
        }
        return null
    }
    fun matchImage(text: List<String>, group: Long): String? {
        AutoReplyConfig.data.rules.values.filter { it.group == -1L || it.group == group }.fastForEach { rule ->
            val matched = when (rule.type) {
                ReplyRuleType.PIC_INCLUDE -> {
                    var matched = false
                    text.fastForEach {
                        if (rule.rule.lowercase() in it)
                            matched = true
                    }
                    matched
                }
                ReplyRuleType.PIC_ALL -> {
                    var matched = false
                    val keywords = rule.rule.lowercase().split(",")
                    text.fastForEach inner@ { msg ->
                        var now = true
                        keywords.fastForEach { if (it !in msg) { now = false } }
                        if (now) {
                            matched = true
                            return@inner
                        }
                    }
                    matched
                }
                ReplyRuleType.PIC_ANY -> {
                    var matched = false
                    val keywords = rule.rule.lowercase().split(",")
                    text.fastForEach outer@ { msg ->
                        keywords.fastForEach inner@ { if (it in msg) { matched = true; return@outer } }
                    }
                    matched
                }
                else -> false
            }
            if (matched)
                return rule.reply
        }
        return null
    }
    suspend fun addRule(type: ReplyRuleType, rule: String, reply: String, group: Long, creator: Long = -1, name: String = "",
                        time: Long = System.currentTimeMillis() / 1000) {
        AutoReplyConfig.data.rules[AutoReplyConfig.data.nextId] = AutoReplyRule(AutoReplyConfig.data.nextId, name, rule, type, group, reply, creator, time)
        AutoReplyConfig.data.nextId += 1
        AutoReplyConfig.save()
    }
    suspend fun removeRule(id: Int) {
        AutoReplyConfig.data.rules.remove(id)
        AutoReplyConfig.save()
    }


    override suspend fun subscribe() {
        events.subscribeAlways<GroupMessageEvent> {
            reply.checkAndRun(this)
        }
        events.subscribeAlways<ImageReceivedEvent> {
            replyPic.checkAndRun(this)
        }
        events.subscribeGroupMessages {
            startsWith("自动回复设置") { raw ->
                if (!sender.isAdminCommandPermitted())
                    return@startsWith
                val args = raw.toArgsList()
                if (args.isEmpty()) {
                    quoteReply("使用格式：自动回复设置 <子命令> (附加参数)\n支持的子命令：新建、删除")
                }
                when (args.first()) {
                    "新建" -> {
                        quoteReply("请输入类别（全等/正则/(图片)包含/(图片)任含/(图片)全含）：")
                        val type = getTypeFromName(nextMessage().content)!!
                        quoteReply("请输入规则：")
                        val rule = nextMessage().content
                        quoteReply("请输入回复内容：")
                        val reply = nextMessage().content
                        quoteReply("匹配类别：${getNameFromType(type)}\n规则：$rule\n回复：$reply\n确认？(y/n)")
                        if (nextMessage().content.lowercase() == "y") {
                            addRule(type, rule, reply, group.id)
                            quoteReply("添加成功")
                        }
                    }
                    "删除" -> {
                        when (args.size) {
                            2 -> {
                                AutoReplyConfig.data.rules[args[1].toInt()] ?.let {
                                    if (it.group == group.id) {
                                        removeRule(args[1].toInt())
                                    }
                                }
                            }
                            else -> quoteReply("使用格式：自动回复设置 删除 规则编号")
                        }
                    }
                }
                pass
            }
        }
    }
    val reply = GroupCommand("自动回复", "reply", checkSender = true) {
        matchText(message.filterIsInstance<PlainText>().joinToString("\n").toSimple().lowercase(),
            group.id) ?.let { quoteReply(it) }
    }
    val replyPic = Command<ImageReceivedEvent>("自动回复图片", "reply_pic", checkSender = true) {
        if (event is GroupMessageEvent &&
            AutoReplyConfig.data.rules.filter {
                it.value.group == event.group.id
            }.isNotEmpty()) {
            val textList = img.map { ChineseOCRLite.ocr(it.absolutePath).lowercase() }
            matchImage(textList, event.group.id) ?.let { event.quoteReply(it) }
        }
    }
}