package xyz.xszq.nereides.message

import xyz.xszq.nereides.message.ark.ListArk
import xyz.xszq.nereides.payload.message.MessageArk

open class Ark(
    val ark: MessageArk
) : Message {
    override fun contentToString(): String {
        return "[ark]" + if (this is ListArk) text else ""
    }
}