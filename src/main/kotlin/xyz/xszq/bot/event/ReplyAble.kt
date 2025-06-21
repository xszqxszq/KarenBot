package xyz.xszq.bot.event

sealed interface ReplyAble: Event {
    val id: String
    var seq: Int
}