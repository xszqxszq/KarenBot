@file:Suppress("unused", "UNUSED_PARAMETER")
package tk.xszq.otomadbot.media

import com.google.gson.Gson
import com.google.zxing.*
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.client.j2se.MatrixToImageWriter.toBufferedImage
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.soywiz.korio.file.std.localVfs
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.contact.Contact.Companion.uploadImage
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.message.data.anyIsInstance
import net.mamoe.mirai.message.nextMessage
import net.mamoe.mirai.utils.ExternalResource.Companion.sendAsImageTo
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import org.jsoup.Jsoup
import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula
import tk.xszq.otomadbot.*
import tk.xszq.otomadbot.api.PyApi
import tk.xszq.otomadbot.database.*
import java.awt.Color
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.image.BufferedImage
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.DecimalFormat
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.roundToInt
import kotlin.random.Random.Default.nextInt

val fonts = mutableListOf<Pair<String, Font>>()

suspend fun isValidGIF(file: File): Boolean {
    return try {
        val header = localVfs(file).readRangeBytes(0..5)
        header.contentEquals("GIF89a".toByteArray())
    } catch (e: Exception) {
        false
    }
}
fun isValidGIFBlocking(file: File): Boolean = runBlocking { isValidGIF(file) }

fun isImage(filename: Path): Boolean {
    return getMIMEType(filename).split("/")[0] == "image"
}

fun BufferedImage.save(location: String, format: String = "png"): File {
    val result = File(location)
    ImageIO.write(this, format, result)
    return result
}
fun BufferedImage.save(file: File, format: String = "png"): File {
    ImageIO.write(this, format, file)
    return file
}

object ImageUtils: CommandUtils("image") {
    @Command("生成二维码", "qrcode.encode")
    suspend fun generateQRCode(args: Args, event: MessageEvent) = event.run {
        args.firstOrNull()?.let { link ->
            link.generateQR()?.let {
                quoteReply(subject.uploadImage(it.toInputStream()!!))
            }
        }
    }
    @CommandEqualsTo("搜图", "search.illusion")
    suspend fun searchIllusion(event: MessageEvent) = event.run {
        quoteReply("请发送想要搜索的图片（暂不支持三次元图片）：")
        val pics = nextMessage()
        var tempCounter = 0
        pics.forEach { pic ->
            if (pic is Image) {
                tempCounter += 1
                subject.sendMessage(
                    pics.quote() + (if (tempCounter > 1) "【图$tempCounter】" else "")
                            + getImageSearchByUrl(pic.queryUrl())
                )
            }
        }
    }
    @Command("搜图", "search.illusion")
    suspend fun searchIllusionInSingleMessage(args: Args, event: MessageEvent) = event.run {
        var tempCounter = 0
        message.forEach { pic ->
            if (pic is Image) {
                tempCounter += 1
                subject.sendMessage(message.quote() + (if (tempCounter > 1) "【图$tempCounter】" else "")
                        + getImageSearchByUrl(pic.queryUrl()))
            }
        }
    }
    @CommandEqualsTo("搜番", "search.bangumi")
    suspend fun searchBangumi(event: MessageEvent) = event.run {
        quoteReply("请发送想要搜索的番剧截图：")
        val pics = nextMessageEvent()
        pics.message.forEach { pic ->
            if (pic is Image) {
                doHandleTraceMoe(pic, pics)
            }
        }
    }
    @Command("搜番", "search.bangumi")
    suspend fun searchBangumiInSingleMessage(args: Args, event: MessageEvent) = event.run {
        message.forEach { pic ->
            if (pic is Image) {
                doHandleTraceMoe(pic, this)
            }
        }
    }
    @CommandSingleArg("生成latex", "latex")
    suspend fun renderLatex(text: String, event: MessageEvent) = event.run {
        val formula = TeXFormula(text)
        val result = newTempFile()
        formula.createPNG(TeXConstants.STYLE_DISPLAY, 22.0F, result.absolutePath, Color.WHITE, Color.BLACK)
        result.toExternalResource().use {
            quoteReply(subject.uploadImage(it))
        }
        result.delete()
    }
    @Command("球面化", "spherize")
    suspend fun spherize(args: Args, event: MessageEvent) = event.run {
        if (message.anyIsInstance<Image>()) { message }
        else { quoteReply("请发送想要球面化的图片："); nextMessage() }.filterIsInstance<Image>().forEach {
            quoteReply(PyApi().getSpherizedImage(it)!!.toExternalResource().uploadAsImage(subject))
        }
    }
}

