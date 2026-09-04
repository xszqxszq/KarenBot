package xyz.xszq.bot.database

import xyz.xszq.bot.event.GroupMessageEvent

/**
 * 检查命令是否启用，启用时执行
 *
 * @param command 命令名（如 `random.blonde.auto`）
 * @param block 代码块
 */
suspend fun GroupMessageEvent.whenEnabled(
    command: String,
    block: suspend GroupMessageEvent.() -> Unit
): GroupCommandSettings.WhenEnabled {
    val enabled = GroupCommandSettings.isEnabled(group.id, command)
    if (enabled)
        block()
    return GroupCommandSettings.WhenEnabled(enabled, this)
}
