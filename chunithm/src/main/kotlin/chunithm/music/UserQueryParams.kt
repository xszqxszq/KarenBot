package xyz.xszq.bot.chunithm.music

import xyz.xszq.bot.event.MessageEvent

/**
 * 玩家查询参数
 *
 * @property event 查询事件
 * @property isSelf 是否查询自己
 * @property settings 玩家自定义设置
 */
sealed class UserQueryParams(
    open val event: MessageEvent,
    open val isSelf: Boolean,
    open val settings: PlayerSettings?
) {
    /**
     * 查询自己
     */
    data class Self(
        override val event: MessageEvent,
        override val settings: PlayerSettings? = null
    ) : UserQueryParams(event, true, settings)

    /**
     * 按查分器用户名查询
     *
     * @property username 查分器用户名
     */
    data class Username(
        val username: String,
        override val event: MessageEvent,
        override val isSelf: Boolean = false,
        override val settings: PlayerSettings? = null
    ) : UserQueryParams(event, isSelf, settings)

    /**
     * 按查分器好友码查询
     *
     * @property friendCode 好友码
     */
    data class FriendCode(
        val friendCode: String,
        override val event: MessageEvent,
        override val isSelf: Boolean = false,
        override val settings: PlayerSettings? = null
    ) : UserQueryParams(event, isSelf, settings)
}