@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package tk.xszq.otomadbot

import net.mamoe.mirai.console.permission.PermissionId
import net.mamoe.mirai.console.permission.PermissionService

enum class HandlerType {
    DEFAULT_ENABLED,
    DEFAULT_DISABLED,
    RESTRICTED_ENABLED,
    RESTRICTED_DISABLED
}
val enabledTypes = listOf(HandlerType.DEFAULT_ENABLED, HandlerType.RESTRICTED_ENABLED)
val disabledTypes = listOf(HandlerType.DEFAULT_DISABLED, HandlerType.RESTRICTED_DISABLED)
val restrictedTypes = listOf(HandlerType.RESTRICTED_ENABLED, HandlerType.RESTRICTED_DISABLED)

open class AbstractEventHandler(open val funcName: String, open val permName: String, open var type: HandlerType) {
    open fun register() {}
}
open class EventHandler(
    override val funcName: String, override val permName: String,
    override var type: HandlerType = HandlerType.DEFAULT_ENABLED
) : AbstractEventHandler(funcName, permName, type) {
    val allowed by lazy {
        PermissionService.INSTANCE.register(
            PermissionId("otm", permName), "允许$funcName")
    }
    val denied by lazy {
        PermissionService.INSTANCE.register(
            PermissionId("otm", "deny.$permName"), "禁用$funcName")
    }
    override fun register() {
        when (type) {
            in enabledTypes -> denied
            in disabledTypes -> allowed
            else -> pass
        }
        super.register()
    }
}
open class AdminEventHandler: AbstractEventHandler("", "", HandlerType.RESTRICTED_ENABLED) {
    companion object {
        val botAdmin by lazy {
            PermissionService.INSTANCE.register(
                PermissionId("otm", "admin"), "机器人管理者，允许使用所有管理命令")
        }
    }
    override fun register() {
        botAdmin
        super.register()
    }
}