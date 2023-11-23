package xyz.xszq

import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.encode
import com.soywiz.korio.file.std.localCurrentDirVfs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import xyz.xszq.bot.*
import xyz.xszq.bot.dao.*
import xyz.xszq.bot.maimai.Maimai
import xyz.xszq.nereides.*
import xyz.xszq.nereides.event.GlobalEventChannel
import xyz.xszq.nereides.event.GroupAtMessageEvent

lateinit var database: Database
lateinit var config: BotConfig

suspend fun init() {
    config = BotConfig.load(localCurrentDirVfs["config.yml"])
    database = Database.connect(config.databaseUrl, driver = "org.mariadb.jdbc.Driver",
        config.databaseUser, config.databasePassword)
    QueueForArcades.init()
    RandomImage.load("reply")
    RandomImage.load("gif", "reply")
    RandomImage.load("afraid", "reply")
    Maimai.init()
}

fun subscribe() {
    GlobalEventChannel.subscribeMessages {
        equalsTo("") {
            sendImage(RandomImage.getRandom("reply"))
        }
        startsWith("/ping") {
            reply("bot在")
        }
        startsWith("/latex") { text ->
            if (text.isBlank()) {
                reply("用法：/latex LaTeX文本")
                return@startsWith
            }
            try {
                val image = LaTeX.generateLaTeX(text)
                sendImage(image)
            } catch (e: Exception) {
                if (e is org.scilab.forge.jlatexmath.ParseException) {
                    reply(e.message!!)
                }
            }
        }
        startsWith("/搜番") {
            if (this is GroupAtMessageEvent) {
                val img = attachments.firstOrNull { it.isImage()}
                if (img == null) {
                    reply("使用搜番命令时，请同时发送想要搜索的动漫截图！")
                    return@startsWith
                }
                reply(TraceMoe.doHandleTraceMoe(img.url))
            }
        }
        startsWith("/生成") { raw ->
            val args = raw.toArgsList()
            if (args.isEmpty()) {
                reply("使用方法：/生成 模式\n当前支持的模式如下：\n对称 球面化 反球面化")
                return@startsWith
            }
            if (this !is GroupAtMessageEvent)
                return@startsWith

            val img = attachments.firstOrNull { it.isImage()}
            if (img == null) {
                reply("使用生成命令时，请同时发送一张图片！")
                return@startsWith
            }
            when (args[0]) {
                "对称" -> {
                    ImageGeneration.flipImage(img.url).forEach {
                        sendImage(it)
                        delay(500L)
                    }
                }
                "球面化" -> {
                    sendImage(ImageGeneration.spherize(img.url))
                }
                "反球面化" -> {
                    sendImage(ImageGeneration.pincushion(img.url))
                }
                else -> {
                    reply("当前支持的模式如下：\n对称 球面化 反球面化")
                }
            }
        }
        startsWith("/自助问答") {

        }
        startsWith("/功能管理") {

        }
        startsWith("/排卡管理") { raw ->
            if (this !is GroupAtMessageEvent)
                return@startsWith
            val args = raw.toArgsList()
            if (args.size < 2) {
                reply(buildString {
                    appendLine("本命令可以设置机厅排卡功能。支持的子命令如下：")
                    appendLine("/排卡管理 加入分组 分组名")
                    appendLine("/排卡管理 添加机厅 机厅名称")
                    appendLine("/排卡管理 删除机厅 机厅名称")
                    appendLine("/排卡管理 查看别名 机厅名称")
                    appendLine("/排卡管理 添加别名 机厅名称 机厅别名")
                    appendLine("/排卡管理 删除别名 机厅名称 机厅别名")
                })
                return@startsWith
            }
            val name = args[1]
            when (args[0]) {
                "加入分组" -> {
                    newSuspendedTransaction(Dispatchers.IO) {
                        ArcadeQueueGroup.find {
                            ArcadeQueueGroups.name eq name
                        }.firstOrNull() ?.let { group ->
                            ArcadeCenterQueueGroup.new(groupId) {
                                this.group = group.id
                            }
                            reply("加入成功！")
                        } ?: run {
                            reply("分组不存在")
                        }
                    }
                }
                "添加机厅" -> {
                    newSuspendedTransaction(Dispatchers.IO) {
                        val queueGroup = QueueForArcades.getQueueGroup(groupId)
                        ArcadeCenter.new {
                            this.group = queueGroup.id
                            this.name = name
                            this.abbr = name
                            this.value = 0
                        }
                        reply("添加机厅${name}成功。请使用/排卡管理 设置别名 别名 来添加机厅别名。")
                    }
                }
                "删除机厅" -> {
                    newSuspendedTransaction(Dispatchers.IO) {
                        val queueGroup = QueueForArcades.getQueueGroup(groupId)
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
                        reply("使用方法：/排卡管理 添加别名 机厅名称 机厅别名")
                        return@startsWith
                    }
                    val alias = args[2]
                    newSuspendedTransaction(Dispatchers.IO) {
                        val queueGroup = QueueForArcades.getQueueGroup(groupId)
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
                        reply("使用方法：/排卡管理 删除别名 机厅名称 机厅别名")
                        return@startsWith
                    }
                    val alias = args[2]
                    newSuspendedTransaction(Dispatchers.IO) {
                        val queueGroup = QueueForArcades.getQueueGroup(groupId)
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
                    newSuspendedTransaction(Dispatchers.IO) {
                        val queueGroup = QueueForArcades.getQueueGroup(groupId)
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
        always {
            QueueForArcades.handle(this)
        }
        always {
            if (this is GroupAtMessageEvent)
                AutoQA.handle(this)
        }
        startsWith(listOf(
            "/5k", "/生成5k",
            "5k",  "生成5k",
            "/gocho", "gocho",
            "/choyen", "choyen"
        )) { raw ->
            val args = raw.toArgsListByLn()
            when (args.size) {
                0 -> reply("使用方法：\n/5k 第一行文本\n第二行文本（可选）")
                1 -> sendImage(FiveThousandChoyen.generate(args.first().trim(), " ").encode(PNG))
                else -> sendImage(FiveThousandChoyen.generate(args[0].trim(), args[1].trim()).encode(PNG))
            }
        }
    }
    Maimai.subscribe()
}

fun main() {
    runBlocking {
        init()
    }
    val bot = Bot(
        appId = config.appId,
        clientSecret = config.clientSecret,
        easyToken = config.token
    )
    subscribe()

    bot.launch()
}