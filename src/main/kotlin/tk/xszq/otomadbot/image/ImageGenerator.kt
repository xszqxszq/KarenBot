package tk.xszq.otomadbot.image

import net.mamoe.mirai.contact.Contact.Companion.uploadImage
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula
import tk.xszq.otomadbot.*
import java.awt.Color

object ImageGeneratorHandler: EventHandler("生成图像", "image.generate") {
    override fun register() {
        GlobalEventChannel.subscribeMessages {
            startsWithSimple("生成二维码", true) { text, _ ->
                requireNot(denied) {
                    generateQR(text, this)
                }
            }
            startsWithSimple("生成latex", true) { text, _ ->
                val formula = TeXFormula(text)
                val result = newTempFile()
                formula.createPNG(TeXConstants.STYLE_DISPLAY, 22.0F, result.absolutePath, Color.WHITE, Color.BLACK)
                result.toExternalResource().use {
                    quoteReply(subject.uploadImage(it))
                }
                result.delete()
            }
        }
        super.register()
    }
    private suspend fun generateQR(text: String, event: MessageEvent) = event.run {
        quoteReply(subject.uploadImage(text.generateQR()!!.toInputStream()!!))
    }
}