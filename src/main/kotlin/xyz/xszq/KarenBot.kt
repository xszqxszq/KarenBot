package xyz.xszq

import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import korlibs.image.format.PNG
import korlibs.image.format.encode
import korlibs.image.format.readNativeImage
import korlibs.io.async.launch
import korlibs.io.file.std.localCurrentDirVfs
import korlibs.io.file.std.toVfs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withPermit
import nu.pattern.OpenCV
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import xyz.xszq.bot.dao.AccessLogs
import xyz.xszq.bot.audio.*
import xyz.xszq.bot.config.BinConfig
import xyz.xszq.bot.config.BotConfig
import xyz.xszq.bot.config.PJSKConfig
import xyz.xszq.bot.dao.*
import xyz.xszq.bot.ffmpeg.FFMpegTask
import xyz.xszq.bot.image.*
import xyz.xszq.bot.maimai.Maimai
import xyz.xszq.bot.maimai.QueueForArcades
import xyz.xszq.bot.text.*
import xyz.xszq.nereides.*
import xyz.xszq.nereides.event.GlobalEventChannel
import xyz.xszq.nereides.event.GroupAtMessageEvent
import xyz.xszq.nereides.event.GuildAtMessageEvent
import xyz.xszq.nereides.message.*
import xyz.xszq.nereides.message.ark.ListArk
import xyz.xszq.nereides.payload.message.MessageKeyboard
import kotlin.random.Random

lateinit var mariadb: Database
lateinit var mongoClient: MongoClient
lateinit var mongodb: MongoDatabase
lateinit var config: BotConfig
lateinit var binConfig: BinConfig

fun initMongo() {
    mongoClient = MongoClient.create(config.mongoUrl)
    mongodb = mongoClient.getDatabase("karenbot")
    AccessLogs.collection = mongodb.getCollection<AccessLog>("accessLog")
}

suspend fun init() {
    OpenCV.loadLocally()
    config = BotConfig.load(localCurrentDirVfs["config.yml"])

    binConfig = BinConfig.load(localCurrentDirVfs["bin.yml"])
    FFMpegTask.ffmpegBin = binConfig.ffmpeg
    FFMpegTask.ffmpegPath = binConfig.ffmpegPath
    FFMpegTask.checkFFMpeg()

    PJSKSticker.config = PJSKConfig.load(localCurrentDirVfs["image/pjsk/characters.json"])

    mariadb = Database.connect(config.databaseUrl, driver = "org.mariadb.jdbc.Driver",
        config.databaseUser, config.databasePassword)
    initMongo()

    launch(Dispatchers.IO) {
        QueueForArcades.init()
    }

    BuildImage.init()

    RandomImage.load("reply")
    RandomImage.load("gif", "reply")
    RandomImage.load("afraid", "reply")

    Maimai.initBlocking()
    RandomText.loadSaizeriya()
}

