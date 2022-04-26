package xyz.xszq.otomadbot.image

import net.mamoe.mirai.contact.Contact.Companion.uploadImage
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula
import xyz.xszq.otomadbot.*
import xyz.xszq.otomadbot.core.Cooldown
import xyz.xszq.otomadbot.core.ifReady
import xyz.xszq.otomadbot.core.remaining
import xyz.xszq.otomadbot.core.update
import java.awt.Color

object ImageGeneratorHandler: EventHandler("生成图像", "image.generate") {
    private val cooldown = Cooldown("image_generate")
    override fun register() {
        GlobalEventChannel.subscribeGroupMessages {
            startsWithSimple("生成二维码", true) { _, text ->
                requireNot(denied) {
                    ifReady(cooldown) {
                        update(cooldown)
                        generateQR(text, this)
                    } ?: run {
                        quoteReply("为防止刷屏，请等待 ${remaining(cooldown)} 秒再试")
                    }
                }
            }
            startsWithSimple("生成latex", true) { _, text ->
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
        }
        super.register()
    }
    private suspend fun generateQR(text: String, event: MessageEvent) = event.run {
        quoteReply(subject.uploadImage(text.generateQR()!!.toInputStream()!!))
    }
}