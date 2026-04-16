package xyz.xszq.bot.maimai.controller

import kotlinx.coroutines.supervisorScope
import kotlinx.datetime.toJavaLocalDateTime
import xyz.xszq.bot.*
import xyz.xszq.bot.Maimai.Companion.textMode
import xyz.xszq.bot.event.GroupMessageEvent
import xyz.xszq.bot.exception.IllegalArgsException
import xyz.xszq.bot.exception.NeedHelpException
import xyz.xszq.bot.exception.NotFoundException
import xyz.xszq.bot.maimai.component.MarkdownTemplates
import xyz.xszq.bot.maimai.component.MarkdownTemplates.Templates.brief
import xyz.xszq.bot.maimai.database.Arcade
import xyz.xszq.bot.maimai.database.ArcadeGroupBind
import xyz.xszq.bot.maimai.endsWith
import xyz.xszq.bot.maimai.substringBefore
import xyz.xszq.bot.message.Markdown
import xyz.xszq.bot.message.MessageElement
import xyz.xszq.bot.payload.markdown.Keyboard
import xyz.xszq.bot.payload.markdown.MarkdownData
import java.time.Duration

@Suppress("unused")
class QueueController(
    override val maimai: Maimai
): Controller(maimai) {
    override suspend fun setRoute() = maimai.route {
        clear()
        startsWith("排卡管理") { raw ->
            if (this !is GroupMessageEvent)
                return@startsWith

            supervisorScope {
                runCatching {
                    val args = raw.split(" ").filter { it.isNotBlank() }.map { it.trim() }
                    if (args.isEmpty()) {
                        throw NeedHelpException()
                    }

                    val command = args[0]
                    val name = args.getOrNull(1)
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
                    queueErrorHandler(e)
                }
            }
        }
        always {
            if (this !is GroupMessageEvent)
                return@always
            handle()
        }
    }

    val queueErrorHandler: ErrorHandler = { e ->
        when (e) {
            is NeedHelpException -> reply(when(textMode()) {
                true -> helpText.toPlainText()
                else -> brief(
                    "排卡管理",
                    "本功能可以提供机厅人数查询及更新功能，可以点击下方按钮进行操作："
                ).toMessage(Keyboard.create {
                    row {
                        at("查询人数", "几", id = "", enter = true)
                        at("添加机厅", "/排卡管理 添加机厅 ", id = "")
                        at("删除机厅", "/排卡管理 删除机厅 ", id = "", enter = true)
                    }
                    row {
                        at("查看别名", "/排卡管理 查看别名", id = "", enter = true)
                        at("添加别名", "/排卡管理 添加别名", id = "", enter = true)
                        at("删除别名", "/排卡管理 删除别名", id = "", enter = true)
                    }
                })
            })
            is IllegalArgsException -> reply(e.message ?: "")
            is NotFoundException -> reply(e.message ?: "")
            else -> e.printStackTrace()
        }
    }

    suspend fun GroupMessageEvent.add(name: String?) {
        name ?: throw IllegalArgsException("使用方法：/排卡管理 添加机厅 机厅名称")
        if (name.length > 32)
            throw IllegalArgsException("机厅名称过长！")
        ArcadeGroupBind.addArcade(group.id, name)
        if (textMode())
            reply("添加机厅成功。")
        else
            reply(queue("排卡管理", "添加机厅成功。", name))
    }

    suspend fun GroupMessageEvent.delete(name: String?) {
        name ?: run {
            selectArcade("/排卡管理 删除机厅", "请点击需要删除的机厅：", true)
            return
        }
        ArcadeGroupBind.deleteArcade(group.id, name)
        if (textMode())
            reply("删除机厅成功。")
        else
            reply(queue("排卡管理", "删除机厅成功。", name))
    }

    private suspend fun GroupMessageEvent.selectArcade(
        command: String,
        hint: String,
        enter: Boolean = false
    ) {
        val arcades = ArcadeGroupBind.listArcades(group.id) ?: run {
            reply(Markdown(MarkdownData(buildString {
                appendLine("**排卡管理**")
                appendLine()
                appendLine("本群还未添加机厅，请点击下方按钮并输入机厅名称来添加。")
            }), Keyboard.create {
                row {
                    at("添加机厅", "/排卡管理 添加机厅 ", id = "")
                }
            }))
            return
        }
        reply(Markdown(MarkdownData(buildString {
            appendLine("**排卡管理**")
            appendLine()
            appendLine(hint)
            appendLine()
            arcades.map { it.name }.forEach { name ->
                appendLine("> " + MarkdownTemplates.href("$command $name", name, enter = false))
            }
        })))
    }

    suspend fun GroupMessageEvent.addAlias(
        name: String ?= null,
        raw: String ?= null
    ) {
        if (name.isNullOrBlank()) {
            if (textMode())
                reply("请输入别名！")
            else
                selectArcade("/排卡管理 添加别名", "请点击要添加别名的机厅，并输入别名：")
            return
        }
        val alias = validateAlias(raw)
        ArcadeGroupBind.addAlias(group.id, name, alias)

        if (textMode())
            reply("添加机厅别名成功。")
        else
            reply(queue("排卡管理", "添加机厅别名成功。", name))
    }

    suspend fun GroupMessageEvent.deleteAlias(
        name: String ?= null,
        raw: String ?= null
    ) {
        if (name.isNullOrBlank()) {
            if (textMode())
                reply("请输入别名！")
            else
                selectArcade("/排卡管理 删除别名", "请点击要删除别名的机厅，并输入要删除的别名：")
            return
        }
        val alias = validateAlias(raw)
        ArcadeGroupBind.deleteAlias(group.id, name, alias)

        if (textMode())
            reply("删除机厅别名成功。")
        else
            reply(queue("排卡管理", "删除机厅别名成功。", name))
    }

    suspend fun GroupMessageEvent.aliases(
        name: String?
    ) {
        if (name.isNullOrBlank()) {
            if (textMode())
                reply("请输入要查看别名的机厅！")
            else
                selectArcade("/排卡管理 查看别名", "请点击要查看别名的机厅：")
            return
        }
        val aliases = ArcadeGroupBind.aliases(group.id, name).joinToString("，")

        if (textMode())
            reply("机厅别名如下：$aliases")
        else
            reply(queue("排卡管理", "机厅别名如下：$aliases", name))
    }

    suspend fun GroupMessageEvent.setGroup(targetName: String?) {
        targetName ?: throw NeedHelpException()
        ArcadeGroupBind.bind(group.id, targetName)

        if (textMode())
            reply("设置分组成功。")
        else
            reply(queue("排卡管理", "设置分组成功。"))
    }

    fun validateAlias(raw: String ?= null): String {
        val alias = raw ?.replace(",", "") ?: throw IllegalArgsException("请输入别名！")
        if (alias.isBlank())
            throw IllegalArgsException("请输入正确的别名！")
        if (alias.length > 32)
            throw IllegalArgsException("别名长度过长！")
        return alias
    }

    private suspend fun clear() = Arcade.clearAll()

    private suspend fun GroupMessageEvent.list(
        arcades: List<Arcade.Snapshot>
    ): MessageElement {
        val nowTime = java.time.LocalDateTime.now()
        if (arcades.size == 1) {
            val nowInfo = status(arcades.first(), nowTime)
            return if (textMode()) nowInfo.toPlainText()
            else queue("排卡管理", nowInfo, arcades.first().name)
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
            Markdown(MarkdownData(buildString {
                appendLine("**机厅排卡人数：**")
                appendLine()
                appendLine(countInfo.split("\n").joinToString("\n") { "> $it" })
                appendLine("> ")
                appendLine("> 可以点击上方机厅名并输入人数来更新。")
                appendLine("> \t例：某某机厅3")
                appendLine("> \t例：机厅+1")
                append("> \t例：jt-2")
            }))
    }

    private fun status(
        arcade: Arcade.Snapshot,
        nowTime: java.time.LocalDateTime,
    ) = buildString {
        append(MarkdownTemplates.href(arcade.name, arcade.name, enter = false))
        append(": ${arcade.value}人 (")
        append(if (arcade.noUpdates()) {
            "今日未更新数据"
        } else if (Duration.between(arcade.modified.toJavaLocalDateTime(), nowTime).toHours() < 1L) {
            "更新于 1 小时内"
        } else {
            "更新于 ${Duration.between(arcade.modified.toJavaLocalDateTime(), nowTime).toHours()} 小时前"
        })
        append(")")
    }

    suspend fun GroupMessageEvent.handle() {
        val raw = text.trim()
            .substringAfter("/mai")
            .substringAfter("/")
            .replace(" ", "")
            .trim()
        if (raw.endsWith(listOf("j", "几", "几个"))) {
            val name = raw.substringBefore(listOf("j", "几", "几个"))
            if (name.isBlank()) {
                val arcades = ArcadeGroupBind.listArcades(group.id)
                if (arcades.isNullOrEmpty()) {
                    if (textMode())
                        reply("当前群未设置机厅，请使用“@可怜BOT /排卡管理 添加机厅”来添加机厅。")
                    else
                        reply(Markdown(
                            MarkdownData(buildString {
                                appendLine("**排卡管理**")
                                appendLine()
                                appendLine("当前群未设置机厅，请点击下方按钮添加机厅。")
                            }),
                            Keyboard.create {
                                row {
                                    at("添加机厅", "/排卡管理 添加机厅 机厅名称", id = "")
                                }
                            }
                        ))
                    return
                }
                reply(list(arcades))
                return
            }
            ArcadeGroupBind.findArcade(group.id, name) ?.let { arcade ->
                reply(list(listOf(arcade)))
            }
            return
        }
        when (val result = ArcadeGroupBind.updateArcade(group.id, raw)) {
            null -> return
            Arcade.UpdateResult.TooLarge -> reply("机厅很小，请你忍一忍")
            is Arcade.UpdateResult.Updated -> {
                if (textMode())
                    reply("更新成功，现在${result.arcade.name}人数为${result.arcade.value}人。")
                else
                    reply(queue(
                        "排卡管理", "更新成功，现在${result.arcade.name}人数为${result.arcade.value}人。",
                        result.arcade.name
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
    
    private fun queue(
        title: String,
        content: String,
        now: String? = null
    ) = Markdown(MarkdownData("**$title**\n\n$content"), Keyboard.create {
        row {
            at("查询人数", "/j", enter = true, id = "")
            now?.let {
                at("添加别名", "/排卡管理 添加别名 $it 这里填别名", id = "")
            }
            at("更新人数", now ?: "\n", id = "")
        }
    })
}