suspend fun getImageSearchByUrl(url: String): String {
    try {
        val client = HttpClient(CIO)
        val response = client.post<HttpResponse>("https://saucenao.com/search.php") {
            body = MultiPartFormDataContent(formData {
                append("url", url)
            })
        }
        return if (response.isSuccessful()) {
            val doc = Jsoup.parse(response.readText())
            val target = doc.select(".resulttablecontent")[0]
            val similarity = target.select(".resultsimilarityinfo").text()
            try {
                val name = target.select(".resulttitle").text()
                val links = target.select(".resultcontentcolumn>a")
                var link = target.select(".resultmiscinfo>a").attr("href")
                if (link == "") target.select(".resulttitle>a").attr("href")
                if (link == "") link = links[0].attr("href")
                val author =
                    when {
                        target.select(".resulttitle").toString().contains(
                            "<strong>Creator: </strong>",
                            ignoreCase = true
                        ) -> target.select(
                            ".resulttitle"
                        ).text()
                        links.size == 1 -> links[0].text()
                        links.size == 0 -> "Various Artist"
                        else -> links[1].text()
                    }
                "[$similarity] $name by $author\n$link"
            } catch (e: Exception) {
                "[$similarity] " + target.select(".resultcontent").text()
            } + "\n结果来自SauceNao，本bot不保证结果准确性，谢绝辱骂"
        } else {
            "网络连接失败，请稍后重试QWQ"
        }
    } catch (e: Exception) {
        return "网络连接失败，请稍后重试QWQ"
    }
}

class TraceMoeResult(val docs: List<HashMap<String, Any>>)

/**
 * Handle Trace.moe request.
 * @param image Image to query.
 * @param message Request message event.
 */
suspend fun doHandleTraceMoe(image: Image, message: MessageEvent) {
    val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 10000
        }
    }
    val response = client.get<HttpResponse>("https://trace.moe/api/search?url=${image.queryUrl()}")
    if (response.isSuccessful()) {
        val result = Gson().fromJson(response.readText(), TraceMoeResult::class.java)
        try {
            if (result.docs.isNotEmpty()) {
                val realResult = result.docs[0]
                message.subject.sendMessage(
                    message.message.quote() + "[${
                        DecimalFormat("0.##").format(
                            realResult["similarity"]!!.toString().toFloat() * 100.0
                        )
                    }%] " +
                            "${realResult["title_chinese"]} " +
                            if (realResult["season"] != "") "(${realResult["season"]}) " else "" +
                                    "第${realResult["episode"].toString().toFloat().roundToInt()}集 " +
                                    String.format(
                                        "%d:%02d", realResult["at"]!!.toString().toFloat().roundToInt() / 60,
                                        realResult["at"]!!.toString().toFloat().roundToInt() % 60
                                    ) +
                                    "\n结果来自trace.moe，本bot不保证结果准确性，谢绝辱骂"
                )
            } else {
                message.subject.sendMessage(message.message.quote() + "没有找到相关番剧……")
            }
        } catch (e: Exception) {
            message.subject.sendMessage(message.message.quote() + "没有找到相关番剧……")
        }
    } else {
        message.subject.sendMessage(message.message.quote() + "网络错误，请重试")
    }
}
class ReplyPicList {
    @SuppressWarnings("WeakerAccess")
    val list = HashMap<String, ArrayList<Path>>()
    @SuppressWarnings("WeakerAccess")
    val included = mutableListOf<String>()
    fun load(dir: String, target: String = dir) {
        if (!list.containsKey(target)) {
            list[target] = arrayListOf()
            included.add(target)
        }
        Files.walk(Paths.get("$pathPrefix/image/$dir"), 2).filter { i -> Files.isRegularFile(i) }
            .forEach { path -> list[target]!!.add(path) }
    }
    fun getRandom(dir: String): File? = list[dir]?.let { it[nextInt(it.size)].toFile() }
    fun isDirIncluded(dir: String) = included.contains(dir)
    fun insert(dir: String, element: String) = list[dir]!!.add(File(element).toPath())
}
fun File.decodeQR(): String {
    val binarizer = HybridBinarizer(BufferedImageLuminanceSource(ImageIO.read(FileInputStream(this))))
    val decodeHints = HashMap<DecodeHintType, Any?>()
    decodeHints[DecodeHintType.CHARACTER_SET] = "UTF-8"
    val result = MultiFormatReader().decode(BinaryBitmap(binarizer), decodeHints)
    return result.text
}

