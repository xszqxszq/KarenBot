package tk.xszq.otomadbot.text

import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.sendTo
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import tk.xszq.otomadbot.*
import tk.xszq.otomadbot.image.ImageCommonHandler

object ForwardMessageConstructor: EventHandler("forward", "转发消息伪造",
    HandlerType.DEFAULT_DISABLED) {
    override fun register() {
        GlobalEventChannel.subscribeMessages {
            equalsTo("伪造消息", true) {
                requireBotAdmin {
                    val admin = bot.friends.find { it.id == 943551369L }
                    ForwardMessageBuilder(admin as Contact).add(admin, PlainText("假转发消息测试")
                        ).add(admin, ImageCommonHandler.replyPic.getRandom("reply").uploadAsImage(subject))
                        .build().sendTo(subject)
                    pass
                }
            }
        }
        super.register()
    }
}