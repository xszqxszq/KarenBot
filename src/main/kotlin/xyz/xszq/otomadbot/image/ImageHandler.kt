package xyz.xszq.otomadbot.image

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.AnimatedGifReader
import com.sksamuel.scrimage.nio.ImageIOReader
import com.sksamuel.scrimage.nio.ImageSource
import com.sksamuel.scrimage.nio.PngWriter
import com.soywiz.korim.awt.AwtNativeImage
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.format.readNativeImage
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.std.toVfs
import com.soywiz.korio.util.UUID
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.mamoe.mirai.console.permission.PermissionId
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.permission.PermitteeId.Companion.permitteeId
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.data.ImageType
import net.mamoe.mirai.message.data.MarketFace
import net.mamoe.mirai.message.data.MessageSource.Key.recall
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import top.mrxiaom.overflow.contact.RemoteBot
import xyz.xszq.OtomadBotCore
import xyz.xszq.events
import xyz.xszq.otomadbot.CommandModule
import xyz.xszq.otomadbot.Cooldown
import xyz.xszq.otomadbot.ImageCommand
import xyz.xszq.otomadbot.NetworkUtils
import xyz.xszq.otomadbot.admin.BadWordHandler
import xyz.xszq.otomadbot.api.PythonApi
import xyz.xszq.otomadbot.kotlin.isUrl
import xyz.xszq.otomadbot.kotlin.tempDir
import xyz.xszq.otomadbot.mirai.getTmpId
import xyz.xszq.otomadbot.mirai.queryUrl
import xyz.xszq.otomadbot.mirai.quoteReply
import xyz.xszq.otomadbot.text.AutoReplyHandler
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.Path
import kotlin.random.Random


class ReplyPicList {
    val list = HashMap<String, ArrayList<Path>>()
    val included = mutableListOf<String>()
    fun load(dir: String, target: String = dir) {
        if (!list.containsKey(target)) {
            list[target] = arrayListOf()
            included.add(target)
        }
        if (!OtomadBotCore.configFolder.resolve("image").exists())
            OtomadBotCore.configFolder.resolve("image").mkdir()
        if (!OtomadBotCore.configFolder.resolve("image/$dir").exists())
            OtomadBotCore.configFolder.resolve("image/$dir").mkdir()
        Files.walk(OtomadBotCore.configFolder.resolve("image/$dir").toPath(), 2)
            .filter { i -> Files.isRegularFile(i) }
            .forEach { path -> list[target]!!.add(path) }
    }
    fun getRandom(dir: String): File = list[dir]!![Random.nextInt(list[dir]!!.size)].toFile()
}

object ImageHandler: CommandModule("图片检测", "image.common") {
    val replyPic = ReplyPicList()
    val cooldown = Cooldown("image_common")
    val whitelist = PermissionService.INSTANCE.register(
            PermissionId("otm", "image.common.long.whitelist"), "龙图撤回白名单")
    override suspend fun subscribe() {
        events.subscribeAlways<GroupMessageEvent> { event ->
            val files = message.filterIsInstance<Image>().filter { it.imageType != ImageType.GIF }.mapNotNull {
                it.cacheOrDownload()
            }.toMutableList()
//            files.addAll(message.filterIsInstance<MarketFace>().mapNotNull {
//                it.cacheOrDownload()
//            })
            message.filterIsInstance<Image>().filter { it.imageType == ImageType.GIF }.mapNotNull {
                it.cacheOrDownload()
            }.forEach { gif ->
                tempDir.resolve("${gif.name}_1.png").let {
                    kotlin.runCatching {
                        AnimatedGifReader.read(ImageSource.of(gif)).frames.first().output(PngWriter.MinCompression, it)
                        files.add(it)
                    }.onFailure {
                        it.printStackTrace()
                    }
                }
            }
            if (files.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    longDetect.checkAndRun(event, files)
                }
//                withContext(Dispatchers.IO) {
//                    qrScan.checkAndRun(event, files)
//                }
                withContext(Dispatchers.IO) {
                    blondeDetect.checkAndRun(event, files)
                }
                withContext(Dispatchers.IO) {
                    AutoReplyHandler.replyPic.checkAndRun(event, files)
                }
                withContext(Dispatchers.IO) {
                    BadWordHandler.image.checkAndRun(event, files)
                }
            }
        }
    }
    val longDetect = ImageCommand("龙图检测", "long", false) { img ->
        img!!.forEach { image ->
            kotlin.runCatching {
                if (PythonApi.isLt(image)!!) {
                    if (group.botAsMember.isOperator() && !group.permitteeId.hasPermission(whitelist) && !sender.isOperator()) {
                        message.recall()
                    } else {
                        quoteReply("我超，龙")
                    }
                    return@ImageCommand
                }
            }
        }
    }
    val blondeDetect = ImageCommand("黄发检测", "blonde") { img ->
        if (cooldown.isReady(subject)) {
            img!!.forEach { image ->
                kotlin.runCatching {
                    if (PythonApi.isBlonde(image)!!) {
                        delay(Random.nextLong(400L, 800L))
                        replyPic.getRandom("reply").toExternalResource().use {
                            subject.sendMessage(it.uploadAsImage(subject))
                        }
                        cooldown.update(subject)
                        return@ImageCommand
                    }
                }
            }
        }
    }
//    val qrScan = ImageCommand("二维码扫描", "qrscan", false) { img ->
//        kotlin.runCatching {
//            val list = img ?.map { it.decodeQR() } ?.filter { it.isUrl() }
//            if (list ?.isNotEmpty() == true)
//                quoteReply(list.joinToString("\n"))
//        }
//    }
}
suspend fun Image.cacheOrDownload(): File? {
//    val imgFile = tempDir.resolve(getRealId())
//    if (imgFile.exists())
//        return imgFile
    val headers: List<Pair<String, String>> =
        if (OtomadBotCore.cookies.isBlank()) listOf()
        else listOf(Pair("Cookie", OtomadBotCore.cookies))
    return NetworkUtils.downloadTempFile(queryUrl(), headers)
}
suspend fun MarketFace.cacheOrDownload(): File? {
    val imgFile = tempDir.resolve(getTmpId())
    if (imgFile.exists())
        return imgFile
    return NetworkUtils.downloadFile(queryUrl(), tempDir, getTmpId())
}
suspend fun File.readAsImage() = toVfs().readAsImage()
suspend fun VfsFile.readAsImage(): Bitmap {
    return try {
        readNativeImage()
    } catch (e: Exception) {
        AwtNativeImage(ImmutableImage.loader().fromPath(Path(absolutePath)).awt())
    }
}

val qrDecodeHint = buildMap {
    put(DecodeHintType.CHARACTER_SET, "UTF-8")
}

suspend fun File.decodeQR(): String = withContext(Dispatchers.IO) {
    HybridBinarizer(BufferedImageLuminanceSource(ImageIOReader().read(readBytes()).awt())).let { binarizer ->
        return@withContext QRCodeReader().decode(BinaryBitmap(binarizer), qrDecodeHint).text
    }
}

suspend fun BufferedImage.toInputStream(): InputStream? = withContext(Dispatchers.IO) {
    val stream = ByteArrayOutputStream()
    return@withContext try {
        ImageIO.write(this@toInputStream, "png", stream)
        ByteArrayInputStream(stream.toByteArray())
    } catch (e: Exception) {
        null
    }
}