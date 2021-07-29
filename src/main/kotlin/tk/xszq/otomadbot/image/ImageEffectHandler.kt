package tk.xszq.otomadbot.image

import com.soywiz.korim.bitmap.resized
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.encode
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.file.std.toVfs
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.ScaleMode
import com.soywiz.krypto.encoding.Base64
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.message.data.anyIsInstance
import net.mamoe.mirai.message.nextMessage
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import tk.xszq.otomadbot.*
import tk.xszq.otomadbot.NetworkUtils.getFile
import tk.xszq.otomadbot.api.PythonApi
import kotlin.math.roundToInt

object ImageEffectHandler: EventHandler("图像滤镜", "image.effect") {
    private const val resizeWidth = 300
    override fun register() {
        GlobalEventChannel.subscribeMessages {
            startsWithSimple("球面化") { arg, _ ->
                requireNot(denied) {
                    if (message.anyIsInstance<Image>() || arg == "") {
                        val target = if (message.anyIsInstance<Image>()) {
                            message
                        } else {
                            quoteReply("请发送欲球面化的图片（需取消请发送不带图片的消息）：")
                            nextMessage()
                        }
                        target.filterIsInstance<Image>().forEach {
                            val file = it.getFile()!!
                            val bitmap = file.toVfs().readBitmap()
                            val input = Base64.encode(bitmap.resized(resizeWidth,
                                (1.0 * bitmap.height * resizeWidth / bitmap.width).roundToInt(),
                                ScaleMode.EXACT, Anchor.CENTER).encode(PNG))
                            PythonApi.spherize(input)!!.toExternalResource().use { exr ->
                                subject.sendMessage(target.quote() + exr.uploadAsImage(subject))
                            }
                            file.delete()
                        }
                    }
                }
            }
        }
        super.register()
    }
}