fun subscribe() {
    GlobalEventChannel.subscribePublicMessages {
        always {
            launch(Dispatchers.IO) {
                AccessLogs.saveLog(subjectId, contextId, contentString)
            }
        }
    }
    GlobalEventChannel.subscribePublicMessages {
        startsWith("/ping") {
            reply("bot在")
        }
        startsWith("/搜番") {
            if (this is GroupAtMessageEvent) {
                message.filterIsInstance<RemoteImage>().firstOrNull() ?.let { img ->
                    img.getFile().apply {
                        reply(AnimeDB.handle(this))
                    }.also {
                        it.delete()
                    }
                } ?: run {
                    reply("使用搜番命令时，请同时发送想要搜索的动漫截图！")
                }
            }
        }
        always {
            AutoQA.handle(this)
        }
        startsWith(listOf("av", "BV", "https://b23.tv", "b23.tv")) {
            reply(Bilibili.getVideoDetails(message.text))
        }
        startsWith(listOf("help", "/help", "!help", "帮助", "/帮助")) {
            val help = localCurrentDirVfs["image/help.png"].toImage()
            if (config.auditMode) {
                reply(help)
            } else {
                reply(help + localCurrentDirVfs["image/QR.png"].toImage())
            }
        }
    }
    Maimai.subscribe()
    GlobalEventChannel.subscribePublicMessages(permName = "arcade") {
        startsWith("/排卡管理") { raw ->
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
                    val queueGroup = QueueForArcades.getQueueGroup(contextId)
                    transactionWithLock {
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
                    val queueGroup = QueueForArcades.getQueueGroup(contextId)
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
                        reply("使用方法：/排卡管理 添加别名 机厅名称 机厅别名")
                        return@startsWith
                    }
                    val alias = args[2]
                    val queueGroup = QueueForArcades.getQueueGroup(contextId)
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
                        reply("使用方法：/排卡管理 删除别名 机厅名称 机厅别名")
                        return@startsWith
                    }
                    val alias = args[2]
                    transactionWithLock {
                        val queueGroup = QueueForArcades.getQueueGroup(contextId)
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
                        val queueGroup = QueueForArcades.getQueueGroup(contextId)
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
    }
    GlobalEventChannel.subscribePublicMessages(permName = "audio.touhou") {
        startsWith(listOf("随机东方原曲", "/随机东方原曲")) {
            if (this !is GroupAtMessageEvent)
                return@startsWith
            val duration = 15.0
            val file = RandomMusic.get("touhou")
            file.cropPeriod(Random.nextDouble(
                0.0,
                file.getAudioDuration() - duration
            ), duration)?.let { v ->
                reply(v.toVoice())
            }
        }
        startsWith(listOf("原曲认知测验", "/原曲认知测验")) { raw ->
            if (this !is GroupAtMessageEvent)
                return@startsWith
            val args = raw.toArgsList()
            if (args.isEmpty()) {
                reply(buildString {
                    appendLine("使用方法：/原曲认知测验 [难度] [范围]")
                    appendLine("\t例：/原曲认知测验 easy")
                    appendLine("\t例：/原曲认知测验 lunatic 心绮楼后")
                    appendLine("目前支持的难度：easy normal hard lunatic")
                    appendLine("目前支持的范围：辉针城前 心绮楼后 全部")
                })
                return@startsWith
            }
            val difficulty = when (args[0].lowercase()) {
                "easy" -> TouhouGuessGame.Difficulty.Easy
                "normal" -> TouhouGuessGame.Difficulty.Normal
                "hard" -> TouhouGuessGame.Difficulty.Hard
                "lunatic" -> TouhouGuessGame.Difficulty.Lunatic
                else -> {
                    reply("格式有误，请使用 /原曲认知测验 查看说明。")
                    return@startsWith
                }
            }
            val range = when {
                args.size < 2 || args[1] == "全部" -> TouhouGuessGame.Range.AllNew
                args[1] == "辉针城前" -> TouhouGuessGame.Range.BeforeKishinjou
                args[1] == "心绮楼后" -> TouhouGuessGame.Range.AfterShinkirou
                args[1] == "弹幕作" -> TouhouGuessGame.Range.STGOnly
                args[1] == "旧作" -> TouhouGuessGame.Range.Old
                else -> {
                    reply("格式有误，请使用 /原曲认知测验 查看说明。")
                    return@startsWith
                }
            }
            launch(Dispatchers.IO) {
                TouhouGuessGame.start(this, difficulty, range)
            }
        }
    }
    GlobalEventChannel.subscribePublicMessages(permName = "image.generate") {
        startsWith(listOf("生成", "/生成")) { raw ->
            val args = raw.toArgsList()
            val images = when (this) {
                is GroupAtMessageEvent -> message.filterIsInstance<RemoteImage>().map {
                    it.getFile().readNativeImage()
                }
                is GuildAtMessageEvent -> {
                    message.filterIsInstance<GuildAt>().filter { it.target != bot.botGuildInfo.id }
                        .map { it.user.avatar }.ifEmpty { listOf(author.avatar) }.map {
                            NetworkUtils.downloadTempFile(it)!!.toVfs().readNativeImage()
                        }
                }
                else -> return@startsWith
            }
            if (args.isEmpty() || (args.size == 1 && args.first() == "帮助")) {
                reply(ListArk.build {
                    desc { "生成功能帮助" }
                    prompt { "生成功能帮助" }
                    text { "这是一个制作表情包的功能。您需要向机器人提供文本/图片作为输入来制作表情包。" }
                    text { "指令格式：”/生成 模式“" }
                    link("https://otmdb.cn/karenbot/meme") { "点我查看功能列表及表情包预览" }
                    text { "使用”/生成 帮助 [模式]“来查看该模式的帮助说明。" }
                    if (this@startsWith is GuildAtMessageEvent) {
                        text { "由于频道机器人无法接收图片，因此在使用本命令时，可以@自己或别人来将头像作为参数传入。" }
                    }
                })
                return@startsWith
            }
            when (args[0]) {
                "对称" -> {
                    val img = images.firstOrNull() ?: run {
                        reply("使用生成命令时，请同时发送一张图片！")
                        return@startsWith
                    }
                    reply(MessageChain(ImageGeneration.flipImage(img).map {
                        it.toImage()
                    }))
                }
                "球面化" -> {
                    val img = images.firstOrNull() ?: run {
                        reply("使用生成命令时，请同时发送一张图片！")
                        return@startsWith
                    }
                    reply(ImageGeneration.spherize(img).toImage())
                }
                "反球面化" -> {
                    val img = images.firstOrNull() ?: run {
                        reply("使用生成命令时，请同时发送一张图片！")
                        return@startsWith
                    }
                    reply(ImageGeneration.pincushion(img).toImage())
                }
                "5k" -> {
                    val nowArgs = raw.trim().substringAfter("5k").toArgsListByLn()
                    when (nowArgs.size) {
                        0 -> reply("使用方法：\n/生成 5k 第一行文本\n第二行文本（可选）")
                        1 -> reply(FiveThousandChoyen.generate(nowArgs.first().trim(), " ").encode(PNG).toImage())
                        else -> reply(FiveThousandChoyen.generate(nowArgs[0].trim(), nowArgs[1].trim()).encode(PNG).toImage())
                    }
                }
                "蔚蓝档案logo" -> {
                    reply("使用方法：\n/ba 左侧文本 右侧文本")
                }
                "ba" -> {
                    val nowArgs = raw.trim().substringAfter("ba").toArgsList()
                    when (nowArgs.size) {
                        2 -> reply(BlueArchiveLogo.draw(nowArgs[0].trim(), nowArgs[1].trim()).encode(PNG).toImage())
                        else -> reply("使用方法：\n/ba 左侧文本 右侧文本")
                    }
                }
                "帮助" -> {
                    reply(when(val type = args[1]) {
                        in listOf("对称", "球面化", "反球面化") -> "使用方法：\n/生成 模式 [图片]，使用时请务必发送一张图片！"
                        "5k" -> "使用方法：\n/生成 5k 第一行文本\n第二行文本（可选）"
                        in listOf("蔚蓝档案logo", "ba") -> "使用方法：\n/ba 左侧文本 右侧文本"
                        else -> MemeGenerator.getHelpText(type)
                    }.trimEnd().ifBlank { "未找到该类别" })
                }
                else -> {
                    kotlin.runCatching {
                        reply(MemeGenerator.handle(
                            args.first(),
                            args.subArgsList(),
                            images.map { it.toBuildImage() }
                        ).toImage())
                    }.onFailure {
                        when (it) {
                            is TextOrNameNotEnoughException -> reply("文本长度过短，请使用 /生成 查看帮助。")
                            is TextOverLengthException -> reply("文本长度过长，请缩短后再尝试")
                            is UnsupportedOperationException -> reply("不存在该模式，请使用 /生成 查看帮助。")
                            else -> {
                                val help = MemeGenerator.getHelpText(args.first())
                                reply("使用方法有误，可能缺少了必要的参数（如图片或者文本）。请使用 /生成 查看帮助。\n$help")
                            }
                        }
                    }
                }
            }
        }
        startsWith(listOf("球面化", "/球面化", "我巨爽", "/我巨爽", "反球面化", "/反球面化")) {
            reply("请使用/生成 指令！")
        }
        startsWith("/latex") { text ->
            if (text.isBlank()) {
                reply("用法：/latex LaTeX文本\n例：/latex \\LaTeX")
                return@startsWith
            }
            try {
                val image = LaTeX.generateLaTeX(text)
                reply(image.toImage())
            } catch (e: Exception) {
                if (e is org.scilab.forge.jlatexmath.ParseException) {
                    reply(e.message!!)
                }
            }
        }
        startsWith(listOf(
            "/5k", "5k",
            "/gocho", "gocho",
            "/choyen", "choyen"
        )) { raw ->
            val args = raw.toArgsListByLn()
            when (args.size) {
                0 -> reply("使用方法：\n/5k 第一行文本\n第二行文本（可选）\n\n例：/5k 干什么！")
                1 -> reply(FiveThousandChoyen.generate(args.first().trim(), " ").encode(PNG).toImage())
                else -> reply(FiveThousandChoyen.generate(args[0].trim(), args[1].trim()).encode(PNG).toImage())
            }
        }
        startsWith("/ba") { raw ->
            val args = raw.toArgsList()
            when (args.size) {
                2 -> reply(BlueArchiveLogo.draw(args[0].trim(), args[1].trim()).encode(PNG).toImage())
                else -> reply("使用方法：\n/ba 左侧文本 右侧文本\n例：/ba Blue Archive")
            }
        }
    }
    GlobalEventChannel.subscribePublicMessages(permName = "text.stereotypes") {
        startsWith(listOf("发病", "/发病")) { name ->
            if (name.isBlank()) {
                reply("使用方法：/发病 名字\n例：/发病 小冰")
                return@startsWith
            }
            reply(RandomText.randomStereotypes(name))
        }
    }
    GlobalEventChannel.subscribePublicMessages(permName = "audio.voice") {
        startsWith(listOf("活字印刷", "/活字印刷")) { text ->
            if (text.isBlank()) {
                reply("使用方法：/活字印刷 文本\n例：/活字印刷 大家好啊，我是可怜Bot")
                return@startsWith
            }
            audioSemaphore.withPermit {
                kotlin.runCatching {
                    OttoVoice.generate(text) ?.toVoice() ?.let {
                        reply(it)
                    }
                }.onFailure {
                    reply("生成失败，请检查内容是否为中文/英文/数字。")
                }
            }
        }
    }
    GlobalEventChannel.subscribePublicMessages(permName = "random") {
        startsWith(listOf("/萨吃什么", "萨吃什么")) {
            reply(RandomText.saizeriya())
        }
    }
    GlobalEventChannel.subscribePublicMessages(permName = "wiki") {
        endsWith("是什么") { keyword ->
            reply(WikiQuery.query(keyword) ?: ListArk.build {
                desc { "Wiki 查询结果" }
                prompt { "查询结果" }
                text { "未在音MAD维基/THBWiki/萌娘百科上找到相关内容。" }
            })
        }
    }
    GlobalEventChannel.subscribePublicMessages(permName = "image.random") {
        equalsTo("") {
            reply(RandomImage.getRandom("reply").toImage())
        }
        startsWith(listOf("黄毛", "/黄毛", "来点黄毛", "/来点黄毛", "来张黄毛", "/来张黄毛")) {
            reply(RandomImage.getRandom("reply").toImage())
        }
    }
    GlobalEventChannel.subscribePublicMessages(permName = "image.pjsk") {
        startsWith("/pjsk") { raw ->
            if (PJSKSticker.aliases.none { it.value.any { alias -> raw.trim().startsWith(alias) } }) {
                reply(ListArk.build {
                    desc { "PJSK表情包功能帮助" }
                    prompt { "PJSK帮助" }
                    text { "这是一个生成PJSK（プロセカ）表情包的功能。" }
                    text { "使用方法：/pjsk 角色名+图片编号 文本" }
                    text { "\t例：/pjsk 初音6 已举办" }
                    text { "\t例：/pjsk ena9 喜欢你" }
                    link("https://otmdb.cn/karenbot/pjsk") { "点我查看可选的图片列表" }
                })
                return@startsWith
            }
            val list = raw.split(" ", limit = 2)
            if (list.size < 2) {
                reply("使用方法有误，请使用/pjsk查看说明！")
                return@startsWith
            }
            println("asd")
            val (templateName, text) = list
            val (character, alias) = PJSKSticker.aliases.mapNotNull { (character, aliases) ->
                aliases.forEach { alias ->
                    if (templateName.lowercase().startsWith(alias))
                        return@mapNotNull Pair(character, alias)
                }
                return@mapNotNull null
            }.firstOrNull() ?: run {
                reply("使用方法有误，请使用/pjsk查看说明！")
                return@startsWith
            }
            println(templateName.substringAfter(alias))
            val picId = templateName.substringAfter(alias).toIntOrNull() ?: run {
                reply("使用方法有误，请使用/pjsk查看说明！")
                return@startsWith
            }
            val config = PJSKSticker.config.characters.first {
                it.character == character &&
                        it.name.split(" ").last().toInt() == picId
            }
            reply(PJSKSticker.draw(config, text).savePng().toImage())
        }
    }
//    GlobalEventChannel.subscribePublicMessages(permName = "sleep") {
//        equalsTo(listOf("晚安", "/晚安", "睡觉", "/睡觉")) {
//            WakeSleep.sleep(contextId, subjectId) ?.let {
//                "晚安~你是本${if (this is GroupAtMessageEvent) "群" else "频道"}第${it}个入睡的"
//            } ?: run {
//                "你已经睡过了！"
//            }
//        }
//    }
}
lateinit var bot: Bot
fun main() {
    runBlocking {
        init()
    }
    bot = Bot(
        appId = config.appId,
        clientSecret = config.clientSecret,
        easyToken = config.token,
        sandbox = config.sandbox
    )
    bot.logger.info { "正在设置监听……" }
    subscribe()

    bot.logger.info { "启动 Bot 中……" }
    bot.launch()
}