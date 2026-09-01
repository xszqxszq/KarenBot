package xyz.xszq.bot.maimai.music

import xyz.xszq.bot.event.MessageEvent

sealed class UserQueryParams(
    open val event: MessageEvent,
    open val isSelf: Boolean,
    open val settings: PlayerSettings?
) {
    data class Self(
        override val event: MessageEvent,
        override val settings: PlayerSettings? = null
    ) : UserQueryParams(event, true, settings)
    data class Username(
        val username: String,
        override val event: MessageEvent,
        override val isSelf: Boolean = false,
        override val settings: PlayerSettings? = null
    ) : UserQueryParams(event, isSelf, settings)
    data class FriendCode(
        val friendCode: String,
        override val event: MessageEvent,
        override val isSelf: Boolean = false,
        override val settings: PlayerSettings? = null
    ) : UserQueryParams(event, isSelf, settings)
}