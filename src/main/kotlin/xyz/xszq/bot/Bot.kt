package xyz.xszq.bot

import xyz.xszq.bot.payload.UsersMeResponse
import xyz.xszq.bot.service.OpenAPI
import xyz.xszq.bot.service.TencentCOS

/**
 * Bot 类
 *
 * @property api 与 QQ 服务器通信的客户端
 * @property cos 腾讯云 COS 客户端
 * @property me 机器人自身详情
 */
class Bot(
    val api: OpenAPI,
    val cos: TencentCOS,
    val me: UsersMeResponse ?= null
) {
    val self: User by lazy {
        User(this, me ?.id ?: "", me ?.username ?: "", isBot = true, isSelf = true)
    }
}