package xyz.xszq.bot.event

/**
 * 可回复事件
 *
 * @property id 对应的 ID
 * @property seq 消息序号
 */
sealed interface ReplyAble: Event {
    val id: String
    var seq: Int
}