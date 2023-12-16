package xyz.xszq.nereides.message

import xyz.xszq.nereides.payload.message.MessageArk

open class Ark(
    val ark: MessageArk
) : Message {
    override fun contentToString(): String {
        return "[ark]"
    }
}