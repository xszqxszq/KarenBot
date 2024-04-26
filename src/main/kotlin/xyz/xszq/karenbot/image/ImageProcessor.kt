package xyz.xszq.karenbot.image

import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.encode
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.anyIsInstance
import net.mamoe.mirai.message.data.buildMessageChain
import net.mamoe.mirai.message.nextMessage
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula
import xyz.xszq.events
import xyz.xszq.karenbot.*
import xyz.xszq.karenbot.kotlin.generateQR
import xyz.xszq.karenbot.kotlin.newTempFile
import xyz.xszq.karenbot.kotlin.substringAfterPrefix
import xyz.xszq.karenbot.kotlin.toArgsList
import xyz.xszq.karenbot.mirai.quoteReply
import xyz.xszq.karenbot.mirai.reply
import xyz.xszq.karenbot.mirai.startsWithSimple
import java.awt.Color
import kotlin.math.*

object ImageProcessor: CommandModule("生成功能", "image.process") {
    private val cooldown = Cooldown("image_processor")
    private val quota = Quota("image_effect")
    private val memeList = mutableListOf<Meme>()
    private const val quotaExceededMessage = "今日该功能限额已经用完了哦~"
    private val json = Json {
        prettyPrint = true; isLenient = true; ignoreUnknownKeys = true
    }
    val disabledList = listOf("5000choyen", "acg_entrance", "bluearchive", "china_flag")
    private val client = HttpClient {
        install(HttpTimeout) {
            socketTimeoutMillis = 10000
            requestTimeoutMillis = 10000
            connectTimeoutMillis = 10000
        }
        install(ContentNegotiation) {
            json(json)
        }
    }
    suspend fun reload() {
        updateMemes()
    }
    suspend fun updateMemes() {
        memeList.clear()
        val keys = client.get("${MemeConfig.data.url}/memes/keys").body<List<String>>()
        keys.forEach { key ->
            if (key !in disabledList) {
                val info = client.get("${MemeConfig.data.url}/memes/$key/info").body<Meme>()
                memeList.add(info)
            }
        }
    }
    override suspend fun subscribe() {
        events.subscribeGroupMessages {
//            startsWithSimple("二维码", true) { _, text ->
//                ifReady(cooldown) {
//                    generateQR.checkAndRun(this@startsWithSimple, text)
//                }
//            }
            startsWithSimple("latex", true) { _, text ->
                ifReady(cooldown) {
                    generateLatex.checkAndRun(this@startsWithSimple, text)
                }
            }
            startsWithSimple("我巨爽") { _, _ ->
                if (available(quota)) {
                    flipImage.checkAndRun(this@startsWithSimple)
                } else {
                    quoteReply(quotaExceededMessage)
                }
            }
            startsWithSimple("球面化") { _, _ ->
                if (available(quota)) {
                    spherizeImage.checkAndRun(this@startsWithSimple)
                } else {
                    quoteReply(quotaExceededMessage)
                }
            }
            startsWithSimple("反球面化") { _, _ ->
                if (available(quota)) {
                    revSpherizeImage.checkAndRun(this@startsWithSimple)
                } else {
                    quoteReply(quotaExceededMessage)
                }
            }
            always {
                meme.checkAndRun(this)
            }
        }
    }

