package xyz.xszq.bot.maimai.database

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import xyz.xszq.bot.exception.IllegalArgsException
import xyz.xszq.bot.exception.NotFoundException

class ArcadeGroupBind(id: EntityID<String>): Entity<String>(id) {
    var group by ArcadeGroupBindTable.group

    companion object : EntityClass<String, ArcadeGroupBind>(ArcadeGroupBindTable) {
        private fun currentTime() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        private fun findGroup(openId: String) = findById(openId) ?.let {
            ArcadeGroup.findById(it.group)
        }

        private fun groupOrCreate(openId: String) = findGroup(openId) ?: ArcadeGroup.new {
            name = openId
        }.also { newGroup ->
            new(openId) {
                group = newGroup.id
            }
        }

        suspend fun group(
            openId: String
        ) = suspendedTransactionAsync {
            groupOrCreate(openId)
        }.await()

        suspend fun find(
            openId: String
        ) = suspendedTransactionAsync {
            findGroup(openId)
        }.await()

        suspend fun bind(openId: String, group: ArcadeGroup) = newSuspendedTransaction {
            findById(openId) ?.let {
                it.group = group.id
            } ?: new(openId) {
                this.group = group.id
            }
        }

        suspend fun bind(openId: String, groupName: String) = newSuspendedTransaction {
            val target = ArcadeGroup.find { ArcadeGroupTable.name eq groupName }.firstOrNull()
                ?: throw IllegalArgsException("该分组不存在。")
            findById(openId) ?.let {
                it.group = target.id
            } ?: new(openId) {
                group = target.id
            }
        }

        suspend fun addArcade(openId: String, name: String) = newSuspendedTransaction {
            val group = groupOrCreate(openId)
            if (group.find(name) != null)
                throw IllegalArgsException("机厅已存在！")
            Arcade.new {
                this.group = group.id
                this.name = name
                aliases = name
                value = 0
            }
        }

        suspend fun deleteArcade(openId: String, name: String) = newSuspendedTransaction {
            val group = findGroup(openId) ?: throw NotFoundException("机厅不存在！")
            val arcade = group.find(name) ?: throw NotFoundException("机厅不存在！")
            arcade.delete()
        }

        suspend fun addAlias(openId: String, name: String, alias: String) = newSuspendedTransaction {
            val group = findGroup(openId) ?: throw NotFoundException("机厅不存在！")
            val arcade = group.find(name) ?: throw NotFoundException("机厅不存在！")
            val aliases = arcade.aliases.split(",").filter { it.isNotBlank() }.toMutableList()
            if (group.find(alias) != null || aliases.any { it.equals(alias, ignoreCase = true) })
                throw IllegalArgsException("别名已存在！")
            aliases.add(alias)
            arcade.aliases = aliases.joinToString(",")
        }

        suspend fun deleteAlias(openId: String, name: String, alias: String) = newSuspendedTransaction {
            val group = findGroup(openId) ?: throw NotFoundException("机厅不存在！")
            val arcade = group.find(name) ?: throw NotFoundException("机厅不存在！")
            val aliases = arcade.aliases.split(",").filter { it.isNotBlank() }.toMutableList()
            aliases.removeAll { it == alias }
            arcade.aliases = aliases.joinToString(",")
        }

        suspend fun aliases(openId: String, name: String) = newSuspendedTransaction {
            val group = findGroup(openId) ?: throw NotFoundException("机厅不存在！")
            val arcade = group.find(name) ?: throw NotFoundException("机厅不存在！")
            arcade.aliases.split(",").filter { it.isNotBlank() }
        }

        suspend fun listArcades(openId: String) = newSuspendedTransaction {
            val group = findGroup(openId) ?: return@newSuspendedTransaction null
            val arcades = group.arcades.toList()
            arcades.forEach { it.clear() }
            arcades.map { it.snapshot() }
        }

        suspend fun findArcade(openId: String, name: String) = newSuspendedTransaction {
            val group = findGroup(openId) ?: return@newSuspendedTransaction null
            val arcade = group.find(name) ?: return@newSuspendedTransaction null
            arcade.clear()
            arcade.snapshot()
        }

        suspend fun updateArcade(openId: String, raw: String, modifiedAt: LocalDateTime = currentTime()) = newSuspendedTransaction {
            val group = findGroup(openId) ?: return@newSuspendedTransaction null
            for (arcade in group.arcades) {
                for (alias in arcade.aliases.split(",").filter { it.isNotBlank() }) {
                    if (!raw.startsWith(alias))
                        continue

                    var newValue = when {
                        raw.startsWith("$alias+") -> arcade.value + raw.substringAfter("${alias}+").filter { it.isDigit() }.toInt()
                        raw.startsWith("$alias-") -> arcade.value - raw.substringAfter("${alias}-").filter { it.isDigit() }.toInt()
                        else -> raw.substringAfter(alias).replace("=", "").toIntOrNull() ?: return@newSuspendedTransaction null
                    }
                    if (newValue > 50)
                        return@newSuspendedTransaction Arcade.UpdateResult.TooLarge
                    if (newValue < 0)
                        newValue = 0
                    arcade.value = newValue
                    arcade.modified = modifiedAt
                    return@newSuspendedTransaction Arcade.UpdateResult.Updated(arcade.snapshot())
                }
            }
            null
        }
    }
}
