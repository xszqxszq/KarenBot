package xyz.xszq.bot

/**
 * 运行时控制，管理插件与 core 交互的界面
 */
interface RuntimeControl {
    /**
     * 调试日志开关
     */
    var debugLog: Boolean

    /**
     * 重载 Bot 相关配置
     */
    fun reloadConfig()
}