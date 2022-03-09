@file:Suppress("MemberVisibilityCanBePrivate")

package tk.xszq.otomadbot.image

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.encode
import com.soywiz.korim.format.readNativeImage
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.std.toVfs
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.ScaleMode
import kotlinx.coroutines.Dispatchers
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.message.data.anyIsInstance
import net.mamoe.mirai.message.nextMessage
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import tk.xszq.otomadbot.*
import tk.xszq.otomadbot.NetworkUtils.getFile
import tk.xszq.otomadbot.core.Cooldown
import tk.xszq.otomadbot.core.ifReady
import tk.xszq.otomadbot.core.remaining
import tk.xszq.otomadbot.core.update
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt


object ImageEffectHandler: EventHandler("图像滤镜", "image.effect") {
    private val cooldown = Cooldown("image_effect")
    private const val resizeWidth = 700
    override fun register() {
        GlobalEventChannel.subscribeGroupMessages {
            startsWithSimple("球面化") { _, _ ->
                ifReady(cooldown) {
                    requireNot(denied) {
                        update(cooldown)
                        val target = if (message.anyIsInstance<Image>()) {
                            message
                        } else {
                            quoteReply("请发送欲球面化的图片（需取消请发送不带图片的消息）：")
                            nextMessage()
                        }
                        target.filterIsInstance<Image>().forEach {
                            val file = it.getFile()!!
                            var input: Bitmap = file.toVfs().readNativeImage()
                            if (input.width > 900)
                                input = input.resizeToW(resizeWidth)
                            spherize(input.toBMP32())
                                .toExternalResource().use { exr ->
                                    subject.sendMessage(target.quote() + exr.uploadAsImage(subject))
                                }
                            file.delete()
                        }
                    } ?: pass
                } ?: run {
                    quoteReply("你又在玩球面化喔，休息" + remaining(cooldown) + "秒好不好")
                }
            }
            startsWithSimple("反球面化") { _, _ ->
                ifReady(cooldown) {
                    requireNot(denied) {
                        update(cooldown)
                        val target = if (message.anyIsInstance<Image>()) {
                            message
                        } else {
                            quoteReply("请发送欲反球面化的图片（需取消请发送不带图片的消息）：")
                            nextMessage()
                        }
                        target.filterIsInstance<Image>().forEach {
                            val file = it.getFile()!!
                            var input: Bitmap = file.toVfs().readNativeImage()
                            if (input.width > 900)
                                input = input.resizeToW(resizeWidth)
                            spherize(input.toBMP32(),
                                0.32, 3.0, -9.0, true).toExternalResource().use { exr ->
                                    subject.sendMessage(target.quote() + exr.uploadAsImage(subject))
                                }
                            file.delete()
                        }
                    } ?: pass
                } ?: run {
                    quoteReply("你又在玩反球面化喔，休息" + remaining(cooldown) + "秒好不好")
                }
            }
            startsWithSimple("我巨爽") { arg, _ ->
                ifReady(cooldown) {
                    requireNot(denied) {
                        if (message.anyIsInstance<Image>() || arg == "") {
                            update(cooldown)
                            val target = if (message.anyIsInstance<Image>()) {
                                message
                            } else {
                                quoteReply("请发送欲水平翻转的图片（需取消请发送不带图片的消息）：")
                                nextMessage()
                            }
                            target.filterIsInstance<Image>().forEach {
                                val file = it.getFile()!!
                                val input = file.toVfs().readNativeImage().resizeToW(resizeWidth)
                                val resultLeft = input.clone().context2d {
                                    drawImage(input.sliceWithSize(0, 0, input.width / 2, input.height)
                                        .extract().flipX(), input.width / 2 + 1, 0)
                                    dispose()
                                }.encode(PNG)
                                val resultRight = input.clone().context2d {
                                    drawImage(input.sliceWithSize(input.width / 2 + 1, 0,
                                        input.width - input.width / 2, input.height)
                                        .extract().flipX(), 0, 0)
                                    dispose()
                                }.encode(PNG)
                                resultLeft.toExternalResource().use { first ->
                                    resultRight.toExternalResource().use { second ->
                                        subject.sendMessage(target.quote() + first.uploadAsImage(subject)
                                                + second.uploadAsImage(subject))
                                    }
                                }
                                file.delete()
                            }
                        }
                    } ?: pass
                } ?: run {
                    quoteReply("又在玩我巨爽喔，休息" + remaining(cooldown) + "秒好不好")
                }
            }
            startsWith("/sp") { raw ->
                requireBotAdmin {
                    val args = raw.toArgsList()
                    quoteReply("请发送欲球面化的图片（需取消请发送不带图片的消息）：")
                    val target = nextMessage()
                    target.filterIsInstance<Image>().forEach {
                        val file = it.getFile()!!
                        spherize(file.toVfs().readNativeImage().toBMP32(),
                            args[0].toDouble(), args[1].toDouble(), args[2].toDouble(), true)
                            .toExternalResource().use { exr ->
                                subject.sendMessage(target.quote() + exr.uploadAsImage(subject))
                            }
                        file.delete()
                    }
                }
            }
        }
        super.register()
    }
    /**
     * Apply a Spherize Filter on the image.
     * @param a: Affects only the outermost pixels of the image
     * @param b: Amount of the effect
     * @param c: Most uniform correction
     * Reference: https://stackoverflow.com/questions/12620025/barrel-distortion-correction-algorithm-to-correct-fisheye-lens-failing-to-impl
     */
    suspend fun spherize(img: Bitmap32, a: Double = 1.0, b: Double = 3.0, c: Double = -9.0,
                         flipped: Boolean = false): ByteArray {
        val result = Bitmap32(img.width, img.height)
        val d = 1.0 - a - b - c
        val radius = min(result.width, result.height) / 2
        result.forEach { _, x, y ->
            val midX = (result.width - 1) / 2.0
            val midY = (result.height - 1) / 2.0
            val dX = x - midX
            val dY = y - midY
            val dstR = sqrt((dX * dX + dY * dY) / radius / radius)
            val factor = abs(1.0 / (a * dstR * dstR * dstR + b * dstR * dstR + c * dstR + d))
            val srcX = (midX + dX * factor).toInt()
            val srcY = (midY + dY * factor).toInt()
            if (result.inside(srcX, srcY)) {
                if (flipped) result.fillNear(srcX, srcY, img[x, y])
                else result[x, y] = img[srcX, srcY]
            }
        }
        return result.encode(PNG)
    }
}
fun Bitmap.resizeToW(w: Int) = resized(w, (1.0 * height * w / width).roundToInt(), ScaleMode.EXACT, Anchor.CENTER)
fun Bitmap32.fillNear(tx: Int, ty: Int, content: RGBA) {
    val lst = listOf(-1, 0, 1)
    lst.fastForEach { x ->
        lst.fastForEach { y ->
            launchImmediately(Dispatchers.Default) {
                if (inside(tx + x, ty + y)) {
                    if (this[tx + x, ty + y] == RGBA(0, 0, 0, 0))
                        this[tx + x, ty + y] = content
                    else
                        this[tx + x, ty + y] = this[tx + x, ty + y].mix(content)
                }
            }
        }
    }
}