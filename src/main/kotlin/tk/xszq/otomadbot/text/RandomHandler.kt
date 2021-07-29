package tk.xszq.otomadbot.text

import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import ru.gildor.coroutines.okhttp.await
import tk.xszq.otomadbot.*
import tk.xszq.otomadbot.audio.AudioEncodeUtils

object RandomHandler: EventHandler("随机", "random") {
    override fun register() {
        super.register()
        GlobalEventChannel.subscribeMessages {
            equalsTo("随机教程") {
                handleTutorial(this)
            }
        }
    }
    private suspend fun handleTutorial(event: MessageEvent) = event.run {
        val html = Jsoup.parse(
            OkHttpClient().newCall(
                Request.Builder()
                    .url("https://otomad.wiki/%E5%88%B6%E4%BD%9C%E6%95%99%E7%A8%8B")
                    .build())
                .await().body!!.get())
        val links = mutableListOf<String>()
        html.selectFirst(".mw-body").select("a").forEach {
            val href = it.attr("href")
            if ("#" !in href) links.add(href)
        }
        var selected = links.random()
        if (selected[0] == '/')
            selected = "https://otomad.wiki$selected"
        quoteReply(selected)
    }
    suspend fun handleChiptune(event: MessageEvent) = event.run {
        val client = OkHttpClient()
        for (i in 0..20) {
            val html = Jsoup.parse(
                client.newCall(
                    Request.Builder().url("https://modarchive.org/index.php?request=view_random").build())
                    .await().body!!.get()
            )
            val title = html.selectFirst("h1").text()
            val link = html.selectFirst("a.standard-link").attr("href")
            val ext = "." + link.split(".").last()
            if (NetworkUtils.getDownloadFileSize(link) >= 1048576L || ext !in arrayOf(".xm", ".mod", ".it", ".mptm", ".s3m"))
                continue
            val mod = NetworkUtils.downloadTempFile(link, ext=ext)
            val raw = AudioEncodeUtils.cropPeriod(mod, 5.0, 15.0)!!
            val after = AudioEncodeUtils.mp3ToSilk(raw)
            after.toExternalResource().use {
                sendMessage(subject.sendMessage(subject.uploadVoice(it)).quote() + title)
            }
            raw.delete()
            after.delete()
            mod.delete()
            break
        }
    }

}