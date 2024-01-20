package xyz.xszq.bot.rhythmgame

import kotlinx.coroutines.runBlocking
import xyz.xszq.bot.dao.ArcadeCenter
import xyz.xszq.bot.dao.ArcadeCenterQueueGroup
import xyz.xszq.bot.dao.ArcadeQueueGroup
import xyz.xszq.bot.dao.transactionWithLock
import xyz.xszq.nereides.event.PublicMessageEvent
import xyz.xszq.nereides.isSameDay
import xyz.xszq.nereides.message.ark.ListArk
import java.time.Duration
import java.time.LocalDateTime

object QueueForArcades {
    private val initTime = LocalDateTime.of(2000, 1, 1, 0, 0)
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
    fun init() {
        clear()
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
    private suspend fun getQueueInfo(centers: List<ArcadeCenter>) = ListArk.build {
        val nowTime = LocalDateTime.now()
        desc { "机厅排卡" }
        prompt { "机厅排卡" }
        text { "机厅排卡人数：" }
        text { "" }
        centers.forEach { arcade ->
            text {
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
            }
        }
        text { "" }
        text { "更新数据请使用“机厅名+数量”的格式，如 “jt3” 或 “jt+1” 或 “jt-1”。" }
    }
    suspend fun handle(event: PublicMessageEvent) = event.run {
        val command = message.text.trim().lowercase()
        transactionWithLock {
            val groupId = ArcadeCenterQueueGroup.findById(event.contextId) ?: run {
                if (command in arrayOf("几", "j", "机厅几", "/j"))
                    reply("当前群未绑定任何机厅。可以使用 /排卡管理 命令来设置。")
                return@transactionWithLock
            }
            val centers = ArcadeQueueGroup.findById(groupId.group) ?.centers ?: run {
                if (command in arrayOf("几", "j", "机厅几", "/j"))
                    reply("当前群未绑定任何机厅。可以使用 /排卡管理 命令来设置")
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