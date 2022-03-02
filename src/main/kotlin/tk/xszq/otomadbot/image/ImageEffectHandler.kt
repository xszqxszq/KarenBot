package tk.xszq.otomadbot.image

import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.bitmap.flippedX
import com.soywiz.korim.bitmap.resized
import com.soywiz.korim.bitmap.sliceWithSize
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.encode
import com.soywiz.korim.format.readBitmap
import com.soywiz.korim.format.readNativeImage
import com.soywiz.korio.file.std.toVfs
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.ScaleMode
import com.soywiz.krypto.encoding.Base64
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
import tk.xszq.otomadbot.api.PythonApi
import tk.xszq.otomadbot.core.Cooldown
import tk.xszq.otomadbot.core.ifReady
import tk.xszq.otomadbot.core.remaining
import tk.xszq.otomadbot.core.update
import kotlin.math.roundToInt


object ImageEffectHandler: EventHandler("图像滤镜", "image.effect") {
    private val cooldown = Cooldown("image_effect")
    private const val resizeWidth = 300
    override fun register() {
        GlobalEventChannel.subscribeGroupMessages {
            startsWithSimple("球面化") { arg, _ ->
                ifReady(cooldown) {
                    requireNot(denied) {
                        if (message.anyIsInstance<Image>() || arg == "") {
                            update(cooldown)
                            val target = if (message.anyIsInstance<Image>()) {
                                message
                            } else {
                                quoteReply("请发送欲球面化的图片（需取消请发送不带图片的消息）：")
                                nextMessage()
                            }
                            target.filterIsInstance<Image>().forEach {
                                val file = it.getFile()!!
                                val input = Base64.encode(file.readBytes())
                                PythonApi.spherize(input)!!.toExternalResource().use { exr ->
                                    subject.sendMessage(target.quote() + exr.uploadAsImage(subject))
                                }
                                file.delete()
                            }
                        }
                    } ?: pass
                } ?: run {
                    quoteReply("你又在玩球面化喔，休息" + remaining(cooldown) + "秒好不好")
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
                                val bitmap = file.toVfs().readNativeImage()
                                val input = bitmap.resized(resizeWidth,
                                    (1.0 * bitmap.height * resizeWidth / bitmap.width).roundToInt(),
                                    ScaleMode.EXACT, Anchor.CENTER)
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
        }
        super.register()
    }
}
