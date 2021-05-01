@file:Suppress("unused")
package tk.xszq.otomadbot.database

import com.soywiz.korio.util.UUID
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.contact.isOwner
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.at
import net.mamoe.mirai.message.data.toPlainText
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.`java-time`.timestamp
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jsoup.Jsoup
import tk.xszq.otomadbot.*
import tk.xszq.otomadbot.api.MediaWikiUtils
import tk.xszq.otomadbot.media.AudioEncodeUtils
import tk.xszq.otomadbot.media.downloadFile
import tk.xszq.otomadbot.media.fonts
import tk.xszq.otomadbot.media.getDownloadFileSize
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import kotlin.random.Random

object Randoms : IntIdTable() {
    override val tableName = "random"
    val category = varchar("category", 64)
    val name = varchar("name", 64)
    val content = text("content")
    val createTime = timestamp("createtime")
}
class Random(id: EntityID<Int>) : IntEntity(id) {
    var category by Randoms.category
    var name by Randoms.name
    var content by Randoms.content
    var createTime by Randoms.createTime
    companion object : IntEntityClass<ReplyRule>(ReplyRules)
}

@Suppress("UNUSED_PARAMETER")
object RandomUtils: CommandUtils("random") {
    @CommandMatching("splashes", "splashes")
    suspend fun handleSplashes(result: MatchResult, event: MessageEvent) = event.run {
        newSuspendedTransaction(db = Databases.mysql) {
            Randoms.select { Randoms.category eq "splashes" }.orderBy(org.jetbrains.exposed.sql.Random()).firstOrNull()
                ?.let { quoteReply(it[Randoms.content]) }
        }
    }
    @CommandEqualsTo("随机uuid", "uuid")
    suspend fun handleUUID(event: MessageEvent) = event.run {
        quoteReply(UUID.randomUUID().toString())
    }
    @Command("随机数字", "integer")
    suspend fun handleInteger(args: Args, event: MessageEvent) = event.run {
        quoteReply(when (args.size) {
            0 -> Random.nextInt().toString()
            1 -> Random.nextLong(args.first().toLong()).toString()
            2 -> Random.nextLong(args.first().toLong(), args.last().toLong()).toString()
            else -> "随机数字将生成 {x|下界<=x<上界} 内的数字。使用方法：\n①随机数字\n②随机数字 上界\n③随机数字 下界 上界"
        })
    }
    @Command("随机群友", "group.member", true)
    suspend fun handleGroupMember(args: Args, event: GroupMessageEvent) = event.run {
        quoteReply((if (args.isNotEmpty()) group.members.filter {
            when (args.first()) {
                "不含管理员" -> !it.isOperator()
                "不含群主" -> !it.isOwner()
                else -> true
            }
        }.randomOrNull()
            else group.members.random()) ?.let { selected ->
                if (sender.isOperator())
                    selected.at()
                else
                    "${selected.nameCardOrNick} (${selected.id})".toPlainText()
            } ?: "本群似乎没有非管理员的成员哦~\n使用方法：①随机群友\n②随机群友 不含管理员\n③随机群友 不含群主".toPlainText())
    }
    @CommandEqualsTo("随机字体", "font")
    suspend fun handleFont(event: MessageEvent) = event.run {
        newSuspendedTransaction(db = Databases.mysql) {
            val fontItem = fonts.random()
            val font = Font(fontItem.second.fontName, Font.PLAIN, 48)
            var img = BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)
            var g2d = img.createGraphics()
            g2d.font = font
            var fm = g2d.fontMetrics
            val width = fm.stringWidth(fontItem.first)
            val height = fm.height
            g2d.dispose()
            img = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            g2d = img.createGraphics()
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY)
            g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE)
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
            g2d.font = font
            fm = g2d.fontMetrics
            g2d.color = Color.WHITE
            g2d.fillRect(0, 0, width, height)
            g2d.color = Color.BLACK
            g2d.drawString(fontItem.first, 0, fm.ascent)
            g2d.dispose()
            img.toByteArray().toExternalResource().use {
                quoteReply(subject.uploadImage(it))
            }
        }
    }
    @CommandEqualsTo("随机chiptune", "chiptune", true)
    suspend fun randomChiptune(event: GroupMessageEvent) = event.run {
        val client = HttpClient(CIO)
        for (i in 0..20) {
            val html = Jsoup.parse(
                client.get<HttpResponse>("https://modarchive.org/index.php?request=view_random")
                    .readText()
            )
            val title = html.selectFirst("h1").text()
            val link = html.selectFirst("a.standard-link").attr("href")
            val ext = "." + link.split(".").last()
            if (getDownloadFileSize(link) >= 1048576L || ext !in arrayOf(".xm", ".mod", ".it", ".mptm", ".s3m"))
                continue
            val mod = downloadFile(link, UUID.randomUUID().toString(), ext)
            val raw = AudioEncodeUtils.cropPeriod(mod, 5.0, 15.0)!!
            val after = AudioEncodeUtils.mp3ToSilk(raw)
            after.toExternalResource().use {
                group.sendMessage(group.sendMessage(group.uploadVoice(it)).quote() + title)
            }
            raw.delete()
            after.delete()
            mod.delete()
            break
        }
    }
    @CommandEqualsTo("随机教程", "tutorial")
    suspend fun getRandomTutorial(event: MessageEvent) = event.run {
        val html = Jsoup.parse(
            MediaWikiUtils.client.get<HttpResponse>
            ("https://otomad.wiki/%E5%88%B6%E4%BD%9C%E6%95%99%E7%A8%8B").readText())
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
}