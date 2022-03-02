@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package tk.xszq.otomadbot.text

import com.charleskorn.kaml.Yaml
import com.soywiz.kds.iterators.fastForEach
import kotlinx.serialization.Serializable
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.anyIsInstance
import tk.xszq.otomadbot.*
import tk.xszq.otomadbot.api.PythonApi
import tk.xszq.otomadbot.core.OtomadBotCore

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
    val type: Byte = 0,
    val group: Long,
    val reply: String,
    val creator: Long,
    val createTime: Long
) {
    companion object {
        val idNameMap = listOf(
            Pair(ReplyRuleType.PIC_INCLUDE.type, "图片包含"),
            Pair(ReplyRuleType.PIC_ALL.type, "图片全含"),
            Pair(ReplyRuleType.PIC_ANY.type, "图片任含"),
            Pair(ReplyRuleType.INCLUDE.type, "包含"),
            Pair(ReplyRuleType.EQUAL.type, "全等"),
            Pair(ReplyRuleType.REGEX.type, "正则"),
            Pair(ReplyRuleType.ANY.type, "任含"),
            Pair(ReplyRuleType.ALL.type, "全含"),
        )
        fun getNameFromType(type: Byte) = idNameMap.find { it.first == type }
        fun getTypeFromName(name: String) = idNameMap.find {it.second == name}
    }
}
@Serializable
class AutoReplyRules {
    var nextId = 1
    val rules = mutableMapOf<Int, AutoReplyRule>()
}
object AutoReplyHandler: EventHandler("自动回复", "reply", HandlerType.RESTRICTED_ENABLED) {
    var config = AutoReplyRules()
    fun saveConfig() {
        OtomadBotCore.configFolderPath.resolve("reply.yml").toFile()
            .writeText(Yaml.default.encodeToString(AutoReplyRules.serializer(), config))
    }
    fun reloadConfig() {
        config = Yaml.default.decodeFromString(AutoReplyRules.serializer(), OtomadBotCore.configFolderPath
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
                            textList.add(PythonApi.ocr(it.queryUrl()))
                        }
                        matchImage(textList, group.id) ?.let { quoteReply(it) }
                    }
                }
            }
        }
        super.register()
    }
    fun matchText(msg: String, group: Long): String? {
        config.rules.values.filter { it.group == -1L || it.group == group }.fastForEach { rule ->
            val matched = when (rule.type) {
                ReplyRuleType.INCLUDE.type -> rule.rule in msg
                ReplyRuleType.EQUAL.type -> rule.rule == msg
                ReplyRuleType.REGEX.type -> Regex(rule.rule).find(msg) != null
                ReplyRuleType.ANY.type -> {
                    var matched = false
                    rule.rule.split(",").fastForEach { if (it in msg) { matched = true } }
                    matched
                }
                ReplyRuleType.ALL.type -> {
                    var matched = true
                    val keywords = rule.rule.split(",")
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
                ReplyRuleType.PIC_INCLUDE.type -> {
                    var matched = false
                    text.fastForEach {
                        if (rule.rule in it)
                            matched = true
                    }
                    matched
                }
                ReplyRuleType.PIC_ALL.type -> {
                    var matched = false
                    val keywords = rule.rule.split(",")
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
                ReplyRuleType.PIC_ANY.type -> {
                    var matched = false
                    val keywords = rule.rule.split(",")
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
}