package xyz.xszq.bot

import xyz.xszq.bot.payload.UsersMeResponse

/**
 * 机器人门面
 *
 * 持有本机器人的开放平台客户端与自身信息，是插件与 QQ 交互的入口
 *
 * @property api QQ 开放平台客户端
 * @property cos 腾讯云 COS 客户端
 * @property me 机器人自身信息，启动时拉取，失败时为空
 */
class Bot(
    val api: OpenAPI,
    val cos: TencentCOS,
    val me: UsersMeResponse ?= null
) {
    /**
     * 机器人自身，以用户视角表示
     *
     * 个人信息拉取失败时 id 与用户名退化为空串
     */
    val self: User by lazy {
        User(this, me?.id ?: "", me?.username ?: "", isBot = true, isSelf = true)
    }
}