package xyz.xszq.bot.load

/**
 * 压测场景
 *
 * @property name 场景名
 * @property content 发送的消息内容
 * @property weight 场景权重
 * @property user 发送者编号
 * @property group 所在群编号
 */
class LoadScenario(
    val name: String,
    val content: String,
    val weight: Int = 1,
    val user: Int = 0,
    val group: Int = 0
)
