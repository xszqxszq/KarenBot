package xyz.xszq.nereides.message

import xyz.xszq.nereides.payload.user.GuildUser

open class At(val target: String): Message {
    override fun contentToString(): String {
        return "[at:${target}]"
    }

}