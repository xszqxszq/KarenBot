package xyz.xszq.otomadbot

import net.mamoe.mirai.console.permission.Permission
import net.mamoe.mirai.console.permission.PermissionId
import net.mamoe.mirai.console.permission.PermissionService
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType

abstract class CommandModule(val name: String, val permName: String) {
    val allowPerm: Permission = PermissionService.INSTANCE.register(
        PermissionId("otm", permName), "允许使用 $name 模块的所有功能")
    val denyPerm: Permission = PermissionService.INSTANCE.register(
        PermissionId("otm", "deny.$permName"), "禁止使用 $name 模块的所有功能")
    abstract suspend fun subscribe()
    suspend fun register() {
        this::class.declaredMemberProperties.filter {
            it.returnType.isSubtypeOf(Command::class.starProjectedType)
        }.forEach {
            val now = (it.call(this) as Command<*>)
            now.perm =
                if (now.defaultEnabled)
                    PermissionService.INSTANCE.register(
                        PermissionId("otm", "deny.$permName.${now.permName}"),
                        "禁用 $name 模块的 ${now.name} 功能", denyPerm)
                else PermissionService.INSTANCE.register(
                    PermissionId("otm", "$permName.${now.permName}"),
                        "启用 $name 模块的 ${now.name} 功能", allowPerm)
            now.parent = this
            now.perm
        }
        subscribe()
    }
    fun getCommands(): List<Command<*>> =
        this::class.declaredMemberProperties.filter {
            it.returnType.isSubtypeOf(Command::class.starProjectedType)
        }.map { it.call(this) as Command<*>}
}