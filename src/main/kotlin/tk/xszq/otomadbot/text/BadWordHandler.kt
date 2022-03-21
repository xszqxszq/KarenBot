@file:Suppress("MemberVisibilityCanBePrivate")

package tk.xszq.otomadbot.text

import kotlinx.serialization.Serializable
import net.mamoe.mirai.contact.getMemberOrFail
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.Face
import net.mamoe.mirai.message.data.MessageSource.Key.recall
import tk.xszq.otomadbot.EventHandler
import tk.xszq.otomadbot.core.OtomadBotCore
import tk.xszq.otomadbot.core.OtomadBotCore.yaml
import tk.xszq.otomadbot.core.SafeYamlConfig
import tk.xszq.otomadbot.quoteReply
import tk.xszq.otomadbot.startsWithSimple

@Serializable
class BadWordConfig: SafeYamlConfig() {
    val rules = arrayOf<BadWordConfigItem>()
}

@Serializable
class BadWordConfigItem(
    val type: Int,
    val status: Int,
    val content: String,
    val group: Long,
    val time: Int = 0,
    val reply: String
)
// TODO: Implement this
object BadWordHandler: EventHandler("不良词汇/詈语控制", "badword") {
    var config = BadWordConfig()
    fun saveConfig() {
        OtomadBotCore.configFolderPath.resolve("badword.yml").toFile()
            .writeText(yaml.encodeToString(BadWordConfig.serializer(), config))
    }
    fun reloadConfig() {
        val file = OtomadBotCore.configFolderPath.resolve("badword.yml").toFile()
        if (!file.exists())
            saveConfig()
        config = yaml.decodeFromString(BadWordConfig.serializer(), file.readText())
    }
    const val ALLGROUP = -1L
    // Const for types
    const val KEYWORD = 0
    const val QQFACE = 1
    const val REGEX = 2
    // Const for status
    const val DISABLED = 0
    const val WARNING = 1
    const val RECALL = 2
    const val MUTE = 3
    const val KICK = 4
    override fun register() {
        reloadConfig()
        GlobalEventChannel.subscribeAlways<GroupMessageEvent> {
            handle(this)
        }
        //TODO
        GlobalEventChannel.subscribeGroupMessages {
            startsWithSimple("屏蔽设置") { rawArgs, _ ->

            }
        }
        super.register()
    }
    suspend fun handle(event: GroupMessageEvent) = event.run {
        config.rules.filter { it.status != DISABLED && (it.group == ALLGROUP || it.group == this.group.id) }.forEach { rule ->
            val matched = when (rule.type) {
                KEYWORD -> message.contentToString().contains(rule.content)
                QQFACE -> Face(rule.content.toInt()) in message
                REGEX -> Regex(rule.content).matches(message.contentToString())
                else -> false
            }
            if (matched) {
                when (rule.status) {
                    WARNING -> quoteReply(rule.reply.ifEmpty { "请注意发言哦" })
                    RECALL -> if (group.botAsMember.isOperator()) {
                        message.recall()
                        group.sendMessage(rule.reply.ifEmpty { "请遵守群规哦" })
                    }
                    MUTE -> if (group.botAsMember.isOperator()) {
                        message.recall()
                        sender.mute(rule.time)
                        group.sendMessage(rule.reply.ifEmpty { "请遵守群规哦" })
                    }
                    KICK -> if (group.botAsMember.isOperator()) {
                        message.recall()
                        group.getMemberOrFail(sender.id).kick(rule.reply)
                        group.sendMessage(rule.reply.ifEmpty { "请遵守群规哦" })
                    }
                }
            }
        }
    }
}