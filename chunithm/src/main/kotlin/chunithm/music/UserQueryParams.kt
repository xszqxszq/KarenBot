package xyz.xszq.bot.chunithm.music

sealed class UserQueryParams {
    data class QQ(val qq: Long) : UserQueryParams()
    data class Username(val username: String) : UserQueryParams()
}