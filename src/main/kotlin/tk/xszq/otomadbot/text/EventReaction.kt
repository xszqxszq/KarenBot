@file:Suppress("unused", "MemberVisibilityCanBePrivate")
package tk.xszq.otomadbot.text

import com.google.gson.Gson
import kotlinx.serialization.Serializable
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MemberJoinEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.events.NudgeEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.LightApp
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.message.data.toPlainText
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import tk.xszq.otomadbot.*
import tk.xszq.otomadbot.api.BilibiliApi
import tk.xszq.otomadbot.api.PythonApi
import tk.xszq.otomadbot.core.Counter
import tk.xszq.otomadbot.image.ImageCommonHandler
import java.io.IOException


object WelcomeHandler: EventHandler("欢迎消息", "welcome") {
    override fun register() {
        super.register()
        GlobalEventChannel.subscribeAlways<MemberJoinEvent> { event ->
            requireNot(WelcomeHandler.denied) {
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
object Repeater: EventHandler("重复草字", "kusa") {
    val kusa = Counter()
    override fun register() {
        super.register()
        GlobalEventChannel.subscribeAlways<MessageEvent> {
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
    override fun register() {
        super.register()
        GlobalEventChannel.subscribeAlways<MessageEvent> {
            message.filterIsInstance<LightApp>().forEach {
                Gson().fromJson(it.content, LightAppRoot::class.java).meta.forEach { (_, it) ->
                    quoteReply(if (it.title == "哔哩哔哩") {
                        try {
                            val bvid = getBV(it.qqdocurl)
                            val info = BilibiliApi.queryBv(bvid)
                            val result = "$bvid\n" +
                                    "${info.data.title}\n" +
                                    "${(info.data.stat["view"] as Double).toInt()}播放 " +
                                    "${(info.data.stat["danmaku"] as Double).toInt()}弹幕 " +
                                    "${(info.data.stat["reply"] as Double).toInt()}评论\n" +
                                    "UP主：${info.data.owner.name}\n" +
                                    "简介：\n" +
                                    info.data.desc
                            val cover = NetworkUtils.downloadTempFile(info.data.pic)
                            (cover?.toExternalResource()?.use { img ->
                                subject.uploadImage(img)
                            } ?: "".toPlainText()) + result
                        } catch (e: Exception) {
                            e.printStackTrace()
                            (it.qqdocurl ?: it.url).toPlainText()
                        }
                    } else (it.qqdocurl ?: it.url).toPlainText())
                }
            }
        }
    }
    fun getBV(link: String?): String {
        link ?.let {
            return OkHttpClient.Builder()
                .addNetworkInterceptor(object : Interceptor {
                    @Throws(IOException::class)
                    override fun intercept(chain: Interceptor.Chain): Response {
                        return chain.proceed(chain.request())
                    }
                })
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
object SentimentDetector: EventHandler("sentiment", "情感检测") {
    override fun register() {
        super.register()
        GlobalEventChannel.subscribeAlways<GroupMessageEvent> {
            if (message.contains(At(bot))) {
                quoteReply(ImageCommonHandler.replyPic.getRandom(if (PythonApi.sentiment(message.content)!!) "reply"
                    else "afraid").uploadAsImage(group))
            }
        }
    }
}

data class LightAppRoot(val desc: String, val view: String, val ver: String, val prompt: String,
val meta: HashMap<String, LightAppDetail>)
data class LightAppDetail(val appid: String, val desc: String, val preview: String, val qqdocurl: String?,
                          val title: String, val url: String)
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
}

object TextSettings : AutoSavePluginConfig("text") {
    val welcome by value(WelcomeTextSettings())
    val regex by value(RegexSettings())
}