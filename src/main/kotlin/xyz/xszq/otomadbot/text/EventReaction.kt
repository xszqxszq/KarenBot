@file:Suppress("unused", "MemberVisibilityCanBePrivate", "EXPERIMENTAL_API_USAGE")
package xyz.xszq.otomadbot.text

import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.permission.PermitteeId.Companion.permitteeId
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.LightApp
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.message.data.toPlainText
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import xyz.xszq.otomadbot.*
import xyz.xszq.otomadbot.api.BilibiliApi
import xyz.xszq.otomadbot.api.PythonApi
import xyz.xszq.otomadbot.core.*
import xyz.xszq.otomadbot.image.ImageCommonHandler

object WelcomeHandler: EventHandler("欢迎消息", "welcome") {
    override fun register() {
        super.register()
        GlobalEventChannel.subscribeAlways<MemberJoinEvent> { event ->
            requireNot(denied) {
                group.sendMessage(
                    when (event) {
                        is MemberJoinEvent.Invite -> TextSettings.welcome.invite
                        is MemberJoinEvent.Active -> TextSettings.welcome.active
                        is MemberJoinEvent.Retrieve -> TextSettings.welcome.retrieve
                        else -> ""
                    }
                )
            }
        }
    }
}
object Repeater: EventHandler("重复草字", "kusa", HandlerType.RESTRICTED_ENABLED) {
    val kusa = Counter()
    override fun register() {
        super.register()
        GlobalEventChannel.subscribeAlways<GroupMessageEvent> {
            requireNot(denied) {
                if (message.content == "草") {
                    kusa.increase(subject)
                    if (kusa.get(subject) == 3L) {
                        subject.sendMessage("草")
                    }
                } else {
                    kusa.reset(subject)
                }
            }
        }
    }
}
object NudgeBounce: EventHandler("戳一戳回弹", "nudge.bounce") {
    override fun register() {
        super.register()
        GlobalEventChannel.subscribeAlways<NudgeEvent> {
            requireNot(denied) {
                if (target is Bot && from.id != target.id) {
                    from.nudge().sendTo(subject)
                }
            }
        }
    }
}
object LightAppHandler: EventHandler("QQ小程序解析", "lightapp") {
    val cooldown = Cooldown("lightapp")
    override fun register() {
        super.register()
        GlobalEventChannel.subscribeAlways<GroupMessageEvent> {
            requireNot(denied) {
                ifReady(cooldown) {
                    message.filterIsInstance<LightApp>().forEach {
                        OtomadBotCore.json.decodeFromString<LightAppRoot>(it.content).meta.forEach { (_, it) ->
                            quoteReply(if (it.title == "哔哩哔哩") {
                                try {
                                    val bvid = getBV(it.qqdocurl)
                                    val info = BilibiliApi.queryBv(bvid)
                                    val result = "$bvid\n" +
                                            "${info.data.title}\n" +
                                            "${(info.data.stat.jsonObject["view"]!!.jsonPrimitive.double).toInt()}播放 " +
                                            "${(info.data.stat.jsonObject["danmaku"]!!.jsonPrimitive.double).toInt()}弹幕 " +
                                            "${(info.data.stat.jsonObject["reply"]!!.jsonPrimitive.double).toInt()}评论\n" +
                                            "UP主：${info.data.owner.name}\n" +
                                            "简介：\n" +
                                            info.data.desc.take(50) + (if (info.data.desc.length > 50) "……" else "")
                                    val cover = NetworkUtils.downloadTempFile(info.data.pic)
                                    (cover?.toExternalResource()?.use { img ->
                                        subject.uploadImage(img)
                                    } ?: "".toPlainText()) + result
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    (it.qqdocurl ?: it.url ?: it.contentJumpUrl ?: "").toPlainText()
                                }
                            } else {
                                val result = (it.qqdocurl ?: it.url ?: it.contentJumpUrl ?: "")
                                (if (result.startsWith("mqqapi://")) "" else result).toPlainText()
                            })
                        }
                        update(cooldown)
                    }
                }
            }
        }
    }
    fun getBV(link: String?): String {
        link ?.let {
            return OkHttpClient.Builder()
                .addNetworkInterceptor(Interceptor { chain -> chain.proceed(chain.request()) })
                .build()
                .newCall(
                Request.Builder()
                    .addHeader("User-Agent", availableUA)
                    .url(it)
                    .build()).execute().request.url.pathSegments.last()
        } ?: run {
            return ""
        }
    }
}
object SentimentDetector: EventHandler("情感检测", "sentiment", HandlerType.RESTRICTED_ENABLED) {
    val cooldown = Cooldown("sentiment")
    override fun register() {
        super.register()
        GlobalEventChannel.subscribeAlways<GroupMessageEvent> {
            requireNot(denied) {
                ifReady(cooldown) {
                    if (message.contains(At(bot))) {
                        quoteReply(
                            ImageCommonHandler.replyPic.getRandom(
                                if (PythonApi.sentiment(message.content)!!) "reply"
                                else "afraid"
                            ).uploadAsImage(group)
                        )
                        update(cooldown)
                    }
                }
            }
        }
    }
}
object RequestAccept: EventHandler("自动同意", "accept") {
    override fun register() {
        super.register()
        GlobalEventChannel.subscribeAlways<NewFriendRequestEvent> {
            this.accept()
        }
        GlobalEventChannel.subscribeAlways<BotInvitedJoinGroupRequestEvent> {
            this.accept()
        }
        GlobalEventChannel.subscribeAlways<BotJoinGroupEvent> {
            if (group.members.size < 40L) {
                when {
                    this is BotJoinGroupEvent.Invite
                            && invitor.permitteeId.hasPermission(AdminEventHandler.botAdmin) -> pass
                    else -> group.quit()
                }
            }
        }
    }
    tailrec suspend fun waitFor(maxDelay: Long, checkPeriod: Long, targetId: Long, bot: Bot) : Boolean{
        if (maxDelay < 0) return false
        if (bot.getGroup(targetId) != null) return true
        delay(checkPeriod)
        return waitFor(maxDelay - checkPeriod, checkPeriod, targetId, bot)
    }
}

@Serializable
data class LightAppRoot(val app: String?=null, val desc: String?=null, val view: String?=null, val ver: String?=null, val prompt: String?=null,
val meta: HashMap<String, LightAppDetail>)
@Serializable
data class LightAppDetail(val appid: String?=null, val id: String?=null, val desc: String?=null, val preview: String?=null, val qqdocurl: String?=null,
                          val title: String?=null, val url: String?=null, val contentJumpUrl: String?=null)
@Serializable
class WelcomeTextSettings {
    var invite = "(๑•̀ㅂ•́)و✧"
    var active = "(o゜▽゜)o☆"
    var retrieve = "(つ´∀｀)つ"
}
@Serializable
class RegexSettings {
    var midishow = "(?:.*(?:有无|有|发一下|发给我|发我|给我|发|找一下|找找|找|球球|求求|求|我想要|我要|要)(.*)的(?i)MID.*|" +
            "^(?:(?i)MIDI搜索|搜索(?i)MIDI)(.*))"
    var eropic = "^(?:来(?:份|点|张)(?:色|涩)图|我要一(?:份|点|张)(?:色|涩)图)(.*)"
    var eropicBatch = "^(?:随机(?:涩|色)图)(.*)"
}

object TextSettings : AutoSavePluginConfig("text") {
    val welcome by value(WelcomeTextSettings())
    val regex by value(RegexSettings())
}