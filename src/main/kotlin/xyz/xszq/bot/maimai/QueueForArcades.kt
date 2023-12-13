package xyz.xszq.bot.maimai

import com.soywiz.korio.async.launch
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.xszq.bot.dao.ArcadeCenter
import xyz.xszq.bot.dao.ArcadeCenterQueueGroup
import xyz.xszq.bot.dao.ArcadeQueueGroup
import xyz.xszq.nereides.event.GroupAtMessageEvent
import xyz.xszq.nereides.event.MessageEvent
import xyz.xszq.nereides.event.PublicMessageEvent
import xyz.xszq.nereides.isSameDay
import java.time.Duration
import java.time.LocalDateTime

object QueueForArcades {
    private val initTime = LocalDateTime.of(2000, 1, 1, 0, 0)
    private fun clear() {
        transaction {
            ArcadeCenter.all().forEach {
                if (it.modified == initTime || LocalDateTime.now().isSameDay(it.modified))
                    return@forEach
                it.value = 0
                it.modified = initTime
            }
        }
    }
    fun init() {
        clear()
    }
    suspend fun getQueueGroup(openId: String) = suspendedTransactionAsync(Dispatchers.IO) {
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
    }.await()
    suspend fun handle(event: PublicMessageEvent) = event.run {
        val command = message.text.trim().lowercase()
        newSuspendedTransaction(Dispatchers.IO) {
            val groupId = ArcadeCenterQueueGroup.findById(event.contextId) ?: run {
                if (command in arrayOf("几", "j", "机厅几", "/j"))
                    reply("当前群未绑定任何机厅。可以使用 /排卡管理 命令来设置。")
                return@newSuspendedTransaction
            }
            val centers = ArcadeQueueGroup.findById(groupId.group) ?.centers ?: run {
                if (command in arrayOf("几", "j", "机厅几", "/j"))
                    reply("当前群未绑定任何机厅。可以使用 /排卡管理 命令来设置")
                return@newSuspendedTransaction
            }
            if (command in arrayOf("几", "j", "机厅几", "/j")) {
                clear()
                val nowTime = LocalDateTime.now()
                reply(buildString {
                    appendLine("机厅排卡人数：")
                    centers.forEach { arcade ->
                        appendLine(buildString {
                            append("${arcade.name}: ${arcade.value}人 (")
                            append(if (arcade.modified == initTime) {
                                "今日未更新数据"
                            } else if (Duration.between(arcade.modified, nowTime).toHours() < 1L){
                                "更新于 1 小时内"
                            } else {
                                "更新于 ${Duration.between(arcade.modified, nowTime).toHours()} 小时前"
                            })
                            append(")")
                        })
                    }
                    appendLine()
                    appendLine("更新数据请使用“机厅名+数量”的格式，如 “jt3” 或 “jt+1” 或 “jt-1”。")
                })
                return@newSuspendedTransaction
            }
            centers.forEach { arcade ->
                arcade.abbr.split(",").forEach names@{ name ->
                    if (!command.startsWith(name))
                        return@names
                    if ("几" in command.substringAfter(name) && command.substringBefore("几") == name) {
                        clear()
                        val nowTime = LocalDateTime.now()
                        reply(buildString {
                            append(arcade.name)
                            append("现在${arcade.value}人 (")
                            if (arcade.modified == initTime) {
                                append("今日未更新数据")
                            } else if (Duration.between(arcade.modified, nowTime).toHours() < 1L) {
                                append("更新于 1 小时内")
                            } else {
                                append("更新于 ${Duration.between(arcade.modified, nowTime).toHours()} 小时前")
                            }
                            append(")\n更新数据请使用“机厅名+数量”的格式，如 “jt3” 或 “jt+1” 或 “jt-1”。")
                        })
                        return@newSuspendedTransaction
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