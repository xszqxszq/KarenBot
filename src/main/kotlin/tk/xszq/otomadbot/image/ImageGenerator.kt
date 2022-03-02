package tk.xszq.otomadbot.image

import net.mamoe.mirai.contact.Contact.Companion.uploadImage
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula
import tk.xszq.otomadbot.*
import tk.xszq.otomadbot.api.FiveThousandChoyenApi
import tk.xszq.otomadbot.core.Cooldown
import tk.xszq.otomadbot.core.ifReady
import tk.xszq.otomadbot.core.remaining
import tk.xszq.otomadbot.core.update
import java.awt.Color

object ImageGeneratorHandler: EventHandler("生成图像", "image.generate") {
    private val cooldown = Cooldown("image_generate")
    override fun register() {
        GlobalEventChannel.subscribeGroupMessages {
            startsWithSimple("生成二维码", true) { text, _ ->
                requireNot(denied) {
                    ifReady(cooldown) {
                        update(cooldown)
                        generateQR(text, this)
                    } ?: run {
                        quoteReply("为防止刷屏，请等待 ${remaining(cooldown)} 秒再试")
                    }
                }
            }
            startsWithSimple("生成latex", true) { text, _ ->
                requireNot(denied) {
                    ifReady(cooldown) {
                        update(cooldown)
                        val formula = TeXFormula(text)
                        val result = newTempFile()
                        formula.createPNG(TeXConstants.STYLE_DISPLAY, 22.0F, result.absolutePath,
                            Color.WHITE, Color.BLACK)
                        result.toExternalResource().use {
                            quoteReply(subject.uploadImage(it))
                        }
                        result.delete()
                    } ?: run {
                        quoteReply("为防止刷屏，请等待 ${remaining(cooldown)} 秒再试")
                    }
                }
            }
            startsWithSimple("生成5k", true) { _, raw ->
                requireNot(denied) {
                    ifReady(cooldown) {
                        update(cooldown)
                        val args = raw.toArgsListByLn()
                        when (args.size) {
                            0 -> quoteReply("使用方法：\n生成5k 第一行文本\n第二行文本（可选）")
                            1 -> quoteReply(FiveThousandChoyenApi.generate(args.first().trim(), " ")
                                .toExternalResource().use {
                                    it.uploadAsImage(subject)
                                })
                            else -> quoteReply(FiveThousandChoyenApi.generate(args[0].trim(), args[1].trim())
                                .toExternalResource().use {
                                    it.uploadAsImage(subject)
                                })
                        }
                    } ?: run {
                        quoteReply("为防止刷屏，请等待 ${remaining(cooldown)} 秒再试")
                    }
                }
            }
        }
        super.register()
    }
    private suspend fun generateQR(text: String, event: MessageEvent) = event.run {
        quoteReply(subject.uploadImage(text.generateQR()!!.toInputStream()!!))
    }
}