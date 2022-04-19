@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package xyz.xszq.otomadbot.text

import com.soywiz.kds.iterators.fastForEach
import kotlinx.serialization.Serializable
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.anyIsInstance
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.message.nextMessage
import xyz.xszq.otomadbot.*
import xyz.xszq.otomadbot.OtomadBotCore.yaml
import xyz.xszq.otomadbot.api.PythonApi
import xyz.xszq.otomadbot.core.SafeYamlConfig
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
class AutoReplyRules: SafeYamlConfig() {
    var nextId = 1
    val rules = mutableMapOf<Int, AutoReplyRule>()
}
object AutoReplyHandler: EventHandler("自动回复", "reply", HandlerType.RESTRICTED_ENABLED) {
    var config = AutoReplyRules()
    fun saveConfig() {
        OtomadBotCore.configFolderPath.resolve("reply.yml").toFile()
            .writeText(yaml.encodeToString(AutoReplyRules.serializer(), config))
    }
    fun reloadConfig() {
        config = yaml.decodeFromString(AutoReplyRules.serializer(), OtomadBotCore.configFolderPath
            .resolve("reply.yml").toFile().readText())
    }
    override fun register() {
        GlobalEventChannel.subscribeAlways<GroupMessageEvent> {
            requireSenderNot(denied) {
                matchText(message.filterIsInstance<PlainText>().joinToString("\n").toSimple().lowercase(),
                group.id) ?.let { quoteReply(it) } ?: run {
                    if (message.anyIsInstance<Image>() &&
                        config.rules.filter { it.value.group == group.id }.isNotEmpty()) {
                        val textList = mutableListOf<String>()
                        message.filterIsInstance<Image>().forEach {
                            textList.add(PythonApi.ocr(it.queryUrl()).lowercase())
                        }
                        matchImage(textList, group.id) ?.let { quoteReply(it) }
                    }
                }
            }
        }
        GlobalEventChannel.subscribeGroupMessages {
            startsWith("自动回复设置") { raw ->
                requireOperator {
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
                                    config.rules[args[1].toInt()] ?.let {
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
        super.register()
    }
    fun matchText(msg: String, group: Long): String? {
        config.rules.values.filter { it.group == -1L || it.group == group }.fastForEach { rule ->
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
        config.rules.values.filter { it.group == -1L || it.group == group }.fastForEach { rule ->
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
    fun addRule(type: ReplyRuleType, rule: String, reply: String, group: Long, creator: Long = -1, name: String = "",
                time: Long = System.currentTimeMillis() / 1000) {
        config.rules[config.nextId] = AutoReplyRule(config.nextId, name, rule, type, group, reply, creator, time)
        saveConfig()
    }
    fun removeRule(id: Int) {
        config.rules.remove(id)
        saveConfig()
    }
}