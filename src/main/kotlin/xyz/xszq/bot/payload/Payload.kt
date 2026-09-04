package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable
import xyz.xszq.bot.util.JsonAsStringSerializer

/**
 * Webhook 收到的事件
 *
 * @param op 操作码
 * @param d 事件数据
 * @param t 事件类型
 * @param id 事件 ID
 */
@Serializable
data class Payload(
    val op: Int,
    @Serializable(with = JsonAsStringSerializer::class)
    val d: String ?= null,
    val s: Int ?= null,
    val t: String? = null,
    val id: String? = null,
)