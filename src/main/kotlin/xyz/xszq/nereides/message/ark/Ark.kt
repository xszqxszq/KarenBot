package xyz.xszq.nereides.message.ark

import xyz.xszq.nereides.message.Message
import xyz.xszq.nereides.payload.message.MessageArk

open class Ark(
    val ark: MessageArk
) : Message {
    override fun contentToString(): String {
        return "[ark]" + if (this is ListArk) text else ""
    }
}