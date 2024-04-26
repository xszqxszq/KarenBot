package xyz.xszq.karenbot.text

import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.content
import org.jetbrains.exposed.sql.and
import xyz.xszq.events
import xyz.xszq.karenbot.CommandModule
import xyz.xszq.karenbot.CommonCommand
import xyz.xszq.karenbot.CommonCommandWithArg
import xyz.xszq.karenbot.dao.*
import xyz.xszq.karenbot.kotlin.isSameDay
import xyz.xszq.karenbot.kotlin.toArgsList
import xyz.xszq.karenbot.kotlin.transactionWithLock
import xyz.xszq.karenbot.mirai.reply
import java.time.Duration
import java.time.LocalDateTime

object ArcadeQueue: CommandModule("机厅排卡", "arcade.queue") {
    private val initTime = LocalDateTime.of(2000, 1, 1, 0, 0)
    override suspend fun subscribe() {
        clear()
        events.subscribeGroupMessages {
            always {
                query.checkAndRun(this)
            }
            startsWith("排卡管理") { raw ->
                manage.checkAndRun(this, raw)
            }
        }
    }
    val query = CommonCommand("排卡查询", "edit", true) {
        if (this is GroupMessageEvent)
            handle(this)
    }
    val manage = CommonCommandWithArg("排卡管理", "manage", true) { raw ->
        if (this !is GroupMessageEvent)
            return@CommonCommandWithArg
        val contextId = this@CommonCommandWithArg.group.id.toString()
        val args = raw!!.toArgsList()
        if (args.size < 2) {
            reply(buildString {
                appendLine("本命令可以设置机厅排卡功能。支持的子命令如下：")
                appendLine("排卡管理 加入分组 分组名")
                appendLine("排卡管理 添加机厅 机厅名称")
                appendLine("排卡管理 删除机厅 机厅名称")
                appendLine("排卡管理 查看别名 机厅名称")
                appendLine("排卡管理 添加别名 机厅名称 机厅别名")
                appendLine("排卡管理 删除别名 机厅名称 机厅别名")
            })
            return@CommonCommandWithArg
        }
        val name = args[1]
        when (args[0]) {
            "加入分组" -> {
                transactionWithLock {
                    ArcadeQueueGroup.find {
                        ArcadeQueueGroups.name eq name
                    }.firstOrNull() ?.let { group ->
                        ArcadeCenterQueueGroup.new(contextId) {
                            this.group = group.id
                        }
                        reply("加入成功！")
                    } ?: run {
                        reply("分组不存在")
                    }
                }
            }
            "添加机厅" -> {
                val queueGroup = getQueueGroup(contextId)
                transactionWithLock {
                    ArcadeCenter.new {
                        this.group = queueGroup.id
                        this.name = name
                        this.abbr = name
                        this.value = 0
                    }
                    reply("添加机厅${name}成功。请使用“排卡管理 设置别名 别名”来添加机厅别名。")
                }
            }
            "删除机厅" -> {
                val queueGroup = getQueueGroup(contextId)
                transactionWithLock {
                    ArcadeCenter.find {
                        (ArcadeCenters.group eq queueGroup.id) and (ArcadeCenters.name eq name)
                    }.firstOrNull() ?.let {
                        it.delete()
                        reply("删除机厅${name}成功。")
                    } ?: run {
                        reply("机厅${name}不存在，请重试！")
                    }
                }
            }
            "添加别名" -> {
                if (args.size < 3) {
                    reply("使用方法：\n\t排卡管理 添加别名 机厅名称 机厅别名")
                    return@CommonCommandWithArg
                }
                val alias = args[2]
                val queueGroup = getQueueGroup(contextId)
                transactionWithLock {
                    ArcadeCenter.find {
                        (ArcadeCenters.group eq queueGroup.id) and (ArcadeCenters.name eq name)
                    }.firstOrNull() ?.let {
                        val abbr = it.abbr.split(",").toMutableSet()
                        abbr.add(alias)
                        it.abbr = abbr.joinToString(",")
                        reply("机厅${name}的别名“${alias}”添加成功，当前别名：${it.abbr}")
                    } ?: run {
                        reply("机厅${name}不存在，请重试！")
                    }
                }
            }
            "删除别名" -> {
                if (args.size < 3) {
                    reply("使用方法：\n\t排卡管理 删除别名 机厅名称 机厅别名")
                    return@CommonCommandWithArg
                }
                val alias = args[2]
                transactionWithLock {
                    val queueGroup = getQueueGroup(contextId)
                    ArcadeCenter.find {
                        (ArcadeCenters.group eq queueGroup.id) and (ArcadeCenters.name eq name)
                    }.firstOrNull() ?.let {
                        val abbr = it.abbr.split(",").toMutableSet()
                        abbr.remove(alias)
                        it.abbr = abbr.joinToString(",")
                        reply("机厅${name}的别名“${alias}”删除成功，当前别名：${it.abbr}")
                    } ?: run {
                        reply("机厅${name}不存在，请重试！")
                    }
                }
            }
            "查看别名" -> {
                transactionWithLock {
                    val queueGroup = getQueueGroup(contextId)
                    ArcadeCenter.find {
                        (ArcadeCenters.group eq queueGroup.id) and (ArcadeCenters.name eq name)
                    }.firstOrNull() ?.let {
                        reply("机厅${name}的别名有：${it.abbr}")
                    } ?: run {
                        reply("机厅${name}不存在，请重试！")
                    }
                }
            }
        }
    }
    private fun clear() = runBlocking {
        transactionWithLock {
            ArcadeCenter.all().forEach {
                if (it.modified == initTime || LocalDateTime.now().isSameDay(it.modified))
                    return@forEach
                it.value = 0
                it.modified = initTime
            }
        }
    }
    suspend fun getQueueGroup(openId: String) = transactionWithLock {
        ArcadeCenterQueueGroup.findById(openId) ?.let {
            ArcadeQueueGroup.findById(it.group)
        } ?: ArcadeQueueGroup.new {
            this.name = openId
        }.run {
            ArcadeCenterQueueGroup.new(openId) {
                this.group = this@run.id
            }
            this
        }
    }
    private fun getQueueInfo(centers: List<ArcadeCenter>) = buildString {
        val nowTime = LocalDateTime.now()
        appendLine("机厅排卡人数：")
        appendLine()
        centers.forEach { arcade ->
            appendLine(
                buildString {
                    append("${arcade.name}: ${arcade.value}人 (")
                    append(if (arcade.modified == initTime) {
                        "今日未更新数据"
                    } else if (Duration.between(arcade.modified, nowTime).toHours() < 1L){
                        "更新于 1 小时内"
                    } else {
                        "更新于 ${Duration.between(arcade.modified, nowTime).toHours()} 小时前"
                    })
                    append(")")
                }
            )
        }
        appendLine()
        appendLine("更新数据请使用“机厅名+数量”的格式，如 “jt3” 或 “jt+1” 或 “jt-1”。")
    }
    suspend fun handle(event: GroupMessageEvent) = event.run {
        val command = message.content.trim().lowercase()
        transactionWithLock {
            val groupId = ArcadeCenterQueueGroup.findById(group.id.toString()) ?: run {
                if (command in arrayOf("几", "j", "机厅几", "/j"))
                    reply("当前群未绑定任何机厅。可以使用“排卡管理”命令来设置。")
                return@transactionWithLock
            }
            val centers = ArcadeQueueGroup.findById(groupId.group) ?.centers ?: run {
                if (command in arrayOf("几", "j", "机厅几", "/j"))
                    reply("当前群未绑定任何机厅。可以使用“排卡管理”命令来设置")
                return@transactionWithLock
            }
            if (command in arrayOf("几", "j", "机厅几", "/j")) {
                clear()
                reply(getQueueInfo(centers.toList()))
                return@transactionWithLock
            }
            centers.forEach { arcade ->
                arcade.abbr.split(",").forEach names@{ name ->
                    if (!command.startsWith(name))
                        return@names
                    if ("几" in command.substringAfter(name) && command.substringBefore("几") == name) {
                        clear()
                        reply(getQueueInfo(listOf(arcade)))
                        return@transactionWithLock
                    }
                    var newValue = when {
                        command.startsWith("$name+") -> {
                            arcade.value + command.substringAfter("${name}+").filter { it.isDigit() }.toInt()
                        }
                        command.startsWith("$name-") -> {
                            arcade.value - command.substringAfter("${name}-").filter { it.isDigit() }.toInt()
                        }
                        else -> {
                            try {
                                command.substringAfter(name).replace("=", "").toInt()
                            } catch (e: Exception) {
                                return@names
                            }
                        }
                    }
                    if (newValue > 50) {
                        reply("机厅很小，请你忍一忍")
                        return@names
                    }
                    if (newValue < 0)
                        newValue = 0
                    arcade.value = newValue
                    arcade.modified = LocalDateTime.now()
                    reply("更新成功，现在${arcade.name}人数为${newValue}人。")
                }
            }
        }
    }
}