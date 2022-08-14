package xyz.xszq.otomadbot.text

import com.soywiz.korio.util.UUID
import io.ktor.client.*
import io.ktor.client.request.*
import net.mamoe.mirai.contact.AudioSupported
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.contact.isOwner
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.at
import net.mamoe.mirai.message.data.toPlainText
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import org.jsoup.Jsoup
import xyz.xszq.*
import xyz.xszq.otomadbot.*
import xyz.xszq.otomadbot.audio.AudioEncodeUtils
import xyz.xszq.otomadbot.audio.getAudioDuration
import xyz.xszq.otomadbot.kotlin.toArgsList
import xyz.xszq.otomadbot.mirai.equalsTo
import xyz.xszq.otomadbot.mirai.quoteReply
import xyz.xszq.otomadbot.mirai.startsWithSimple
import java.io.File
import kotlin.random.Random


object RandomHandler: CommandModule("随机功能", "random") {
    private val audioExts = listOf("mp3", "wav", "ogg", "m4a")
    private val cooldown = Cooldown("random")
    private val quota = Quota("audio_random")
    private const val quotaExceededMessage = "今日该功能限额已经用完了哦~"
    val client = HttpClient()
    override suspend fun subscribe() {
        events.subscribeGroupMessages {
            equalsTo("随机教程") {
                tutorial.checkAndRun(this)
            }
            equalsTo("随机chiptune") {
                chiptune.checkAndRun(this)
            }
            equalsTo("随机uuid") {
                uuid.checkAndRun(this)
            }
            startsWithSimple("随机数字") { rawArg, _ ->
                number.checkAndRun(CommandEvent(rawArg.toArgsList(), this))
            }
            startsWithSimple("随机群友") { rawArg, _ ->
                member.checkAndRun(CommandEvent(rawArg.toArgsList(), this))
            }
            startsWithSimple("随机东方原曲") { _, _ ->
                touhou.checkAndRun(this)
            }
            startsWithSimple("随机maimai") { _, _ ->
                finale.checkAndRun(this)
            }
            startsWithSimple("随机dx") { _, _ ->
                dx.checkAndRun(this)
            }
        }
    }
    val tutorial = GroupCommand("随机教程", "tutorial") {
        ifReady(cooldown) {
            val html = Jsoup.parse(
                client.get<String>("https://otomad.wiki/%E5%88%B6%E4%BD%9C%E6%95%99%E7%A8%8B")
            )
            val links = mutableListOf<String>()
            html.selectFirst(".mw-body")!!.select("a").forEach {
                val href = it.attr("href")
                if ("#" !in href) links.add(href)
            }
            var selected = links.random()
            if (selected[0] == '/')
                selected = "https://otomad.wiki$selected"
            quoteReply(selected)
            update(cooldown)
        }
    }
    val chiptune = GroupCommand("随机chiptune", "chiptune") {
        ifReady(cooldown) {
            for (i in 0..20) {
                val html = Jsoup.parse(
                    client.get<String>("https://modarchive.org/index.php?request=view_random")
                )
                val title = html.selectFirst("h1")!!.text()
                val link = html.selectFirst("a.standard-link")!!.attr("href")
                val ext = link.split(".").last()
                if (NetworkUtils.getDownloadFileSize(link) >= 1048576L || ext !in arrayOf(
                        "xm", "mod", "it", "mptm", "s3m"
                    )
                )
                    continue
                val mod = NetworkUtils.downloadTempFile(link, ext = ext)
                val after = AudioEncodeUtils.cropPeriod(mod!!, 5.0, 15.0)!!
                after.toExternalResource().use {
                    subject.sendMessage(
                        subject.sendMessage((subject as AudioSupported).uploadAudio(it)).quote() + title
                    )
                }
                after.delete()
                mod.delete()
                break
            }
            update(cooldown)
        }
    }
    val uuid = GroupCommand("随机UUID", "uuid") {
        ifReady(cooldown) {
            quoteReply(UUID.randomUUID().toString())
            update(cooldown)
        }
    }
    val number = GroupCommandWithArgs("随机数字", "number") {
        event.ifReady(cooldown) {
            event.quoteReply(when (args.size) {
                0 -> Random.nextInt().toString()
                1 -> Random.nextLong(args.first().toLong()).toString()
                2 -> Random.nextLong(args.first().toLong(), args.last().toLong()).toString()
                else -> "随机数字将生成 {x|下界<=x<上界} 内的数字。使用方法：\n①随机数字\n②随机数字 上界\n③随机数字 下界 上界"
            })
            event.update(cooldown)
        }
    }
    val member = GroupCommandWithArgs("随机群员", "member") {
        event.ifReady(cooldown) {
            event.quoteReply((if (args.isNotEmpty()) event.group.members.filter {
                when (args.first()) {
                    "不含管理员" -> !it.isOperator()
                    "不含群主" -> !it.isOwner()
                    else -> true
                }
            }.filter { it.id != event.bot.id }.randomOrNull()
            else event.group.members.random())?.let { selected ->
                if (event.sender.isOperator())
                    selected.at()
                else
                    "${selected.nameCardOrNick} (${selected.id})".toPlainText()
            } ?: "本群似乎没有非管理员的成员哦~\n使用方法：①随机群友\n②随机群友 不含管理员\n③随机群友 不含群主".toPlainText())
            event.update(cooldown)
        }
    }
    val touhou = GroupCommand("随机东方原曲", "touhou") {
        if (available(quota)) {
            subject.sendMessage(fetchVoice("touhou", this))
            quota.update(subject)
        } else {
            quoteReply(quotaExceededMessage)
        }
    }
    val finale = GroupCommand("随机maimai", "finale") {
        if (available(quota)) {
            subject.sendMessage(fetchVoice("finale", this))
            quota.update(subject)
        } else {
            quoteReply(quotaExceededMessage)
        }
    }
    val dx = GroupCommand("随机dx", "dx") {
        if (available(quota)) {
            subject.sendMessage(fetchVoice("dx", this))
            quota.update(subject)
        } else {
            quoteReply(quotaExceededMessage)
        }
    }
    fun fetchRandom(type: String): File = OtomadBotCore.configFolder.resolve("music/$type").listFiles()!!
        .filter { it.extension in audioExts }.random()
    suspend fun fetchVoice(type: String, event: MessageEvent) = event.run {
        val raw = fetchRandom(type)
        val before = getRandomPeriod(raw)!!
        val result = before.toExternalResource().use {
            (subject as AudioSupported).uploadAudio(it)
        }
        before.delete()
        return@run result
    }
    const val defaultMinDuration = 15.0
    suspend fun getRandomPeriod(file: File, duration: Double = defaultMinDuration): File? = AudioEncodeUtils.cropPeriod(
        file, Random.nextDouble(0.0, file.getAudioDuration() - duration), duration)
}