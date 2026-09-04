package xyz.xszq.bot.subscribe

/**
 * 命令未匹配异常
 *
 * 文本订阅的 handler 判定自己不处理该消息时抛出，
 * 订阅管理器会尝试同命令域中的下一个候选订阅
 */
class CommandNotMatchedException: Exception()