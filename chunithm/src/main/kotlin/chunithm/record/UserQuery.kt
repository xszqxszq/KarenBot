package xyz.xszq.bot.chunithm.record

sealed class UserQuery {
    data class QQ(val value: Int) : UserQuery()
    data class Username(val value: String) : UserQuery()
}