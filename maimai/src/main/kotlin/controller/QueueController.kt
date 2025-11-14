package xyz.xszq.bot.controller

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import xyz.xszq.bot.*
import xyz.xszq.bot.Maimai.Companion.textMode
import xyz.xszq.bot.database.Arcade
import xyz.xszq.bot.database.ArcadeGroup
import xyz.xszq.bot.database.ArcadeGroupBind
import xyz.xszq.bot.event.GroupMessageEvent
import xyz.xszq.bot.exception.IllegalArgsException
import xyz.xszq.bot.exception.NeedHelpException
import xyz.xszq.bot.exception.NotFoundException
import xyz.xszq.bot.message.MessageElement
import java.time.Duration

@Suppress("unused")
class QueueController(
    override val maimai: Maimai
): Controller(maimai) {
    override fun setRoute() = maimai.route {
        runBlocking {
            clear()
        }
        startsWith("排卡管理") { raw ->
            if (this !is GroupMessageEvent)
                return@startsWith

            runCatching {
                val args = raw.split(" ").filter { it.isNotBlank() }.map { it.trim() }
                if (args.size < 2) {
                    throw NeedHelpException()
                }

                val command = args[0]
                val name = args[1]
                when (command) {
                    "添加机厅" -> add(name)
                    "删除机厅" -> delete(name)
                    "添加别名" -> addAlias(name, args.getOrNull(2))
                    "删除别名" -> deleteAlias(name, args.getOrNull(2))
                    "查看别名" -> aliases(name)
                    "添加分组" -> setGroup(name)
                    else -> throw NeedHelpException()
                }
            }.onFailure { e ->
                when (e) {
                    is NeedHelpException ->
                        if (textMode())
                            reply(helpText)
                        else
                            reply(MarkdownTemplates.Templates.QUEUE_HELP)
                    is IllegalArgsException -> reply(e.message ?: "")
                    is NotFoundException -> reply(e.message ?: "")
                    else -> e.printStackTrace()
                }
            }
        }
        always {
            if (this !is GroupMessageEvent)
                return@always
            handle()
        }
    }
    suspend fun GroupMessageEvent.add(name: String) {
        val group = ArcadeGroupBind.group(group.id)
        if (group.find(name) != null)
            throw IllegalArgsException("机厅已存在！")
        if (name.length > 32)
            throw IllegalArgsException("机厅名称过长！")
        Arcade.new(group, name)
        if (textMode())
            reply("添加机厅成功。")
        else
            reply(MarkdownTemplates.Templates.queue("排卡管理", "添加机厅成功。", name))
    }
    suspend fun GroupMessageEvent.delete(name: String) {
        val arcade = get(name)
        suspendedTransactionAsync {
            arcade.delete()
        }
        if (textMode())
            reply("删除机厅成功。")
        else
            reply(MarkdownTemplates.Templates.queue("排卡管理", "删除机厅成功。", name))
    }
    suspend fun GroupMessageEvent.addAlias(
        name: String,
        raw: String ?= null
    ) {
        val arcade = get(name)
        val aliases = arcade.aliases.split(",").toMutableList()
        val alias = validateAlias(raw)
        if (exists(alias) || alias in aliases)
            throw IllegalArgsException("别名已存在！")

        aliases.add(alias)
        suspendedTransactionAsync {
            arcade.aliases = aliases.joinToString(",")
        }

        if (textMode())
            reply("添加机厅别名成功。")
        else
            reply(MarkdownTemplates.Templates.queue("排卡管理", "添加机厅别名成功。", name))
    }
    suspend fun GroupMessageEvent.deleteAlias(
        name: String,
        raw: String ?= null
    ) {
        val arcade = get(name)
        val aliases = arcade.aliases.split(",").toMutableList()
        val alias = validateAlias(raw)

        aliases.remove(alias)
        suspendedTransactionAsync {
            arcade.aliases = aliases.joinToString(",")
        }

        if (textMode())
            reply("删除机厅别名成功。")
        else
            reply(MarkdownTemplates.Templates.queue("排卡管理", "删除机厅别名成功。", name))
    }
    suspend fun GroupMessageEvent.aliases(
        name: String
    ) {
        val arcade = get(name)
        val aliases = arcade.aliases.split(",").joinToString("，")

        if (textMode())
            reply("机厅别名如下：$aliases")
        else
            reply(MarkdownTemplates.Templates.queue("排卡管理", "机厅别名如下：$aliases", name))
    }
    suspend fun GroupMessageEvent.setGroup(targetName: String) {
        val target = ArcadeGroup[targetName] ?: throw IllegalArgsException("该分组不存在。")
        ArcadeGroupBind.bind(group.id, target)

        if (textMode())
            reply("设置分组成功。")
        else
            reply(MarkdownTemplates.Templates.queue("排卡管理", "设置分组成功。"))
    }
    fun validateAlias(raw: String ?= null): String {
        val alias = raw ?.replace(",", "") ?: throw IllegalArgsException("请输入别名！")
        if (alias.isBlank())
            throw IllegalArgsException("请输入正确的别名！")
        if (alias.length > 32)
            throw IllegalArgsException("别名长度过长！")
        return alias
    }
    suspend fun GroupMessageEvent.get(name: String): Arcade {
        val group = ArcadeGroupBind.group(group.id)
        return group.find(name) ?: throw NotFoundException("机厅不存在！")
    }
    suspend fun GroupMessageEvent.exists(name: String): Boolean {
        val group = ArcadeGroupBind.group(group.id)
        return group.find(name) != null
    }
    private suspend fun clear() = suspendedTransactionAsync {
        Arcade.all().forEach {
            it.clear()
        }
    }
    suspend fun GroupMessageEvent.list(
        arcades: List<Arcade>
    ): MessageElement {
        val nowTime = java.time.LocalDateTime.now()
        if (arcades.size == 1) {
            val nowInfo = status(arcades.first(), nowTime)
            return if (textMode()) nowInfo.toPlainText()
            else MarkdownTemplates.Templates.queue("排卡管理", nowInfo, arcades.first().name)
        }
        val countInfo = arcades.joinToString("\n") { arcade ->
            status(arcade, nowTime)
        }
        return if (textMode())
            buildString {
                appendLine("机厅排卡人数：")
                appendLine()
                appendLine(countInfo)
                appendLine()
                appendLine("更新数据请使用“机厅名+数量”的格式，如 “jt3” 或 “jt+1” 或 “jt-1”。")
            }.toPlainText()
        else
            MarkdownTemplates.Templates.queueUpdate(countInfo)
    }
    fun status(
        arcade: Arcade,
        nowTime: java.time.LocalDateTime,
    ) = buildString {
        append("${arcade.name}: ${arcade.value}人 (")
        append(if (arcade.noUpdates()) {
            "今日未更新数据"
        } else if (Duration.between(arcade.modified.toJavaLocalDateTime(), nowTime).toHours() < 1L){
            "更新于 1 小时内"
        } else {
            "更新于 ${Duration.between(arcade.modified.toJavaLocalDateTime(), nowTime).toHours()} 小时前"
        })
        append(")")
    }
    suspend fun GroupMessageEvent.handle() = suspendedTransactionAsync {
        val group = ArcadeGroupBind.find(group.id)
        val raw = text.trim()
            .substringAfter("/mai")
            .substringAfter("/")
            .replace(" ", "")
            .trim()
        if (raw.endsWith(listOf("j", "几", "几个"))) {
            val name = raw.substringBefore(listOf("j", "几", "几个"))
            // List All
            if (name.isBlank()) {
                if (group == null || group.arcades.count() == 0L) {
                    if (textMode())
                        reply("当前群未设置机厅，请使用“@可怜BOT /排卡管理 添加机厅”来添加机厅。")
                    else
                        reply(MarkdownTemplates.Templates.queueInit(
                            "排卡管理",
                            "当前群未设置机厅，请点击下方按钮添加机厅。"
                        ))
                    return@suspendedTransactionAsync
                }
                group.arcades.forEach { it ->
                    it.clear()
                }
                reply(list(group.arcades.toList()))
                return@suspendedTransactionAsync
            }
            if (group == null)
                return@suspendedTransactionAsync
            group.arcades.firstOrNull { name == it.name || name in it.aliases } ?.let { arcade ->
                arcade.clear()
                reply(list(listOf(arcade)))
            }
            return@suspendedTransactionAsync
        }
        if (group == null)
            return@suspendedTransactionAsync
        group.arcades.forEach { arcade ->
            arcade.aliases.split(",").forEach { alias ->
                if (!raw.startsWith(alias))
                    return@forEach

                var newValue = when {
                    raw.startsWith("$alias+") -> {
                        arcade.value + raw.substringAfter("${alias}+").filter { it.isDigit() }.toInt()
                    }
                    raw.startsWith("$alias-") -> {
                        arcade.value - raw.substringAfter("${alias}-").filter { it.isDigit() }.toInt()
                    }
                    else -> {
                        raw.substringAfter(alias)
                            .replace("=", "")
                            .toIntOrNull() ?: return@suspendedTransactionAsync
                    }
                }
                if (newValue > 50) {
                    reply("机厅很小，请你忍一忍")
                    return@suspendedTransactionAsync
                }
                if (newValue < 0)
                    newValue = 0
                arcade.value = newValue
                arcade.modified = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

                if (textMode())
                    reply("更新成功，现在${arcade.name}人数为${newValue}人。")
                else
                    reply(MarkdownTemplates.Templates.queue(
                        "排卡管理", "更新成功，现在${arcade.name}人数为${newValue}人。",
                        arcade.name
                    ))
            }
        }
    }
    val helpText = buildString {
        appendLine("本功能可以提供机厅人数查询及更新功能，支持的功能命令如下：")
        appendLine("查询人数：@可怜BOT 几 (或者 j)")
        appendLine("修改机厅：@可怜BOT 排卡管理 添加机厅/删除机厅 机厅名称")
        appendLine("机厅别名：@可怜BOT 排卡管理 查看别名/添加别名/删除别名 机厅名称 (别名)")
    }.trim().newLine()
}