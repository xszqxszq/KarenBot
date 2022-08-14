@file:Suppress("MemberVisibilityCanBePrivate", "SpellCheckingInspection")

package xyz.xszq.otomadbot.text

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.permission.PermitteeId.Companion.permitteeId
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import xyz.xszq.OtomadBotCore
import xyz.xszq.events
import xyz.xszq.otomadbot.*
import xyz.xszq.otomadbot.admin.Admin
import xyz.xszq.otomadbot.api.BilibiliApi
import xyz.xszq.otomadbot.api.PythonApi
import xyz.xszq.otomadbot.image.ImageHandler
import xyz.xszq.otomadbot.kotlin.pass
import xyz.xszq.otomadbot.mirai.quoteReply

object EventReaction: CommandModule("事件反应", "react") {
    val cooldown = Cooldown("sentiment")
    override suspend fun subscribe() {
        events.subscribeAlways<MemberJoinEvent> {
            welcome.checkAndRun(this)
        }
        events.subscribeAlways<NudgeEvent> {
            nudge.checkAndRun(this)
        }
        events.subscribeAlways<GroupMessageEvent> {
            kusa.checkAndRun(this)
        }
        events.subscribeAlways<NewFriendRequestEvent> {
            accept.checkAndRun(this)
        }
        events.subscribeAlways<BotInvitedJoinGroupRequestEvent> {
            accept.checkAndRun(this)
        }
        events.subscribeAlways<BotJoinGroupEvent> {
            accept.checkAndRun(this)
        }
        events.subscribeAlways<GroupMessageEvent> {
            if (message.anyIsInstance<LightApp>())
                lightApp.checkAndRun(this)
            else
                avbvParse.checkAndRun(this)
        }
        events.subscribeAlways<GroupMessageEvent> {
            if (message.any { it is At && OtomadBotCore.validator.bots.any { b -> b.id == it.target } }) {
                ifReady(cooldown) {
                    sentiment.checkAndRun(this)
                }
            }
        }
        GlobalScope.launch {
            while (true) {
                val result = newRequestChannel.tryReceive()
                if (result.isSuccess) {
                    when (val event = result.getOrThrow()) {
                        is BotInvitedJoinGroupRequestEvent -> event.accept()
                        is NewFriendRequestEvent -> event.accept()
                    }
                }
                delay(CooldownConfig.data.values["accept"]!!)
            }
        }
    }
    val welcome = Command<MemberJoinEvent>("欢迎消息", "welcome") {
        when (this) {
            is MemberJoinEvent.Active -> group.sendMessage(TextSettings.data.values["welcome_active"]!!)
            is MemberJoinEvent.Invite -> group.sendMessage(TextSettings.data.values["welcome_invite"]!!)
            is MemberJoinEvent.Retrieve -> group.sendMessage(TextSettings.data.values["welcome_retrieve"]!!)
        }
    }
    val nudge = Command<NudgeEvent>("戳一戳回弹", "nudge") {
        if (target is Bot && from.id != target.id)
            from.nudge().sendTo(subject)
    }
    val kusa = GroupCommand("重复草字", "kusa") {
        if (message.content == "草") {
            kusaCounter.increase(group)
            if (kusaCounter.get(group) == 3L)
                subject.sendMessage("草")
        } else {
            kusaCounter.reset(group)
        }
    }
    val accept = Command<Event>("自动同意", "accept") {
        when (this) {
            is NewFriendRequestEvent -> newRequestChannel.send(this)
            is BotInvitedJoinGroupRequestEvent -> newRequestChannel.send(this)
            is BotJoinGroupEvent -> {
                if (group.members.size < 60L) {
                    when {
                        group.members.any { it.permitteeId.hasPermission(Admin.botAdmin) } -> pass
                        else -> group.quit()
                    }
                }
            }
        }
    }
    val lightApp = Command<GroupMessageEvent>("小程序解析", "lightapp") {
        val raw = message.firstIsInstance<LightApp>()
        OtomadBotCore.json.decodeFromString<LightAppRoot>(raw.content).meta.values.forEach { app ->
            if (app.title == "哔哩哔哩") {
                runCatching {
                    quoteReply(BilibiliApi.getVideoDetails(app.qqdocurl!!, subject))
                }.onFailure {
                    it.printStackTrace()
                }
            } else {
                val result = app.qqdocurl ?: app.url ?: app.contentJumpUrl ?: ""
                if (result.isNotBlank() && !result.startsWith("mqqapi://"))
                    quoteReply(result)
            }
        }
    }
    val avbvParse = GroupCommand("av/BV号解析", "av_bv_parse") {
        if (message.content.take(2) in listOf("av", "BV") || "b23.tv" in message.content ||
            "bilibili.com/video/" in message.content) {
            kotlin.runCatching {
                quoteReply(BilibiliApi.getVideoDetails(message.content, subject))
            }
        }
    }
    val sentiment = GroupCommand("情感检测", "sentiment") {
        quoteReply(
            ImageHandler.replyPic.getRandom(
                if (PythonApi.sentiment(message.filterIsInstance<PlainText>()
                        .joinToString("。").trim())!!) "reply"
                else "afraid"
            ).uploadAsImage(group)
        )
        update(cooldown)
    }
    val kusaCounter = Counter()
    val newRequestChannel = Channel<BotEvent>()
}

object TextSettings: SafeYamlConfig<MapStringValues>(
    OtomadBotCore, "text",
    MapStringValues(buildMap {
        put("welcome_active", "(o゜▽゜)o☆")
        put("welcome_invite", "(๑•̀ㅂ•́)و✧")
        put("welcome_retrieve", "(つ´∀｀)つ")
        put("regex_midishow", "(?:.*(?:有无|有|发一下|发给我|发我|给我|发|找一下|找找|找|球球|求求|求|我想要|我要|要)" +
                "(.*)的(?i)MID.*|^(?:(?i)MIDI搜索|搜索(?i)MIDI)(.*))")
    }.toMutableMap())
)


@Serializable
data class LightAppRoot(val app: String?=null, val desc: String?=null, val view: String?=null, val ver: String?=null, val prompt: String?=null,
                        val meta: HashMap<String, LightAppDetail>)
@Serializable
data class LightAppDetail(val appid: String?=null, val id: String?=null, val desc: String?=null, val preview: String?=null, val qqdocurl: String?=null,
                          val title: String?=null, val url: String?=null, val contentJumpUrl: String?=null)