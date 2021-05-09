@file:Suppress("unused")
package tk.xszq.otomadbot.database

import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.AbstractEvent
import net.mamoe.mirai.event.events.GroupEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object Permissions : IntIdTable() {
    override val tableName = "permission"
    val name = varchar("name", 64)
    val enabled = bool("enabled")
    val subject = long("subject")
}
class Permission(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Permission>(Permissions)
    var name by Permissions.name
    var enabled by Permissions.enabled
    var subject by Permissions.subject
}

suspend fun getPermissionRaw(permission: String, subject: Long, isGroup: Boolean = false, forbid: Boolean = false) : Boolean {
    val realSubject = (if (isGroup) 1L else -1L) * subject
    val realPermission = (if (forbid) "forbid." else "") + permission
    return newSuspendedTransaction(db = Databases.cache) {
        acquireLock {
            Permissions.select {
                RegexpOpCol(stringParam(realPermission), "name") and
                        (Permissions.subject eq realSubject) and (Permissions.enabled eq true)}.count() != 0L
        }
    }
}

suspend fun isAllowed(permission: String, subject: Long, isGroup: Boolean = false) : Boolean {
    return when {
        getPermissionRaw(permission, subject, isGroup, true) -> false
        getPermissionRaw(permission, subject, isGroup, false) -> true
        getPermissionRaw(permission, 0, isGroup, true) -> false
        getPermissionRaw(permission, 0, isGroup, false) -> true
        else -> true
    }
}

/**
 * 初始化群组权限并获取的函数。
 * @param name 权限名
 * @param subject 对象
 * @param type 初始化类型：-1为设为有权限，-2为设为无权限，0为设成 subject.members.size<=arg，1为设成 subject.members.size>=arg，\
 *             2为设成 subject.members.size<arg，3为设成 subject.members.size>arg
 */
suspend fun getInitGroupPermAndCheck(name: String, subject: Group, type: Int = -1, arg: Long = 0L): Boolean {
    return if (!getPermissionRaw(name, subject.id, true) &&
        !getPermissionRaw(name, subject.id, isGroup = true, forbid = true)) {
        val permStat = when(type) {
            -1 -> true
            -2 -> false
            0 -> subject.members.size <= arg
            1 -> subject.members.size >= arg
            2 -> subject.members.size < arg
            3 -> subject.members.size > arg
            else -> true
        }
        val realPerm = (if (permStat) "^" else "^forbid\\.") + name.replace(".", "\\.")
        newSuspendedTransaction(db = Databases.cache) {
            acquireLock {
                if (Permissions.select { Permissions.name eq realPerm and (Permissions.subject eq subject.id)
                    }.count() == 0L) {
                    Permissions.insert {
                        it[Permissions.name] = realPerm; it[Permissions.subject] = subject.id
                        it[enabled] = true
                    }
                } else {
                    Permissions.update({ Permissions.name eq realPerm and (Permissions.subject eq subject.id) }) {
                        it[enabled] = true
                    }
                }
            }
        }
        permStat
    } else {
        isAllowed(name, subject.id, true)
    }
}

suspend fun <T> GroupEvent.require(permission: String, block: suspend () -> T): T? {
    return if (isAllowed(permission, group.id, true)) block() else null
}

suspend fun <T> GroupEvent.requireOrInit(permission: String, type: Int = -1, arg: Long = 0L, block: suspend () -> T): T? {
    return if (getInitGroupPermAndCheck(permission, group, type, arg)) block() else null
}

suspend fun <T> GroupEvent.requireForbid(permission: String, block: suspend () -> T): T? {
    return if (!isAllowed(permission, group.id, true)) block() else null
}

suspend fun <T> GroupMessageEvent.require(permission: String, block: suspend () -> T): T? {
    return if (isAllowed(permission, group.id, true)) block() else null
}

suspend fun <T> GroupMessageEvent.requireMember(permission: String, member: Member? = null, block: suspend () -> T): T? {
    return if (isAllowed(permission, (member ?: sender).id, false)) block() else null
}

suspend fun <T> GroupMessageEvent.requireMemberOrAdmin(permission: String, member: Member? = null, block: suspend () -> T): T? {
    return if ((member ?: sender).permission.level > 0 || isAllowed(permission, (member ?: sender).id, false)) block() else null
}

suspend fun <T> GroupMessageEvent.requireAdmin(member: Member? = null, block: suspend () -> T): T? {
    return if ((member ?: sender).permission.level > 0) block() else null
}

suspend fun <T> GroupMessageEvent.requireOrInit(permission: String, type: Int = -1, arg: Long = 0L, block: suspend () -> T): T? {
    return if (getInitGroupPermAndCheck(permission, group, type, arg)) block() else null
}

suspend fun <T> MessageEvent.require(permission: String, block: suspend () -> T): T? {
    return if (isAllowed(permission, subject.id, subject is Group)) block() else null
}

suspend fun <T> MessageEvent.requireSender(permission: String, block: suspend () -> T): T? {
    return if (isAllowed(permission, sender.id, false)) block() else null
}

suspend fun <T> AbstractEvent.require(permission: String, subject: Contact, block: suspend () -> T): T? {
    return if (isAllowed(permission, subject.id, subject is Group)) block() else null
}
