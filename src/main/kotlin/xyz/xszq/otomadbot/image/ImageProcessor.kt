package xyz.xszq.otomadbot.image

import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.encode
import com.soywiz.korio.file.std.toVfs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.anyIsInstance
import net.mamoe.mirai.message.data.buildMessageChain
import net.mamoe.mirai.message.nextMessage
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import org.opencv.calib3d.Calib3d.undistort
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc.resize
import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula
import xyz.xszq.events
import xyz.xszq.otomadbot.*
import xyz.xszq.otomadbot.NetworkUtils.getFile
import xyz.xszq.otomadbot.kotlin.generateQR
import xyz.xszq.otomadbot.kotlin.newTempFile
import xyz.xszq.otomadbot.mirai.quoteReply
import xyz.xszq.otomadbot.mirai.startsWithSimple
import java.awt.Color
import kotlin.math.min

object ImageProcessor: CommandModule("图像处理", "image.process") {
    private val cooldown = Cooldown("image_processor")
    private val quota = Quota("image_effect")
    private const val quotaExceededMessage = "今日该功能限额已经用完了哦~"
    override suspend fun subscribe() {
        events.subscribeMessages {
            startsWithSimple("生成二维码", true) { _, text ->
                ifReady(cooldown) {
                    withContext(Dispatchers.IO) {
                        generateQR.checkAndRun(CommandEvent(listOf(text), this@startsWithSimple))
                    }
                }
            }
            startsWithSimple("生成latex", true) { _, text ->
                ifReady(cooldown) {
                    withContext(Dispatchers.IO) {
                        generateLatex.checkAndRun(CommandEvent(listOf(text), this@startsWithSimple))
                    }
                }
            }
            startsWithSimple("我巨爽") { _, _ ->
                if (available(quota)) {
                    withContext(Dispatchers.IO) {
                        flipImage.checkAndRun(this@startsWithSimple)
                    }
                } else {
                    quoteReply(quotaExceededMessage)
                }
            }
            startsWithSimple("球面化") { _, _ ->
                if (available(quota)) {
                    withContext(Dispatchers.IO) {
                        spherizeImage.checkAndRun(this@startsWithSimple)
                    }
                } else {
                    quoteReply(quotaExceededMessage)
                }
            }
            startsWithSimple("反球面化") { _, _ ->
                if (available(quota)) {
                    withContext(Dispatchers.IO) {
                        revSpherizeImage.checkAndRun(this@startsWithSimple)
                    }
                } else {
                    quoteReply(quotaExceededMessage)
                }
            }
        }
    }


    val spDistCoeffs = Mat(4, 1, CvType.CV_32F).apply {
        put(0, 0, 0.6)
    }
    val spCam = Mat(3, 3, CvType.CV_32F).apply {
        put(0, 2, 250.0)
        put(1, 2, 250.0)
        put(0, 0, 100.0)
        put(1, 1, 100.0)
        put(2, 2, 1.0)
    }
    val revSpDistCoeffs = Mat(4, 1, CvType.CV_32F).apply {
        put(0, 0, -0.01)
    }
    val revSpCam = Mat(3, 3, CvType.CV_32F).apply {
        put(0, 2, 250.0)
        put(1, 2, 250.0)
        put(0, 0, 50.0)
        put(1, 1, 50.0)
        put(2, 2, 1.0)
    }

    val generateQR = CommonCommandWithArgs("生成二维码", "generate_qr") {
        args.first().generateQR()?.toInputStream()?.toExternalResource()?.use {
            event.quoteReply(event.subject.uploadImage(it))
        }
        event.update(cooldown)
    }
    val generateLatex = CommonCommandWithArgs("生成Latex", "generate_latex") {
        val text = args.first()
        val formula = TeXFormula(text)
        val result = newTempFile()
        formula.createPNG(
            TeXConstants.STYLE_DISPLAY, 22.0F, result.absolutePath,
            Color.WHITE, Color.BLACK)
        result.toExternalResource().use {
            event.quoteReply(event.subject.uploadImage(it))
        }
        result.delete()
        event.update(cooldown)
    }
    val flipImage = CommonCommand("我巨爽", "flip") {
        val target = if (message.anyIsInstance<Image>()) {
            message
        } else {
            quoteReply("请发送欲水平翻转的图片（需取消请发送不带图片的消息）：")
            nextMessage()
        }
        target.filterIsInstance<Image>().forEach { miraiImg ->
            val file = miraiImg.getFile()!!
            val img = Imgcodecs.imread(file.absolutePath)
            val flipped = Mat()
            Core.flip(img, flipped, 1)

            val resultA = img.clone()
            val resultB = img.clone()


            flipped.submat(
                0, img.rows() - 1, img.cols() / 2, img.cols() - 1
            ).copyTo(
                resultA.submat(
                    0, img.rows() - 1, img.cols() / 2, img.cols() - 1
                ))
            flipped.submat(
                0, img.rows() - 1, 0, img.cols() / 2
            ).copyTo(
                resultB.submat(
                    0, img.rows() - 1, 0, img.cols() / 2
                )
            )
            quoteReply(buildMessageChain {
                listOf(resultA, resultB).map {
                    it.saveToByteArray().toExternalResource().use { r ->
                        r.uploadAsImage(subject)
                    }
                }.forEach { add(it) }
            })
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
            val file = miraiImg.getFile()!!
            val img = file.toVfs().readAsImage().encode(PNG).toMat().square()
            val result = Mat(img.rows(), img.cols(), img.type())
            undistort(img, result, spCam, spDistCoeffs)
            result.submat(148, 352, 148, 352).clone().saveToByteArray()
                .toExternalResource().use {
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
            quoteReply("请发送欲反球面化的图片（需取消请发送不带图片的消息）：")
            nextMessage()
        }
        target.filterIsInstance<Image>().forEach { miraiImg ->
            val file = miraiImg.getFile()!!
            var img = Imgcodecs.imread(file.absolutePath).square()
            val ext = Mat(img.rows() + 50, img.cols() + 50, img.type())
            img.copyTo(ext.submat(25, 525, 25, 525))
            img = ext.clone()
            val result = Mat(img.rows(), img.cols(), img.type())
            undistort(img, result, revSpCam, revSpDistCoeffs)
            result.saveToByteArray()
                .toExternalResource().use {
                    quoteReply(it.uploadAsImage(subject))
                }
            file.delete()
        }
        if (target.anyIsInstance<Image>())
            quota.update(subject)
    }
}

fun Mat.square(size: Int = 500): Mat {
    val target = min(rows(), cols())
    val startR = (rows() - target) / 2
    val startC = (cols() - target) / 2
    val result = Mat(rows(), cols(), type())
    resize(submat(startR, startR + target, startC, startC + target), result,
        Size(size.toDouble(), size.toDouble()))
    return result
}

fun ByteArray.toMat(): Mat {
    val b = MatOfByte()
    b.fromList(this.toMutableList())
    return Imgcodecs.imdecode(b, Imgcodecs.IMREAD_UNCHANGED)
}

fun Mat.saveToByteArray(ext: String = ".png"): ByteArray {
    val result = MatOfByte()
    Imgcodecs.imencode(ext, this, result)
    return result.toArray()
}

