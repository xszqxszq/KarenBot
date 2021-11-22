@file:Suppress("unused", "EXPERIMENTAL_API_USAGE")

package tk.xszq.otomadbot.api

import com.twelvemonkeys.image.ConvolveWithEdgeOp
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.gildor.coroutines.okhttp.await
import tk.xszq.otomadbot.*
import tk.xszq.otomadbot.core.OtomadBotCore
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import java.awt.font.TextAttribute
import java.awt.geom.AffineTransform
import java.awt.image.*
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.roundToInt


@Serializable
class B40Result(val nickname: String, val rating: Int, val additional_rating: Int, val username: String,
                val charts: Map<String, List<B40ScoreResult>>)

@Serializable
open class B40ScoreResult(val achievements: Double, val ds: Double, val dxScore: Int, val fc: String, val fs: String,
                          val level: String, val level_index: Int, val level_label: String, val ra: Int,
                          val rate: String, val song_id: Int, val title: String, val type: String)
object EmptyB40ScoreResult: B40ScoreResult(-114.00, .0, 0, "", "", "",
    0, "", -100, "", -114514, "", "")
fun List<B40ScoreResult>.fillEmpty(target: Int): List<B40ScoreResult> {
    val result = toMutableList()
    for (i in (1..(target-size)))
        result.add(EmptyB40ScoreResult)
    return result
}

object MaimaiDXHandler: EventHandler("舞萌DX", "maimaidx") {
    override fun register() {
        GlobalEventChannel.subscribeMessages {
            startsWithSimple("b40") { _, username ->
                requireNot(denied) {
                    if (username.isBlank())
                        handleB40(id = sender.id.toString(), event = this)
                    else
                        handleB40("username", username, this)
                }
            }
        }
        super.register()
    }

    private suspend fun handleB40(type: String = "qq", id: String, event: MessageEvent) = event.run {
        val payload = buildJsonObject { put(type, id) }
        val request = Request.Builder()
            .url(ApiSettings.list["maimaidxprober"]!!.url)
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        val result = OkHttpClient().newCall(request).await()
        when (result.code) {
            200 -> {
                val info = OtomadBotCore.json.decodeFromString<B40Result>(result.body!!.get())
                println(OtomadBotCore.json.encodeToString(info))
                val img = generateB40Image(info)
                quoteReply(img.toInputStream()!!.uploadAsImage(subject))
            }
            400 -> quoteReply("用户名不存在，请确认用户名对应的玩家在 Diving-Fish 的舞萌 DX 查分器" +
                    "（https://www.diving-fish.com/maimaidx/prober/）上已注册")
            403 -> quoteReply("该玩家已禁止他人查询成绩")
        }
        pass
    }
    private fun generateB40Image(info: B40Result): BufferedImage {
        val bufferedImage = ImageIO.read(OtomadBotCore.configFolder.resolve(MaimaiConfig.bg))
        val g2d = bufferedImage.createGraphics()
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        // Basic info
        val realRating = info.rating + info.additional_rating
        g2d.drawImage(ImageIO.read(OtomadBotCore.configFolder
            .resolve("image/maimai/rating_base_${ratingColor(realRating)}.png")), 451, 16, null)
        g2d.drawString(info.nickname.toSBC(), MaimaiConfig.name)
        g2d.drawString(realRating.toString(), MaimaiConfig.dxrating, Color.yellow)
        g2d.drawString("底分：${info.rating} + 段位分：${info.additional_rating}", MaimaiConfig.ratingDetail)

        // Charts
        g2d.drawCharts(info.charts["sd"]!!.fillEmpty(25), 5, 69, 210, 8)
        g2d.drawCharts(info.charts["dx"]!!.fillEmpty(15), 3, 936, 210, 8)

        g2d.dispose()

        return bufferedImage
    }
    private fun ratingColor(rating: Int): String = when (rating) { // TODO: 找到白框文件名
        in (0..1999) -> "blue"
        in (2000..2999) -> "green"
        in (3000..3999) -> "orange"
        in (4000..4999) -> "red"
        in (5000..5999) -> "purple"
        in (6000..6999) -> "bronze"
        in (7000..7999) -> "silver"
        in (8000..8499) -> "gold"
        in (8500..20000) -> "rainbow"
        else -> "blue"
    }
}

fun String.ellipsize(max: Int): String {
    var result = ""
    var cnt = 0
    forEach {
        cnt += if (it.isDBC()) 1 else 2
        if (cnt > max) return@forEach
        result += it
    }
    return result + if (result.length == length) "…" else ""
}

@Serializable
class MaimaiTextAttr(val fontName: String, val fontSize: Int, val x: Int, val y: Int, val tracking: Double = .0,
                     val hAlign: TextHAlign = TextHAlign.LEFT)

