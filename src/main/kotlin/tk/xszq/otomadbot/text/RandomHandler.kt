package tk.xszq.otomadbot.text

import com.soywiz.korio.util.UUID
import io.ktor.client.*
import io.ktor.client.request.*
import net.mamoe.mirai.contact.AudioSupported
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.contact.isOwner
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.at
import net.mamoe.mirai.message.data.toPlainText
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import org.jsoup.Jsoup
import tk.xszq.otomadbot.*
import tk.xszq.otomadbot.audio.AudioEncodeUtils
import tk.xszq.otomadbot.core.Cooldown
import tk.xszq.otomadbot.core.ifReady
import tk.xszq.otomadbot.core.update
import kotlin.random.Random

object RandomHandler: EventHandler("随机功能", "random") {
    private val cooldown = Cooldown("random")
    override fun register() {
        super.register()
        GlobalEventChannel.subscribeGroupMessages {
            equalsTo("随机教程") {
                ifReady(cooldown) {
                    requireNot(denied) {
                        handleTutorial(this)
                        update(cooldown)
                    }
                }
            }
            equalsTo("随机chiptune") {
                ifReady(cooldown) {
                    requireNot(denied) {
                        handleChiptune(this)
                        update(cooldown)
                    }
                }
            }
            equalsTo("随机uuid") {
                ifReady(cooldown) {
                    requireNot(denied) {
                        handleUUID(this)
                        update(cooldown)
                    }
                }
            }
            startsWithSimple("随机数字") { rawArg, _ ->
                ifReady(cooldown) {
                    requireNot(denied) {
                        handleNumber(rawArg.toArgsList(), this)
                        update(cooldown)
                    }
                }
            }
            startsWithSimple("随机群友") { rawArg, _ ->
                ifReady(cooldown) {
                    requireNot(denied) {
                        handleMember(rawArg.toArgsList(), this)
                        update(cooldown)
                    }
                }
            }
        }
    }
    val client = HttpClient()
    @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
    private suspend fun handleTutorial(event: MessageEvent) = event.run {
        val html = Jsoup.parse(
            client.get<String>("https://otomad.wiki/%E5%88%B6%E4%BD%9C%E6%95%99%E7%A8%8B")
        )!!
        val links = mutableListOf<String>()
        html.selectFirst(".mw-body")!!.select("a").forEach {
            val href = it.attr("href")
            if ("#" !in href) links.add(href)
        }
        var selected = links.random()
        if (selected[0] == '/')
            selected = "https://otomad.wiki$selected"
        quoteReply(selected)
    }
    @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
    private suspend fun handleChiptune(event: MessageEvent) = event.run {
        if (subject !is AudioSupported)
            return@run
        for (i in 0..20) {
            val html = Jsoup.parse(
                client.get<String>("https://modarchive.org/index.php?request=view_random")
            )!!
            val title = html.selectFirst("h1")!!.text()
            val link = html.selectFirst("a.standard-link")!!.attr("href")
            val ext = "." + link.split(".").last()
            if (NetworkUtils.getDownloadFileSize(link) >= 1048576L || ext !in arrayOf(".xm", ".mod", ".it", ".mptm", ".s3m"))
                continue
            val mod = NetworkUtils.downloadTempFile(link, ext=ext)
            val raw = AudioEncodeUtils.cropPeriod(mod!!, 5.0, 15.0)!!
            val after = AudioEncodeUtils.mp3ToSilk(raw)
            after.toExternalResource().use {
                subject.sendMessage(subject.sendMessage((subject as AudioSupported).uploadAudio(it)).quote() + title)
            }
            raw.delete()
            after.delete()
            mod.delete()
            break
        }
    }
    private suspend fun handleUUID(event: MessageEvent) = event.run {
        quoteReply(UUID.randomUUID().toString())
    }
    private suspend fun handleNumber(args: Args, event: MessageEvent) = event.run {
        quoteReply(when (args.size) {
            0 -> Random.nextInt().toString()
            1 -> Random.nextLong(args.first().toLong()).toString()
            2 -> Random.nextLong(args.first().toLong(), args.last().toLong()).toString()
            else -> "随机数字将生成 {x|下界<=x<上界} 内的数字。使用方法：\n①随机数字\n②随机数字 上界\n③随机数字 下界 上界"
        })
    }
    private suspend fun handleMember(args: Args, event: GroupMessageEvent) = event.run {
        quoteReply((if (args.isNotEmpty()) group.members.filter {
            when (args.first()) {
                "不含管理员" -> !it.isOperator()
                "不含群主" -> !it.isOwner()
                else -> true
            }
        }.filter { it.id != bot.id }.randomOrNull()
        else group.members.random()) ?.let { selected ->
            if (sender.isOperator())
                selected.at()
            else
                "${selected.nameCardOrNick} (${selected.id})".toPlainText()
        } ?: "本群似乎没有非管理员的成员哦~\n使用方法：①随机群友\n②随机群友 不含管理员\n③随机群友 不含群主".toPlainText())
    }
}