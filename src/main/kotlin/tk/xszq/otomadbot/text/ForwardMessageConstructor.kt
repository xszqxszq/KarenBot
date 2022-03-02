package tk.xszq.otomadbot.text

import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.ForwardMessage
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.sendTo
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import tk.xszq.otomadbot.*
import tk.xszq.otomadbot.image.ImageCommonHandler

object ForwardMessageConstructor: EventHandler("forward", "转发消息伪造",
    HandlerType.DEFAULT_DISABLED) {
    override fun register() {
        GlobalEventChannel.subscribeAlways<GroupMessageEvent> {
            if (it.message.first() is ForwardMessage) {
                println(it.message.serializeToMiraiCode())
            }
        }
        GlobalEventChannel.subscribeGroupMessages {
            equalsTo("伪造消息", true) {
                requireBotAdmin {
                    val admin = bot.groups.find { it.id == 1006515283L }
                    ForwardMessageBuilder(admin as Contact).add(admin as User, PlainText("假转发消息测试")
                        ).add(admin, ImageCommonHandler.replyPic.getRandom("reply").uploadAsImage(subject))
                        .build().sendTo(subject)
                    pass
                }
            }
        }
        super.register()
    }
}