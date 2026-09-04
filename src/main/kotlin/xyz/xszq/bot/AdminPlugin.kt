package xyz.xszq.bot

/**
 * 管理插件
 *
 * 需要操作 core 的插件需继承此类
 */
interface AdminPlugin {
    /**
     * 运行时控制用
     */
    var control: RuntimeControl
}