    val generateQR = CommonCommandWithArg("二维码", "generate_qr") { text ->
        text?.generateQR()?.toInputStream()?.toExternalResource()?.use {
            quoteReply(subject.uploadImage(it))
            update(cooldown)
        }
    }
    val generateLatex = CommonCommandWithArg("latex", "generate_latex") { text ->
        newTempFile().let { result ->
            TeXFormula(text).createPNG(
                TeXConstants.STYLE_DISPLAY, 22.0F, result.absolutePath,
                Color.WHITE, Color.BLACK)
            result.toExternalResource().use {
                quoteReply(subject.uploadImage(it))
            }
            result.delete()
        }
        update(cooldown)
    }
    val flipImage = CommonCommand("我巨爽", "flip") {
        val target = if (message.anyIsInstance<Image>()) {
            message
        } else {
            quoteReply("请发送欲水平翻转的图片（需取消请发送不带图片的消息）：")
            nextMessage()
        }
        target.filterIsInstance<Image>().forEach { miraiImg ->
            val file = miraiImg.cacheOrDownload()!!
            val img = file.readAsImage()
            val flipped = img.clone().flipX()

            val resultA = img.clone()
            val resultB = img.clone()

            flipped.copy(0, 0, resultA, 0, 0, img.width / 2, img.height)
            flipped.copy(img.width - img.width / 2, 0, resultB, img.width - img.width / 2, 0, img.width / 2, img.height)
            quoteReply(buildMessageChain {
                listOf(resultA, resultB).map {
                    it.encode(PNG).toExternalResource().use { r ->
                        r.uploadAsImage(subject)
                    }
                }.forEach { add(it) }
            })
            file.delete()
        }
        if (target.anyIsInstance<Image>())
            quota.update(subject)
    }
    val spherizeImage = CommonCommand("球面化", "spherize") {
        val target = if (message.anyIsInstance<Image>()) {
            message
        } else {
            quoteReply("请发送欲球面化的图片（需取消请发送不带图片的消息）：")
            nextMessage()
        }
        target.filterIsInstance<Image>().forEach { miraiImg ->
            val file = miraiImg.cacheOrDownload()!!
            val img = file.readAsImage()
            spherize(img.toBMP32()).toExternalResource().use {
                quoteReply(it.uploadAsImage(subject))
            }
            file.delete()
        }
        if (target.anyIsInstance<Image>())
            quota.update(subject)
    }
    val revSpherizeImage = CommonCommand("反球面化", "rev_spherize") {
        val target = if (message.anyIsInstance<Image>()) {
            message
        } else {
            quoteReply("请发送欲球面化的图片（需取消请发送不带图片的消息）：")
            nextMessage()
        }
        target.filterIsInstance<Image>().forEach { miraiImg ->
            val file = miraiImg.cacheOrDownload()!!
            val img = file.readAsImage()
            pincushion(img.toBMP32()).toExternalResource().use {
                quoteReply(it.uploadAsImage(subject))
            }
            file.delete()
        }
        if (target.anyIsInstance<Image>())
            quota.update(subject)
    }
    val meme = CommonCommand("表情包生成", "meme") {
        val raw: String
        val rawArgs: List<String>
        if (message.filterIsInstance<PlainText>().joinToString("").startsWith("生成")) {
            raw = message.filterIsInstance<PlainText>().joinToString("").substringAfterPrefix("生成")
            rawArgs = raw.toArgsList()
            if (rawArgs.isEmpty()) {
                reply(buildString {
                    appendLine("使用方法：“生成 表情包名称 图片或文本参数”")
                    appendLine("\t例：生成 喜报 NullPointerException")
                    appendLine()
                    appendLine("支持的表情包列表：https://otmdb.cn/karenbot/meme")
                }.trim())
                return@CommonCommand
            }
        } else {
            return@CommonCommand
        }
        var args = listOf<String>()
        val target = memeList.find {
            if (it.patterns.isNotEmpty()) {
                val groupValues = Regex(it.patterns.first()).find(raw)?.groupValues
                if (groupValues?.isNotEmpty() == true) {
                    args = groupValues
                    true
                } else {
                    false
                }
            } else {
                if (rawArgs[0] in it.keywords) {
                    args = rawArgs.subList(1, rawArgs.size)
                    true
                } else {
                    false
                }
            }
        } ?: run {
            reply("表情包不存在。\n支持的表情包列表：https://otmdb.cn/karenbot/meme")
            return@CommonCommand
        }


        val images = message.filterIsInstance<Image>()

        if (images.size < target.params.minImages) {
            reply("图片不足，此模板${getImageRequirements(target)}作为输入。")
            return@CommonCommand
        }
        if (args.size < target.params.minTexts) {
            reply("文本不足，此模板${getTextRequirements(target)}作为输入。")
            return@CommonCommand
        }

        val localImages = images.mapNotNull { it.cacheOrDownload() }

        val parts: List<PartData> = formData {
            args.forEach {
                append("texts", it)
            }
            localImages.forEach {
                append("images", it.readBytes(), Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=${it.name}")
                })
            }
        }
        val response = client.submitFormWithBinaryData(
            url = "${MemeConfig.data.url}/memes/${target.key}/",
            formData = parts
        )
        if (response.status == HttpStatusCode.OK) {
            response.body<ByteArray>().toExternalResource().use {
                reply(subject.uploadImage(it))
            }
        } else if (response.status.value == 542) {
            reply("此模板${getImageRequirements(target)}且${getTextRequirements(target)}，请检查输入")
        } else {
            println(response)
            println(response.bodyAsText())
        }
    }
    fun getImageRequirements(target: Meme): String {
        if (target.params.minImages == target.params.maxImages) {
            if (target.params.minImages == 0)
                return "不需要图片"
            return "需要${target.params.minImages}张图片"
        }
        return "需要${target.params.minImages}~${target.params.maxImages}张图片"
    }
    fun getTextRequirements(target: Meme): String {
        if (target.params.minTexts == target.params.maxTexts) {
            if (target.params.minTexts == 0)
                return "不需要文本"
            return "需要${target.params.minTexts}段文本"
        }
        return "需要${target.params.minTexts}~${target.params.maxTexts}段文本"
    }

    /**
     * Apply a Spherize Filter on the image.
     * @param a: Affects only the outermost pixels of the image
     * @param b: Amount of the effect
     * @param c: Most uniform correction
     * Reference: https://stackoverflow.com/questions/12620025/barrel-distortion-correction-algorithm-to-correct-fisheye-lens-failing-to-impl
     */
    suspend fun spherize(img: Bitmap32, a: Double = 1.0, b: Double = 3.0, c: Double = -9.0): ByteArray {
        val d = 1.0 - a - b - c
        val radius = min(img.width, img.height) / 2
        val result = Bitmap32(img.width, img.height) { x, y ->
            val midX = (img.width - 1) / 2.0
            val midY = (img.height - 1) / 2.0
            val dX = x - midX
            val dY = y - midY
            val dstR = sqrt((dX * dX + dY * dY) / radius / radius)
            val factor = abs(1.0 / (a * dstR * dstR * dstR + b * dstR * dstR + c * dstR + d))
            val srcX = (midX + dX * factor).toInt()
            val srcY = (midY + dY * factor).toInt()
            img[srcX, srcY]
        }
        return result.encode(PNG)
    }
    suspend fun pincushion(img: Bitmap32, strength: Double = 7.0, zoom: Double = 1.5): ByteArray {
        val midW = img.width / 2.0
        val midH = img.height / 2.0
        val correctionRadius = sqrt(img.width.toDouble().pow(2) + img.height.toDouble().pow(2)) / strength
        return Bitmap32(img.width, img.height) { x, y ->
            val newX = x - midW
            val newY = y - midH
            val dis = sqrt(newX.pow(2) + newY.pow(2))
            val r = dis / correctionRadius
            val theta = if (r == 0.0) 1.0 else atan(r) / r
            val srcX = midW + theta * newX * zoom
            val srcY = midH + theta * newY * zoom
            img[srcX.toInt(), srcY.toInt()]
        }.encode(PNG)
    }
}