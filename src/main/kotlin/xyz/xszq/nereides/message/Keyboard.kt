package xyz.xszq.nereides.message

import xyz.xszq.nereides.payload.message.MessageKeyboard

class Keyboard(val keyboard: MessageKeyboard): Message {
    override fun contentToString(): String {
        return "[keyboard]"
    }
}