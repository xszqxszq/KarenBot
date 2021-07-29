@file:Suppress("unused")

package tk.xszq.otomadbot

import net.mamoe.mirai.console.permission.PermissionId
import net.mamoe.mirai.console.permission.PermissionService

open class AbstractEventHandler {
    open fun register() {}
}
open class EventHandler(val funcName: String, val permName: String) : AbstractEventHandler() {
    val allowed by lazy {
        PermissionService.INSTANCE.register(
            PermissionId("otm", permName), "允许$funcName")
    }
    val denied by lazy {
        PermissionService.INSTANCE.register(
            PermissionId("otm", "deny.$permName"), "禁用$funcName")
    }
    override fun register() {
        denied // Use `denied` by default
        super.register()
    }
}
open class AdminEventHandler : AbstractEventHandler() {
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