object MaimaiConfig: AutoSavePluginConfig("maimai") {
    val bg by value("image/maimai/dx2021_otmbot.png")
    val name by value(MaimaiTextAttr("方正悠黑_513B", 30, 84, 45))
    val dxrating by value(MaimaiTextAttr("Arial Black", 16, 592, 39, 0.29,
        TextHAlign.RIGHT))
    val ratingDetail by value(MaimaiTextAttr("Source Han Sans CN", 20, 340, 102,
        hAlign = TextHAlign.CENTER))
    val chTitle by value(MaimaiTextAttr("方正悠黑_513B", 18, 8, 22))
    val chAchievements by value(MaimaiTextAttr("方正悠黑_513B", 16, 8, 44))
    val chBase by value(MaimaiTextAttr("方正悠黑_513B", 16, 8, 60))
    val chRank by value(MaimaiTextAttr("方正悠黑_513B", 18, 8, 80))
}
fun Graphics2D.drawString(text: String, attr: MaimaiTextAttr, fontColor: Color = Color.black) {
    val attrs = mapOf(Pair(TextAttribute.TRACKING, attr.tracking))
    font = Font(attr.fontName, Font.PLAIN, attr.fontSize).deriveFont(attrs)
    color = fontColor
    val r2d = font.getStringBounds(text,
        FontRenderContext(null, true, true)
    )
    val xOffset = when (attr.hAlign) {
        TextHAlign.LEFT -> 0
        TextHAlign.CENTER -> - r2d.width.roundToInt() / 2
        TextHAlign.RIGHT -> - r2d.width.roundToInt()
    }
    drawString(text, attr.x + xOffset, attr.y)
}
fun Graphics2D.drawStringRelative(text: String, x: Int, y: Int, attr: MaimaiTextAttr, fontColor: Color = Color.black) {
    val attrs = mapOf(Pair(TextAttribute.TRACKING, attr.tracking))
    font = Font(attr.fontName, Font.PLAIN, attr.fontSize).deriveFont(attrs)
    color = fontColor
    val r2d = font.getStringBounds(text,
        FontRenderContext(null, true, true)
    )
    val xOffset = when (attr.hAlign) {
        TextHAlign.LEFT -> 0
        TextHAlign.CENTER -> - r2d.width.roundToInt() / 2
        TextHAlign.RIGHT -> - r2d.width.roundToInt()
    }
    drawString(text, x + attr.x + xOffset, y + attr.y)
}
fun resolveCover(title: String): File {
    val result = OtomadBotCore.configFolder.resolve("image/maimai/cover/${title.md5()}.jpg")
    if (result.exists())
        return result
    return OtomadBotCore.configFolder.resolve("image/maimai/cover/default.jpg")
}
fun Graphics2D.drawCharts(charts: List<B40ScoreResult>, cols: Int, startX: Int, startY: Int, gap: Int) {
    charts.sortedWith(compareBy({ -it.ra }, { it.achievements })).forEachIndexed { index, chart ->
        val coverRaw = ImageIO.read(resolveCover(chart.title)).scale(160.0 / 190)
        val newHeight = (coverRaw.width * 9.0 / 16.0).roundToInt()
        val cover = coverRaw.getSubimage(0, (coverRaw.height - newHeight) / 2, coverRaw.width, newHeight).blur()
            .brightness(0.6f)
        val x = startX + (index % cols) * (coverRaw.width + gap)
        val y = startY + (index / cols) * (newHeight + gap)

        val shadowOffset = 2
        color = Color.black
        fillRect(x + shadowOffset, y + shadowOffset, coverRaw.width, newHeight)
        drawImage(cover, x, y, null)

        if (chart.title != "") {
            drawImage(ImageIO.read(OtomadBotCore.configFolder
                .resolve("image/maimai/label_${chart.level_label.replace(":", "")}.png")),
                x-19, y, null) // Difficulty

            // Details
            drawStringRelative(chart.title.ellipsize(12), x, y, MaimaiConfig.chTitle, Color.white)
            drawStringRelative(chart.achievements.toString() + "%", x, y, MaimaiConfig.chAchievements, Color.white)
            drawStringRelative("Base: ${chart.ds} -> ${chart.ra}", x, y, MaimaiConfig.chBase, Color.white)
            drawStringRelative("#${index + 1}(${chart.type})", x, y, MaimaiConfig.chRank, Color.white)
            drawImage(ImageIO.read(OtomadBotCore.configFolder
                .resolve("image/maimai/music_icon_${chart.rate}.png"))
                .scale(0.8), x + 90, y + 25, null)
            if (chart.fc.isNotEmpty()) {
                drawImage(ImageIO.read(OtomadBotCore.configFolder
                    .resolve("image/maimai/music_icon_${chart.fc}.png")).scale(0.5),
                    x + 135, y + 25, null)
            }
        }
    }
}
fun BufferedImage.scale(scale: Double): BufferedImage {
    val after = BufferedImage((width*scale).roundToInt(), (height*scale).roundToInt(),
        BufferedImage.TYPE_INT_ARGB)
    AffineTransformOp(AffineTransform.getScaleInstance(scale, scale), AffineTransformOp.TYPE_BILINEAR)
        .filter(this, after)
    return after
}
fun BufferedImage.blur(radius: Int = 4): BufferedImage {
    val size = radius * 2 + 1
    val weight = 1.0f / (size * size)
    val data = generateSequence { weight }.take(size * size).toList().toFloatArray()
    val kernel = Kernel(size, size, data)
    val op: BufferedImageOp = ConvolveWithEdgeOp(kernel, ConvolveWithEdgeOp.EDGE_REFLECT, null)
    return op.filter(this, null)
}
fun BufferedImage.brightness(factor: Float = 0.8f): BufferedImage {
    RescaleOp(factor, 15f, null).filter(this, this)
    return this
}