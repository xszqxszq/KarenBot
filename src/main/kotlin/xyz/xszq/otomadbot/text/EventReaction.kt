@file:Suppress("MemberVisibilityCanBePrivate", "SpellCheckingInspection")

package xyz.xszq.otomadbot.text

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import top.mrxiaom.overflow.contact.RemoteBot
import xyz.xszq.OtomadBotCore
import xyz.xszq.events
import xyz.xszq.otomadbot.*
import xyz.xszq.otomadbot.api.BilibiliApi
import xyz.xszq.otomadbot.api.CookiesResponse
import xyz.xszq.otomadbot.api.PythonApi
import xyz.xszq.otomadbot.api.RemoteBotResponse
import xyz.xszq.otomadbot.image.ImageHandler
import xyz.xszq.otomadbot.mirai.quoteReply
import xyz.xszq.otomadbot.mirai.reply

object EventReaction: CommandModule("自动解析响应", "react") {
    val cooldown = Cooldown("sentiment")
    val json = Json {
        ignoreUnknownKeys = true
    }
    override suspend fun subscribe() {
        events.subscribeAlways<BotOnlineEvent> {
            GlobalScope.launch {
                var response: RemoteBotResponse<CookiesResponse>
                while (true) {
                    response = json.decodeFromString<RemoteBotResponse<CookiesResponse>>(
                        (bot as RemoteBot).executeAction("get_cookies", "{}")
                    )
                    if (response.status == "ok")
                        break
                    delay(5000L)
                }
                OtomadBotCore.cookies = response.data!!.cookies
                OtomadBotCore.bkn = response.data!!.bkn
            }
        }
        events.subscribeAlways<MemberJoinEvent> {
            welcome.checkAndRun(this)
        }
        events.subscribeAlways<NudgeEvent> {
            nudge.checkAndRun(this)
        }
        events.subscribeAlways<NewFriendRequestEvent> {
            delay(5000)
            accept()
        }
        events.subscribeAlways<BotInvitedJoinGroupRequestEvent> {
            delay(5000)
            accept()
        }
        events.subscribeAlways<GroupMessageEvent> {
            kotlin.runCatching {
                kusa.checkAndRun(this)
            }
            kotlin.runCatching {
                if (message.anyIsInstance<LightApp>())
                    lightApp.checkAndRun(this)
                else
                    avbvParse.checkAndRun(this)
            }
            kotlin.runCatching {
                if (message.any { it is At && it.target == bot.id }
                    && !message.anyIsInstance<QuoteReply>()
                    && message.filterIsInstance<PlainText>()
                        .joinToString("").trim().isNotBlank()) {
                    if (sender.id == 2854196310)
                        return@runCatching
                    ifReadyStrict(cooldown) {
                        reply("目前使用可怜Bot时无再需at机器人" + if ("/" in message.content) "，并且也无需“/”斜杠" else "")
                        update(cooldown)
                    }
                }
            }
            kotlin.runCatching {
                if ((message.any { it is At && it.target == bot.id } && message.filterIsInstance<PlainText>()
                    .joinToString("").trim().isBlank())
                    || message.content.trim() == "来点黄毛") {
                    ifReadyStrict(cooldown) {
                        sentiment.checkAndRun(this)
                    }
                }
            }
        }
//        events.subscribeAlways<Event> {
//            when (this) {
//                is MessageEvent -> {
//                    OtomadBotCore.logger.info("${
//                        if (this is GroupMessageEvent)
//                            "[${group.name}(${group.id})] ${sender.remarkOrNameCardOrNick}(${sender.id})"
//                        else
//                            "${sender.remarkOrNick}(${sender.id})"
//                        } -> ${message.content}"
//                    )
//                }
//                is GroupEvent -> {
//                    OtomadBotCore.logger.info("Event: ${this.javaClass.simpleName}(group=$group)")
//                }
//                is UserEvent -> {
//                    OtomadBotCore.logger.info("Event: ${this.javaClass.simpleName}(user=$user)")
//                }
//            }
//        }
    }
    val welcome = CommandWithType<MemberJoinEvent>("欢迎消息", "welcome") {
        delay(1000L)
        when (this) {
            is MemberJoinEvent.Active -> group.sendMessage(TextSettings.data.values["welcome_active"]!!)
            is MemberJoinEvent.Invite -> group.sendMessage(TextSettings.data.values["welcome_invite"]!!)
            is MemberJoinEvent.Retrieve -> group.sendMessage(TextSettings.data.values["welcome_retrieve"]!!)
        }
    }
    val nudge = CommandWithType<NudgeEvent>("戳一戳回弹", "nudge") {
        if (target is Bot && from.id != target.id) {
            delay(1000L)
            from.nudge().sendTo(subject)
        }
    }
    val kusa = GroupCommand("重复草字", "kusa") {
        if (message.content == "草") {
            delay(500L)
            kusaCounter.increase(group)
            if (kusaCounter.get(group) == 3L)
                subject.sendMessage("草")
        } else {
            kusaCounter.reset(group)
        }
    }
//    val accept = CommandWithType<Event>("", "accept") {
//        when (this) {
////            is NewFriendRequestEvent -> newRequestChannel.send(this)
////            is BotInvitedJoinGroupRequestEvent -> newRequestChannel.send(this)
//            is BotJoinGroupEvent -> {
//            }
//        }
//    }
    val lightApp = CommandWithType<GroupMessageEvent>("小程序解析", "lightapp") {
        val raw = message.firstIsInstance<LightApp>()
        OtomadBotCore.json.decodeFromString<LightAppRoot>(raw.content).meta.values.forEach { app ->
            if (app.title == "哔哩哔哩") {
                runCatching {
                    group.sendMessage(BilibiliApi.getVideoDetails(app.qqdocurl!!, subject))
                }.onFailure {
                    it.printStackTrace()
                }
            }
        }
    }
    val avbvParse = GroupCommand("av/BV号解析", "av_bv_parse") {
        if (message.content.take(2) in listOf("av", "BV") ||
            "bilibili.com/video/" in message.content) {
            kotlin.runCatching {
                group.sendMessage(BilibiliApi.getVideoDetails(message.content, subject))
            }
        }
    }
    val sentiment = GroupCommand("", "sentiment") {
        quoteReply(
            ImageHandler.replyPic.getRandom("reply").uploadAsImage(group)
//            ImageHandler.replyPic.getRandom(
//                if (PythonApi.sentiment(message.filterIsInstance<PlainText>()
//                        .joinToString("。").trim())!!) "reply"
//                else "afraid"
//            ).uploadAsImage(group)
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