fun String.generateQR(): BufferedImage? {
    val link = this
    val hints = HashMap<EncodeHintType, Any>()
    hints[EncodeHintType.CHARACTER_SET] = "utf-8"
    hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M
    hints[EncodeHintType.MARGIN] = 2
    return try {
        toBufferedImage(MultiFormatWriter().encode(link, BarcodeFormat.QR_CODE, 300, 300, hints))
    } catch (e: Exception) {
        null
    }
}
fun String.isGif(): Boolean = getMIMEType(Paths.get(this)).split("/")[1] == "gif"
fun String.isNotGif(): Boolean = !isGif()

fun BufferedImage.toInputStream(): InputStream? {
    val stream = ByteArrayOutputStream()
    return try {
        ImageIO.write(this, "png", stream)
        ByteArrayInputStream(stream.toByteArray())
    } catch (e: Exception) {
        null
    }
}

suspend fun doHandleImage(event: GroupMessageEvent) {
    var hso = false
    event.run {
        message.forEach { msg ->
            if (msg is Image && msg.imageId.split(".").last() != "gif") {
                var file: File? = msg.getFile()
                file ?.let { img ->
                    when {
                        isTargetH2Image("reply", img) ->
                            require("image.auto.reply") {
                                event.getCooldown("reply").onReady { cooldown ->
                                    replyPic.getRandom("reply")?.let { pic ->
                                        pic.sendAsImageTo(event.group)
                                        cooldown.update()
                                    }
                                }
                            }
                        isTargetH2Image("ma", img) ->
                            require("image.auto.ma") {
                                event.getCooldown("reply").onReady { cooldown ->
                                    replyPic.getRandom("reply")?.let { pic ->
                                        pic.sendAsImageTo(event.group)
                                        cooldown.update()
                                    }
                                }
                            }
                        else -> pass
                    }
                    require("image.qrcode.decode") {
                        try {
                            val result = img.decodeQR()
                            quoteReply(result)
                        } catch (e: Exception) {
                            pass
                        }
                    }
                    requireOrInit("eropic.detect", 0, configEropic!!.limit["eropic_detect"]!!) {
                        if (!isValidGIF(img) && !isTargetH2Image("eropic_detect_cache", img)) {
                            doInsertIntoH2Database("eropic_detect_cache", img)
                            if (isEropic(msg)) {
                                img.renameTo(File("$pathPrefix/image/eropic/" + img.name))
                                file = null
                                hso = true
                            }
                        }
                    }
                }
                file?.delete()
            }
        }
        if (hso)
            require("eropic.detect.reply") {
                getCooldown("hso").onReady {
                    it.update()
                    event.quoteReply("hso")
                }
            }
    }
}

fun initFonts() {
    val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
    Files.walk(Paths.get("$pathPrefix/fonts")).forEach {
        if (it.toFile().extension.toLowerCase() in arrayOf("otf", "ttf")) {
            val font = Font.createFont(Font.TRUETYPE_FONT, it.toFile())
            val name = it.toFile().name.split(".")[0]
            ge.registerFont(font)
            fonts.add(Pair(name, font))
        }
    }
}