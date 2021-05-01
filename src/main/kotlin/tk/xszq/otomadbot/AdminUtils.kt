@file:Suppress("unused")
package tk.xszq.otomadbot

import com.soywiz.korio.file.baseName
import com.soywiz.korio.file.std.localVfs
import com.soywiz.korio.file.std.openAsZip
import com.soywiz.korio.file.std.toVfs
import com.soywiz.korio.util.UUID
import kotlinx.coroutines.delay
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.message.nextMessage
import net.mamoe.mirai.utils.MiraiExperimentalApi
import net.mamoe.mirai.utils.MiraiInternalApi
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import tk.xszq.otomadbot.api.GithubApi
import tk.xszq.otomadbot.api.QNAPApi
import tk.xszq.otomadbot.database.*
import tk.xszq.otomadbot.media.downloadFile
import tk.xszq.otomadbot.media.getFile
import java.io.File

@Suppress("UNUSED_PARAMETER")
object AdminUtils: CommandUtils("admin") {
    @MiraiExperimentalApi
    @MiraiInternalApi
    @AdminCommand
    suspend fun reload(args: Args, event: MessageEvent) = event.run {
        try {
            doInit()
            quoteReply("重载成功")
        } catch (e: Exception) {
            quoteReply("重载失败：" + e.stackTraceToString())
        }
    }
    @AdminCommand
    suspend fun image(args: Args, event: MessageEvent) = event.run {
        when (if (args.isNotEmpty()) args[0] else "") {
            "add" -> {
                if (args.size == 1) {
                    quoteReply("请注明类别")
                } else {
                    val type = args[1]
                    quoteReply("请发送欲添加的图片：")
                    val msg = nextMessage()
                    msg.forEach { pic ->
                        if (pic is Image) {
                            val id = UUID.randomUUID().toString()
                            val file = pic.getFile()
                            file.renameTo(File("$pathPrefix/image/$type/$id"))
                            doInsertIntoH2Database(type, file)
                            if (replyPic.isDirIncluded(type))
                                replyPic.insert(type, file.absolutePath)
                        }
                    }
                    quoteReply("添加成功")
                }
            }
            else -> quoteReply("未知指令")
        }
    }
    @AdminCommand
    suspend fun say(args: Args, event: MessageEvent) = event.run {
        var targetGroup = mutableListOf<Group>()
        if (args.isEmpty()) {
            quoteReply("请指定消息类型")
        } else {
            if (when (args[0]) {
                    "all" -> {
                        targetGroup = bot.groups.toMutableList(); true
                    }
                    "include" -> {
                        bot.groups.forEach { group ->
                            if (group.id.toString() in args) targetGroup.add(group)
                        }
                        true
                    }
                    "exclude" -> {
                        bot.groups.forEach { group ->
                            if (group.id.toString() !in args) targetGroup.add(group)
                        }
                        true
                    }
                    else -> { quoteReply("未知发送模式"); false }
                }) {
                quoteReply("请发送欲让bot发送的消息内容：")
                val targetMessage = nextMessage()
                subject.sendMessage(targetMessage)
                delay(500)
                var groupList = ""
                targetGroup.forEach { group -> groupList += "${group.name}(${group.id}), " }
                subject.sendMessage("以下群组将会收到消息：$groupList\n请确认接收主体（Y/N）")
                if (nextMessage().content.toLowerCase() == "y") {
                    targetGroup.forEach { group ->
                        for (i in 0..10) try {
                            group.sendMessage(targetMessage)
                            break
                        } catch (e : Exception) {
                        }
                    }
                }
            }
            pass
        }
    }
    @AdminCommand
    suspend fun ban(args: Args, event: MessageEvent) = event.run {
        if (args.isNotEmpty()) {
            try {
                val target = -1L * args[0].toLong()
                newSuspendedTransaction(db = Databases.mysql) {
                    if (Permissions.select {
                            Permissions.name eq "^forbid\\.reply\\.answer" and
                                    (Permissions.subject eq target)
                        }.count() == 0L) {
                        Permissions.insert {
                            it[name] = "^forbid\\.reply\\.answer"
                            it[subject] = target
                            it[enabled] = true
                        }
                    } else {
                        Permissions.update({
                            Permissions.name eq "^forbid\\.reply\\.answer" and
                                    (Permissions.subject eq target)
                        }) {
                            it[enabled] = true
                        }
                    }
                }
                Databases.refreshCache()
                quoteReply("禁言成功")
            } catch (e: Exception) {
                quoteReply("禁言失败")
            }
        }
    }
    @AdminCommand
    suspend fun upgrade(args: Args, event: MessageEvent) = event.run {
        val enableProxy = args.getOrElse(0){""}.toLowerCase() != "n"
        try {
            val api = GithubApi(enableProxy)
            val latest = api.getArtifacts(configMain.github.repository).artifacts.first().archive_download_url
            downloadFile(latest, "OtomadBot.zip", "", pathPrefix, api.requiredHeaders,
                enableProxy).toVfs().openAsZip { zip ->
                val file = zip.listSimple().first()
                file.copyTo(localVfs("$pathPrefix/${file.baseName}"))
            }
            quoteReply("升级成功，正在重启……")
            QNAPApi().restart(configMain.qnap.dockerId)
        } catch (e: Exception) {
            quoteReply("升级失败：" + e.stackTraceToString())
        }
    }
    @AdminCommand
    suspend fun tempdir(args: Args, event: MessageEvent) = event.run {
        quoteReply(tempDir)
    }
    @AdminOrGroupAdminCommand("/自动问答")
    suspend fun managereply(args: Args, event: GroupMessageEvent) = event.run {
        when (args.firstOrNull() ?: "") {
            "查看" -> {
                newSuspendedTransaction(db = Databases.mysql) {
                    var result = ""
                    ReplyRules.getRulesBySubject(event.group.id).forEach {
                        result += "[${ReplyRule.getNameFromType(it.type)}] #${it.id} " +
                                "\"${it.rule}\": \"${it.reply.escape()}\"\n"
                    }
                    quoteReply(result.ifBlank { "暂无规则" })
                }
            }
            "查看全局" -> {
                event.requireSender("admin.reply.global") {
                    newSuspendedTransaction(db = Databases.mysql) {
                        var result = ""
                        ReplyRules.getRulesBySubject(-1).forEach {
                            result += "[${ReplyRule.getNameFromType(it.type)}] #${it.id} " +
                                    "\"${it.rule}\": \"${it.reply.escape()}\"\n"
                        }
                        quoteReply(result)
                    }
                } ?: run {
                    quoteReply("权限不足，仅bot管理者允许查看")
                }
            }
            "新增" -> {
                args.getOrNull(1)?.let { rawType ->
                    ReplyRule.getTypeFromName(rawType)?.let { type ->
                        quoteReply("请发送匹配规则：")
                        val rule = event.nextMessage()
                        group.sendMessage(rule.quote() + "请发送回复内容（仅支持纯文本，同时请勿发送单独一条链接）：")
                        val reply = event.nextMessage()
                        ReplyRules.insertRule(rule.contentToString(), reply.contentToString(), event.group.id, type,
                            sender.id)
                        group.sendMessage(reply.quote() + "新增成功")
                    } ?: run {
                        quoteReply("未知类型，请输入“/自动回复 类型”" +
                                "查看所有支持的类型")
                    }
                } ?: run {
                    quoteReply("用法：/自动问答 新增 类型")
                }
            }
            "删除" -> {
                if (args.size < 2) {
                    quoteReply("用法：/自动问答 删除 编号")
                } else {
                    ReplyRules.getRuleById(args[1].toInt())?.let {
                        if (it.group == -1L) {
                            require("admin.reply.global") {
                                try {
                                    newSuspendedTransaction(db = Databases.mysql) {
                                        it.delete()
                                    }
                                    quoteReply("删除成功")
                                } catch (e: Exception) {
                                    quoteReply("删除失败")
                                }
                            } ?: run { quoteReply("权限不足") }
                        } else {
                            try {
                                newSuspendedTransaction(db = Databases.mysql) {
                                    it.delete()
                                }
                                quoteReply("删除成功")
                            } catch (e: Exception) {
                                quoteReply("删除失败")
                            }
                        }
                    } ?: run {
                        quoteReply("规则不存在")
                    }
                }
            }
            "类型" -> {
                quoteReply("目前支持的类型有7种：\n" +
                        "包含：匹配包含该关键词的聊天内容进行回复。\n" +
                        "全等：匹配与规则内容完全一致的聊天内容。\n" +
                        "正则：按正则模式匹配聊天内容，前后无需加/斜杠。\n" +
                        "任含：聊天内容包含规则指定的关键词中的任意一个即可。设置时需指定以\",\"分隔的关键词列表。\n" +
                        "全含：聊天内容包含规则指定的所有关键词。设置时需指定以\",\"分隔的关键词列表。\n" +
                        "图片包含：将图片上的文本OCR识别后按包含模式匹配。\n" +
                        "图片全含：图片上包含规则中所有指定的关键词时回复。\n" +
                        "图片任含：将图片上的文本OCR识别后按任含模式匹配。\n")
            }
            else -> {
                quoteReply("用法：/自动问答 指令 参数(可选)\n支持的指令有：查看、新增、删除、类型。")
            }
        }
    }
    @AdminCommand
    suspend fun xmltest(args: Args, event: MessageEvent) = event.run {
        val next = nextMessage()
        val xml = next.toXML()
        quoteReply(xml)
        quoteReply(xml.xmlToMessageChain(subject))
    }
    @AdminCommand
    suspend fun debug(args: Args, event: MessageEvent) = event.run {
        debugMode = !debugMode
        quoteReply("当前调试模式：" + if (debugMode) "开启" else "关闭")
    }
    @AdminCommand
    suspend fun perm(args: Args, event: MessageEvent) = event.run {
        when (args.firstOrNull()?.toLowerCase()) {
            "query" -> {
                args.getOrNull(1) ?.let { permission ->
                    args.getOrNull(2) ?. let { subject ->
                        quoteReply("isAllowed: " + isAllowed(permission, subject.toLong()))
                    }
                }
            }
            "refresh" -> {
                try {
                    Databases.refreshCache()
                    quoteReply("权限重载成功。")
                } catch (e: Exception) {
                    quoteReply("重载失败：\n" + e.stackTraceToString())
                }
            }
            else -> quoteReply("未知命令")
        }
    }
    @AdminCommand
    suspend fun restart(args: Args, event: MessageEvent) = event.run {
        QNAPApi().restart(configMain.qnap.dockerId)
    }
}