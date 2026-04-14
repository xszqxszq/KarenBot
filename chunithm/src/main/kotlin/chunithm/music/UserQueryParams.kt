package xyz.xszq.bot.chunithm.music

import xyz.xszq.bot.event.MessageEvent

sealed class UserQueryParams(
    open val event: MessageEvent,
    open val isSelf: Boolean,
    open val settings: PlayerSettings?
) {
    data class QQ(
        val qq: Long,
        override val event: MessageEvent,
        override val isSelf: Boolean,
        override val settings: PlayerSettings? = null
    ) : UserQueryParams(event, isSelf, settings)
    data class Username(
        val username: String,
        override val event: MessageEvent,
        override val isSelf: Boolean,
        override val settings: PlayerSettings? = null
    ) : UserQueryParams(event, isSelf, settings)
}