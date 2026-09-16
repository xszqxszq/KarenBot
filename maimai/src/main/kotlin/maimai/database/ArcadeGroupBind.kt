package xyz.xszq.bot.maimai.database

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import xyz.xszq.bot.database.newSuspendedTransaction
import xyz.xszq.bot.exception.IllegalArgsException
import xyz.xszq.bot.exception.NotFoundException
import java.util.concurrent.ConcurrentHashMap

/**
 * 排队管理的机厅分组绑定
 */
class ArcadeGroupBind(id: EntityID<String>): Entity<String>(id) {
    var group by ArcadeGroupBindTable.group

    companion object : EntityClass<String, ArcadeGroupBind>(ArcadeGroupBindTable) {
        private val arcadeAliases = ConcurrentHashMap<String, List<ArcadeAlias>>()
        private fun currentTime() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        private data class ArcadeAlias(val id: Int, val names: List<String>)

        /**
         * 清除该群机厅信息缓存
         *
         * @param openId 群 OpenID
         */
        fun clearCache(openId: String) {
            arcadeAliases.remove(openId)
        }

        /**
         * 清空全部缓存
         */
        fun clearCache() {
            arcadeAliases.clear()
        }

        /**
         * 读取群全部机厅名称及别名
         *
         * @param openId 群 OpenID
         * @return 机厅别名
         */
        private suspend fun aliasesOf(openId: String): List<ArcadeAlias> {
            arcadeAliases[openId] ?.let { return it }
            val loaded = newSuspendedTransaction {
                findGroup(openId) ?.arcades ?.map { arcade ->
                    ArcadeAlias(
                        id = arcade.id.value,
                        names = arcade.aliases.split(",").filter { it.isNotBlank() }
                    )
                } ?: emptyList()
            }
            arcadeAliases[openId] = loaded
            return loaded
        }

        private fun findGroup(openId: String) = findById(openId) ?.let {
            ArcadeGroup.findById(it.group)
        }

        private fun groupOrCreate(openId: String) = findGroup(openId) ?: run {
            val groupId = ArcadeGroupTable.insert {
                it[name] = openId
            }[ArcadeGroupTable.id]
            ArcadeGroupBindTable.insert {
                it[id] = openId
                it[ArcadeGroupBindTable.group] = groupId
            }
            ArcadeGroup.findById(groupId)!!
        }

        /**
         * 获取群绑定的机厅分组
         *
         * @param openId 群 OpenID
         * @return 机厅分组
         */
        suspend fun group(
            openId: String
        ) = newSuspendedTransaction {
            groupOrCreate(openId)
        }

        /**
         * 将群绑定到指定名称的机厅分组
         *
         * @param openId 群 OpenID
         * @param groupName 分组名
         */
        suspend fun bind(openId: String, groupName: String) = newSuspendedTransaction {
            val target = ArcadeGroup.find { ArcadeGroupTable.name eq groupName }.firstOrNull()
                ?: throw IllegalArgsException("该分组不存在。")
            if (ArcadeGroupBindTable.selectAll().where { ArcadeGroupBindTable.id eq openId }.count() != 0L)
                ArcadeGroupBindTable.update({ ArcadeGroupBindTable.id eq openId }) {
                    it[ArcadeGroupBindTable.group] = target.id
                }
            else
                ArcadeGroupBindTable.insert {
                    it[id] = openId
                    it[ArcadeGroupBindTable.group] = target.id
                }
        }.also { clearCache(openId) }

        /**
         * 在群绑定的机厅分组中添加机厅
         *
         * @param openId 群 OpenID
         * @param name 机厅名
         */
        suspend fun addArcade(openId: String, name: String) = newSuspendedTransaction {
            val group = groupOrCreate(openId)
            if (group.find(name) != null)
                throw IllegalArgsException("机厅已存在！")
            ArcadeTable.insert {
                it[ArcadeTable.group] = group.id
                it[ArcadeTable.name] = name
                it[ArcadeTable.aliases] = name
                it[ArcadeTable.value] = 0
            }
        }.also { clearCache(openId) }

        /**
         * 删除群绑定分组中的机厅
         *
         * @param openId 群 OpenID
         * @param name 机厅名
         */
        suspend fun deleteArcade(openId: String, name: String) = newSuspendedTransaction {
            val group = findGroup(openId) ?: throw NotFoundException("机厅不存在！")
            val arcade = group.find(name) ?: throw NotFoundException("机厅不存在！")
            arcade.delete()
        }.also { clearCache(openId) }

        /**
         * 为分组中的机厅添加别名
         *
         * @param openId 群 OpenID
         * @param name 机厅名
         * @param alias 要添加的别名
         */
        suspend fun addAlias(openId: String, name: String, alias: String) = newSuspendedTransaction {
            val group = findGroup(openId) ?: throw NotFoundException("机厅不存在！")
            val arcade = group.find(name) ?: throw NotFoundException("机厅不存在！")
            val aliases = arcade.aliases.split(",").filter { it.isNotBlank() }.toMutableList()
            if (group.find(alias) != null || aliases.any { it.equals(alias, ignoreCase = true) })
                throw IllegalArgsException("别名已存在！")
            aliases.add(alias)
            ArcadeTable.update({ ArcadeTable.id eq arcade.id }) {
                it[ArcadeTable.aliases] = aliases.joinToString(",")
            }
        }.also { clearCache(openId) }

        /**
         * 删除机厅的别名
         *
         * @param openId 群 OpenID
         * @param name 机厅名
         * @param alias 要删除的别名
         */
        suspend fun deleteAlias(openId: String, name: String, alias: String) = newSuspendedTransaction {
            val group = findGroup(openId) ?: throw NotFoundException("机厅不存在！")
            val arcade = group.find(name) ?: throw NotFoundException("机厅不存在！")
            val aliases = arcade.aliases.split(",").filter { it.isNotBlank() }.toMutableList()
            aliases.removeAll { it == alias }
            ArcadeTable.update({ ArcadeTable.id eq arcade.id }) {
                it[ArcadeTable.aliases] = aliases.joinToString(",")
            }
        }.also { clearCache(openId) }

        /**
         * 获取机厅的全部别名
         *
         * @param openId 群 OpenID
         * @param name 机厅名
         * @return 别名列表
         */
        suspend fun aliases(openId: String, name: String) = newSuspendedTransaction {
            val group = findGroup(openId) ?: throw NotFoundException("机厅不存在！")
            val arcade = group.find(name) ?: throw NotFoundException("机厅不存在！")
            arcade.aliases.split(",").filter { it.isNotBlank() }
        }

        /**
         * 列出群绑定分组下的全部机厅
         *
         * 先清理隔天数据，写入后于新事务中重读以拿到清理结果
         *
         * @param openId 群 OpenID
         * @return 机厅列表
         */
        suspend fun listArcades(openId: String): List<Arcade>? {
            newSuspendedTransaction {
                val group = findGroup(openId) ?: return@newSuspendedTransaction
                group.arcades.toList().forEach { it.clear() }
            }
            return newSuspendedTransaction {
                val group = findGroup(openId) ?: return@newSuspendedTransaction null
                group.arcades.toList()
            }
        }

        /**
         * 按名称或别名查找群绑定分组中的机厅
         *
         * 先清理隔天数据，写入后于新事务中重读以拿到清理结果
         *
         * @param openId 群 OpenID
         * @param name 机厅名或别名
         * @return 机厅
         */
        suspend fun findArcade(openId: String, name: String): Arcade? {
            newSuspendedTransaction {
                val group = findGroup(openId) ?: return@newSuspendedTransaction
                group.find(name) ?.clear()
            }
            return newSuspendedTransaction {
                val group = findGroup(openId) ?: return@newSuspendedTransaction null
                group.find(name)
            }
        }

        /**
         * 更新机厅的排卡人数
         *
         * 写入后于新事务中重读以拿到更新结果
         *
         * @param openId 群 OpenID
         * @param raw 原始查询文本
         * @param modifiedAt 更新时间
         * @return 更新后的机厅
         */
        suspend fun updateArcade(
            openId: String,
            raw: String,
            modifiedAt: LocalDateTime = currentTime()
        ): Arcade? {
            val (candidate, alias) = aliasesOf(openId).firstNotNullOfOrNull { candidate ->
                candidate.names.firstOrNull { name -> raw.startsWith(name) }
                    ?.let { name -> Pair(candidate, name) }
            } ?: return null

            val arcadeId = newSuspendedTransaction {
                val arcade = Arcade.findById(EntityID(candidate.id, ArcadeTable))
                    ?: return@newSuspendedTransaction null

                var newValue = when {
                    raw.startsWith("$alias+") -> {
                        arcade.value + raw.substringAfter("${alias}+").filter { it.isDigit() }.toInt()
                    }
                    raw.startsWith("$alias-") -> {
                        arcade.value - raw.substringAfter("${alias}-").filter { it.isDigit() }.toInt()
                    }
                    else -> {
                        raw.substringAfter(alias)
                            .replace("=", "").toIntOrNull() ?: return@newSuspendedTransaction null
                    }
                }
                if (newValue > 50)
                    return@newSuspendedTransaction null
                if (newValue < 0)
                    newValue = 0
                ArcadeTable.update({ ArcadeTable.id eq arcade.id }) {
                    it[ArcadeTable.value] = newValue
                    it[ArcadeTable.modified] = modifiedAt
                }
                arcade.id
            } ?: return null
            return newSuspendedTransaction {
                Arcade.findById(arcadeId)
            }
        }
    }
}