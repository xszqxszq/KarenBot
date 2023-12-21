package xyz.xszq.bot.maimai

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import korlibs.image.format.PNG
import korlibs.image.format.encode
import korlibs.io.async.launch
import korlibs.io.file.std.localCurrentDirVfs
import korlibs.io.lang.substr
import korlibs.memory.toIntCeil
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.upsert
import xyz.xszq.bot.dao.MaimaiBinding
import xyz.xszq.bot.dao.MaimaiSettings
import xyz.xszq.bot.dao.Permissions
import xyz.xszq.bot.dao.transactionWithLock
import xyz.xszq.bot.maimai.MaimaiUtils.dailyOps
import xyz.xszq.bot.maimai.MaimaiUtils.difficulties
import xyz.xszq.bot.maimai.MaimaiUtils.getPlateByFilename
import xyz.xszq.bot.maimai.MaimaiUtils.getPlateFilename
import xyz.xszq.bot.maimai.MaimaiUtils.getPlateVerList
import xyz.xszq.bot.maimai.MaimaiUtils.levels
import xyz.xszq.bot.maimai.MaimaiUtils.name2Difficulty
import xyz.xszq.bot.maimai.MaimaiUtils.plateCategories
import xyz.xszq.bot.maimai.MaimaiUtils.recordCategories
import xyz.xszq.bot.maimai.MaimaiUtils.versionsBrief
import xyz.xszq.bot.maimai.payload.ChartNotes
import xyz.xszq.bot.maimai.payload.PlateResponse
import xyz.xszq.bot.maimai.payload.PlayerData
import xyz.xszq.nereides.event.GlobalEventChannel
import xyz.xszq.nereides.event.GroupAtMessageEvent
import xyz.xszq.nereides.event.MessageEvent
import xyz.xszq.nereides.message.ark.ListArk
import xyz.xszq.nereides.message.plus
import xyz.xszq.nereides.message.toImage
import xyz.xszq.nereides.message.toPlainText
import xyz.xszq.nereides.toArgsList
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

object Maimai {
    private val logger = KotlinLogging.logger("Maimai")

    val musics = MusicsInfo(logger)
    val prober = DXProberClient(logger)
    val images = MaimaiImage(musics, logger, localCurrentDirVfs["maimai"])
    val aliases = Aliases(musics)
    val guessGame = GuessGame(musics, images, aliases)
    private lateinit var config: MaimaiConfig
    private suspend fun queryBindings(openId: String): Pair<String, String>? = transactionWithLock {
        val bindings = MaimaiBinding.findById(openId) ?: return@transactionWithLock null
        Pair(bindings.type, bindings.credential)
    }
    private suspend fun updateBindings(openId: String, type: String, credential: String) = transactionWithLock {
        MaimaiBinding.findById(openId) ?.let {
            it.type = type
            it.credential = credential
        } ?: run {
            MaimaiBinding.new(openId) {
                this.type = type
                this.credential = credential
            }
        }
    }
    suspend fun initBlocking() {
        config = MaimaiConfig.load(localCurrentDirVfs["maimai/settings.yml"])

        logger.info { "正在获取歌曲数据……" }
        musics.updateMusicInfo(prober.getMusicList())
        musics.updateStats(prober.getChartStat())
        musics.updateHot()

        logger.info { "正在更新别名数据……" }
        kotlin.runCatching {
            aliases.updateXrayAliases(config.xrayAliasUrl)
        }.onFailure {
            it.printStackTrace()
            logger.error { "别名更新失败！" }
        }.onSuccess {
            logger.error { "别名更新成功！" }
        }

        logger.info { "正在缓存歌曲封面中……" }
        images.downloadCovers(config, musics.getAll())

        logger.info { "正在加载图片中……" }
        images.load(config.theme)

        logger.info { "正在生成定数表中……" }
        images.preGenerateDsList()
        logger.info { "maimai 功能加载完成。" }
    }
    @OptIn(DelicateCoroutinesApi::class)
    fun init() {
        GlobalScope.launch {
            initBlocking()
        }
        GlobalScope.launch {
            delay(3600 * 1000L)
            kotlin.runCatching {
                aliases.updateXrayAliases(config.xrayAliasUrl)
            }.onFailure {
                it.printStackTrace()
                logger.error { "别名定时更新失败！" }
            }.onSuccess {
                logger.error { "别名定时更新成功！" }
            }
        }
    }
    fun testLoad() {
        runBlocking {
            config = MaimaiConfig.load(localCurrentDirVfs["maimai/settings.yml"])

            logger.info { "正在获取歌曲数据……" }
            musics.updateMusicInfo(prober.getMusicList())

            logger.info { "正在加载图片中……" }
            images.load(config.theme)
            logger.info { "maimai 功能加载完成。" }
        }
    }
    private suspend fun getCredential(arg: String, event: MessageEvent): Pair<String, String>? = event.run {
        if (arg.isNotBlank()) Pair("username", arg)
        else queryBindings(subjectId) ?: run {
            reply(buildString {
                appendLine("未绑定账号信息，请指定用户名，或先进行绑定操作！")
                appendLine("您可以使用 /mai bind 绑定查分器账号进行快速查询 (此绑定与查分器绑定无关，在查分器绑定之后仍需在Bot这里绑定一次)")
            }.trimEnd())
            null
        }
    }
    private suspend fun getVersionData(version: String, arg: String, event: MessageEvent): PlateResponse? = event.run {
        val (credentialType, credential) = getCredential(arg, event) ?: return@run null
        val (status, data) = prober.getDataByVersion(credentialType, credential, getPlateVerList(version))
        when (status) {
            HttpStatusCode.BadRequest ->
                reply("绑定的账号/指定的用户名不存在，请确认用户名对应的玩家在 Diving-Fish 的舞萌 DX 查分器上已注册")
            HttpStatusCode.Forbidden ->
                reply("该玩家已禁止他人查询。如果是您本人账号且已绑定QQ号，请不带用户名再次尝试查询一次")
        }
        if (data == null || status != HttpStatusCode.OK)
            return@run null
        data
    }
    private suspend fun getPlayerData(arg: String, event: MessageEvent): PlayerData? = event.run {
        val (credentialType, credential) = getCredential(arg, event) ?: return@run null
        val (status, data) = prober.getPlayerData(credentialType, credential)
        when (status) {
            HttpStatusCode.BadRequest ->
                reply("用户名不存在，请确认用户名对应的玩家在 Diving-Fish 的舞萌 DX 查分器上已注册")
            HttpStatusCode.Forbidden ->
                reply("该玩家已禁止他人查询。如果是您本人账号且已绑定QQ号，请不带用户名再次尝试查询一次")
        }
        if (data == null || status != HttpStatusCode.OK)
            return@run null
        data
    }
    fun subscribe() {
        GlobalEventChannel.subscribePublicMessages {
            equalsTo("/mai") {
                reply(buildString {
                    appendLine("此指令可查询 maimai 相关信息。")
                    append("当前支持的子指令：查歌 bind b50 ap50 id info 是什么歌 有什么别名 添加别名 删除别名 定数查歌 分数线 进度")
                    append(" 完成表 分数列表 随个 mai什么推分 猜歌 舞萌开字母 设置猜歌 今日舞萌 设置b50 谱师查歌 正则查歌")
                })
            }
        }
        GlobalEventChannel.subscribePublicMessages("/mai", permName = "maimai") {
            startsWith("bind") { raw ->
                val args = raw.toArgsList()
                if (args.isEmpty()) {
                    reply(buildString {
                        appendLine("使用方法（以下两种二选一，建议绑qq）：")
                        appendLine("/mai bind qq号。例：/mai bind 123456")
                        appendLine("/mai bind 用户名。例：/mai bind maxscore")
                        appendLine("也可以使用“/mai bind qq qq号”或者“/mai bind username 用户名”来指名绑定的类型。")
                        appendLine()
                        appendLine("（常见问题：为什么需要再在机器人这里绑定一次QQ号？答：因为机器人获取不到QQ号！！获取不到！！）")
                    })
                    return@startsWith
                }
                when (args[0]) {
                    "qq" -> updateBindings(subjectId, "qq", args[1])
                    "username" -> updateBindings(subjectId, "username", args[1])
                    else -> {
                        val id = args[0]
                        if (id.toLongOrNull() != null)
                            updateBindings(subjectId, "qq", id)
                        else
                            updateBindings(subjectId, "username", id)
                    }
                }
                reply("绑定成功。")
            }
            startsWith(listOf("b50", "/b50")) { arg ->
                val (type, credential) =
                    if (arg.isNotBlank()) Pair("username", arg)
                    else queryBindings(subjectId) ?: run {
                        reply(buildString {
                            appendLine("您未在本机器人绑定账号（这个绑定与查分器上面的绑定无关！！！无！关！）")
                            appendLine("请使用“/mai bind 用户名或qq号”来绑定！不是“/b50 bind”而是“/mai bind”！")
                            appendLine("或者使用“/b50 用户名”来按用户名查询")
                            appendLine()
                            appendLine("（常见问题：为什么需要再在机器人这里绑定一次QQ号？答：因为机器人获取不到QQ号！！获取不到！！）")
                        })
                        return@startsWith
                    }
                val (status, data) = prober.getPlayerData(type, credential)
                when (status) {
                    HttpStatusCode.OK -> {
                        reply(images.generateBest(data!!, subjectId).toImage())
                    }
                    HttpStatusCode.BadRequest -> {
                        reply("您的QQ未在机器人处绑定查分器账号（详见/mai bind）或所查询的用户名不存在，" +
                                "请确认用户名对应的玩家在 Diving-Fish 的舞萌 DX 查分器上已注册")
                    }
                    HttpStatusCode.Forbidden -> {
                        reply("该玩家已禁止他人查询成绩")
                    }
                }
            }
            startsWith(listOf("mb50", "/mb50")) { arg ->
                val (type, credential) =
                    if (arg.isNotBlank()) Pair("username", arg)
                    else queryBindings(subjectId) ?: run {
                        reply(buildString {
                            appendLine("您未在本机器人绑定账号（这个绑定与查分器上面的绑定无关！！！无！关！）")
                            appendLine("请使用“/mai bind 用户名或qq号”来绑定！")
                            appendLine("或者使用“/mai mb50 用户名”来按用户名查询")
                            appendLine()
                            appendLine("（常见问题：为什么需要再在机器人这里绑定一次QQ号？答：因为机器人获取不到QQ号！！获取不到！！）")
                        })
                        return@startsWith
                    }
                prober.getPlayerDataManually(type, credential)?.let { data ->
                    reply(images.generateBest(data, subjectId).toImage())
                }
            }
            startsWith(listOf("ap50", "/ap50")) { arg ->
                val (type, credential) =
                    if (arg.isNotBlank()) Pair("username", arg)
                    else queryBindings(subjectId) ?: run {
                        reply(buildString {
                            appendLine("您未在本机器人绑定账号（这个绑定与查分器上面的绑定无关！！！无！关！）")
                            appendLine("请使用“/mai bind 用户名或qq号”来绑定！")
                            appendLine("或者使用“/mai ap50 用户名”来按用户名查询")
                            appendLine()
                            appendLine("（常见问题：为什么需要再在机器人这里绑定一次QQ号？答：因为机器人获取不到QQ号！！获取不到！！）")
                        })
                        return@startsWith
                    }
                val (status, data) = prober.getPlayerData(type, credential)
                val records = prober.getDataByVersion(type, credential, getPlateVerList("all"))
                when (status) {
                    HttpStatusCode.OK -> {
                        reply(images.generateAP50(data!!, records.second!!.verList, subjectId).toImage())
                    }
                    HttpStatusCode.BadRequest -> {
                        reply("您的QQ未绑定查分器账号（详见/mai bind）或所查询的用户名不存在，" +
                                "请确认用户名对应的玩家在 Diving-Fish 的舞萌 DX 查分器上已注册")
                    }
                    HttpStatusCode.Forbidden -> {
                        reply("该玩家已禁止他人查询成绩")
                    }
                }
            }
            startsWith("id") { raw ->
                val id = raw.filter {
                    it.isDigit()
                }.toIntOrNull()?.toString() ?: return@startsWith
                val cover = images.getCoverById(id)
                if (cover.exists())
                    reply(musics.getInfo(id).toPlainText() + cover.toImage())
                else
                    reply(musics.getInfo(id))
            }
            startsWith("查歌") { name ->
                val result = musics.findByName(name)
                when (result.size) {
                    0 -> {
                        reply("未搜索到歌曲，请检查拼写。\n按别名搜索请发送“XXX是什么歌”")
                    }
                    1 -> {
                        val music = result.first()
                        val cover = images.getCoverById(music.id)
                        if (cover.exists())
                            reply(musics.getInfo(music.id).toPlainText() + cover.toImage())
                        else
                            reply(musics.getInfo(music.id))
                    }
                    else -> {
                        reply(ListArk.build {
                            desc { "maimai 查歌" }
                            prompt { "查歌结果" }
                            text { "您要找的歌曲可能是：" }
                            result.forEach { music ->
                                text { "${music.id}. ${music.title}" }
                            }
                        })
                    }
                }
            }
            endsWith("是什么歌") { alias ->
                val result = aliases.findByAlias(alias)
                when (result.size) {
                    0 -> {
                        reply("未找到相关歌曲。\n使用方法：XXX是什么歌\n按歌曲名称查询请用/mai 查歌，添加别名请使用“/mai 添加别名”")
                    }
                    1 -> {
                        val music = result.first()
                        val cover = images.getCoverById(music.id)
                        if (cover.exists())
                            reply(musics.getInfo(music.id).toPlainText() + cover.toImage())
                        else
                            reply(musics.getInfo(music.id))
                    }
                    else -> {
                        reply(ListArk.build {
                            desc { "maimai 按别名查歌" }
                            prompt { "别名查歌结果" }
                            text { "您要找的歌曲可能是：" }
                            result.forEach { music ->
                                text { "${music.id}. ${music.title}" }
                            }
                        })
                    }
                }
            }
            startsWith("正则查歌") { rawRegex ->
                if (rawRegex.isBlank()) {
                    reply("使用方法：/mai 正则查歌 正则表达式\n例：/mai 正则查歌 ^(?i)w.*(?i)ing\$")
                }
                val regex = kotlin.runCatching {
                    Regex(rawRegex)
                }.getOrNull() ?: run {
                    reply("请使用正确的正则表达式查询。\n使用方法：/mai 正则查歌 正则表达式")
                    return@startsWith
                }
                val result = musics.findByRegex(regex)
                when (result.size) {
                    0 -> {
                        reply("未搜索到歌曲，请检查正则表达式。")
                    }
                    1 -> {
                        val music = result.first()
                        val cover = images.getCoverById(music.id)
                        if (cover.exists())
                            reply(musics.getInfo(music.id).toPlainText() + cover.toImage())
                        else
                            reply(musics.getInfo(music.id))
                    }
                    else -> {
                        reply(ListArk.build {
                            desc { "maimai 正则查歌" }
                            prompt { "正则查歌结果" }
                            text { "您要找的歌曲可能是：" }
                            result.forEach { music ->
                                text { "${music.id}. ${music.title}" }
                            }
                        })
                    }
                }
            }
            startsWith("谱师查歌") { raw ->
                val args = raw.toArgsList()
                if (args.isEmpty()) {
                    reply("用法：/mai 谱师查歌 谱师名称 页数\n例：/mai 谱师查歌 翠楼屋 2\n例：/mai 谱师查歌 合作だよ")
                    return@startsWith
                }
                val charter = args[0]
                val page = args.getOrNull(1)?.toIntOrNull() ?: 1
                val total = musics.findByCharter(charter)
                val result = if (page in 1..(total.size / 16.0).toIntCeil()) {
                    total.subList((page - 1) * 16, total.size).take(16)
                } else {
                    emptyList()
                }
                when (result.size) {
                    0 -> {
                        reply("未搜索到歌曲，请检查谱师名称拼写或者页数。\n按别名搜索请发送“XXX是什么歌”")
                    }
                    1 -> {
                        val music = result.first()
                        val cover = images.getCoverById(music.id)
                        if (cover.exists())
                            reply(musics.getInfo(music.id).toPlainText() + cover.toImage())
                        else
                            reply(musics.getInfo(music.id))
                    }
                    else -> {
                        reply(ListArk.build {
                            desc { "maimai 谱师查歌" }
                            prompt { "谱师查歌结果" }
                            text { "您要找的歌曲可能是：" }
                            result.forEach { music ->
                                text { "${music.id}. ${music.title}" }
                            }
                        })
                    }
                }
            }
            endsWith("有什么别名") { id ->
                val music = musics.getById(id) ?: run {
                    reply(buildString {
                        appendLine("id 不存在。")
                        appendLine("使用方法：[id号]有什么别名")
                        appendLine("例：379有什么别名")
                    })
                    return@endsWith
                }
                val nowAliases = aliases.getAllAliases(id)
                if (nowAliases.isEmpty()) {
                    reply("该歌曲无别名。您可以使用“/mai 添加别名 id 别名”来添加别名。")
                    return@endsWith
                }
                reply(ListArk.build {
                    desc { "maimai 别名查看" }
                    prompt { "别名列表" }
                    text { "${music.id}. ${music.title} 有如下别名：" }
                    nowAliases.forEach {
                        text { it }
                    }
                    text { "" }
                    text { "可以使用“/mai 添加别名 id 别名”来添加别名。" }
                })
            }
            startsWith("添加别名") { raw ->
                val args = raw.toArgsList()
                if (args.size != 2) {
                    reply("使用方法：/mai 添加别名 id 别名\n例：/mai 添加别名 834 潘")
                    return@startsWith
                }
                val (id, alias) = args.take(2)
                if (musics.getById(id) == null) {
                    reply("该id的歌曲不存在！")
                    return@startsWith
                }
                aliases.add(id, alias)
                val nowAliases = aliases.getAllAliases(id)
                reply("添加成功，当前该歌曲别名有：" + nowAliases.joinToString(", "))
            }
            startsWith("删除别名") { raw ->
                val args = raw.toArgsList()
                if (args.size != 2) {
                    reply("使用方法：/mai 删除别名 id 别名\n例：/mai 删除别名 834 asd")
                    return@startsWith
                }
                val (id, alias) = args.take(2)
                if (musics.getById(id) == null) {
                    reply("该id的歌曲不存在！")
                    return@startsWith
                }
                val nowAliases = aliases.getAllAliases(id).toMutableList()
                if (alias !in nowAliases) {
                    reply("该别名不存在！")
                    return@startsWith
                }
                aliases.remove(id, alias)
                nowAliases.remove(alias)
                reply("添加成功，当前该歌曲别名有：" + nowAliases.joinToString(", "))
            }
            startsWith("定数查歌") { raw ->
                val args = raw.toArgsList().mapNotNull { it.toDoubleOrNull() }
                if (args.isEmpty()) {
                    reply(buildString {
                        appendLine("使用方法：/定数查歌 [定数] [定数]")
                        appendLine("例：/定数查歌 13.9")
                        appendLine("例：/定数查歌 13.0 13.6")
                    })
                    return@startsWith
                }
                reply(if (args.size == 1) {
                    musics.findByDS(args.first()..args.first())
                } else {
                    musics.findByDS(args.first()..args.last())
                })
            }
            startsWith("分数线") { raw ->
                val args = raw.toArgsList()
                if (args.size == 1 && args.first() == "帮助") {
                    reply(buildString {
                        appendLine("此功能为查找某首歌分数线设计。")
                        appendLine("命令格式：分数线 <难度+歌曲id> <分数线>")
                        appendLine("例如：分数线 紫379 100.5")
                        appendLine("命令将返回分数线允许的 TAP GREAT 容错以及 BREAK 50落等价的 TAP GREAT 数。")
                        appendLine("以下为 TAP GREAT 的对应表：")
                        appendLine("GREAT/GOOD/MISS")
                        appendLine("TAP   1/2.5/5")
                        appendLine("HOLD  2/5/10")
                        appendLine("SLIDE 3/7/15")
                        appendLine("TOUCH 1/2/5")
                        appendLine("BREAK 5/12.5/25(外加200落)")
                    })
                    return@startsWith
                }
                if (args.size != 2 ||
                    args[1].toDoubleOrNull() == null ||
                    (args[1].toDouble() !in 0.0..101.0) ||
                    name2Difficulty(args[0][0]) == null ||
                    args[0].none { it.isDigit() } ||
                    musics.getById(args[0].filter { it.isDigit() }) == null) {
                    reply("格式错误，请输入“/mai 分数线 帮助”查看使用说明。")
                    return@startsWith
                }
                val difficulty = name2Difficulty(args[0][0])!!
                val id = args[0].filter { it.isDigit() }
                val music = musics.getById(id)!!
                if (music.level.size <= difficulty) {
                    reply("该谱面没有此难度。请输入“/mai 分数线 帮助”查看使用说明。")
                    return@startsWith
                }
                val line = args[1].toDouble()
                val notes = ChartNotes.fromList(music.charts[difficulty].notes)!!
                val totalScore = notes.tap * 500.0 + notes.hold * 1000 + notes.slide * 1500 +
                        (notes.touch ?: 0) * 500 + notes.breaks * 2500
                val breakBonus = 0.01 / notes.breaks
                val break50Reduce = totalScore * breakBonus / 4
                val reduce = 101.0 - line
                reply(buildString {
                    appendLine("[${args[0][0]}] ${music.title}")
                    append("分数线 $line% 允许的最多 TAP GREAT 数量为 ")
                    append(String.format("%.2f", totalScore * reduce / 10000))
                    appendLine(" (每个 -" + String.format("%.4f", 10000.0 / totalScore) + "%),")
                    append("BREAK 50落 (一共 ${notes.breaks} 个) 等价于 ")
                    append(String.format("%.3f", break50Reduce / 100) + " 个 TAP GREAT ")
                    append("(-" + String.format("%.4f", break50Reduce / totalScore * 100) + "%)")
                })
            }
            versionsBrief.forEach { ver ->
                plateCategories.forEach { type ->
                    if (ver != "" || type == "霸者") {
                        startsWith("${ver}${type}进度") { arg ->
                            val data = getVersionData(ver, arg, this) ?: return@startsWith
                            reply(musics.plateProgress(ver, type, getPlateVerList(ver), data.verList))
                        }
                    }
                    if (ver != "" && type != "霸者" && ver != "舞")
                        startsWith("${ver}${type}完成表") { arg ->
                            val data = getVersionData(ver, arg, this) ?: return@startsWith
                            reply(images.plateProgress(ver, type, getPlateVerList(ver), data.verList).toImage())
                        }
                }
            }
            levels.forEach { level ->
                recordCategories.forEach { type ->
                    val commands = mutableListOf("${level}${type}进度")
                    when (type) {
                        "sss" -> commands.add("${level}将进度")
                        "sss+" -> commands.add("${level}鸟加进度")
                        "fc" -> commands.add("${level}极进度")
                        "ap" -> commands.add("${level}神进度")
                        "fdx" -> commands.add("${level}舞舞进度")
                        "clear" -> commands.add("${level}霸者进度")
                    }
                    startsWith(commands) { arg ->
                        val data = getVersionData("all", arg, this) ?: return@startsWith
                        reply(musics.dsProgress(level, type, data.verList))
                    }
                }
                startsWith("${level}定数表") {
                    val result = images.getImage("ds/${level}.png")
                    reply(result.encode(PNG).toImage())
                }
                startsWith("${level}完成表") { arg ->
                    val data = getVersionData("all", arg, this) ?: return@startsWith
                    val result = images.dsProgress(level, data.verList)
                    reply(result.toImage())
                }
                startsWith("${level}分数列表") { raw ->
                    val args = raw.toArgsList()
                    val page = if (args.isEmpty()) 1 else args[0].toIntOrNull() ?: 1
                    val username = if (args.size > 1) args[1] else ""
                    val (type, credential) = getCredential(username, this) ?: return@startsWith
                    val (status, basicInfo) = prober.getPlayerData(type, credential)
                    when (status) {
                        HttpStatusCode.BadRequest -> {
                            reply("您的QQ未绑定查分器账号或所查询的用户名不存在，" +
                                    "请确认用户名对应的玩家在 Diving-Fish 的舞萌 DX 查分器上已注册")
                            return@startsWith
                        }
                        HttpStatusCode.Forbidden -> {
                            reply("该玩家已禁止他人查询成绩")
                            return@startsWith
                        }
                    }
                    if (basicInfo == null)
                        return@startsWith
                    val records = prober.getDataByVersion(type, credential, getPlateVerList("all"))
                    reply(images.getLevelRecordList(
                        level,
                        page,
                        basicInfo,
                        records.second!!.verList,
                        subjectId
                    ).toImage())
                }
            }
            difficulties.forEachIndexed { difficulty, name ->
                startsWith("${name}id") { raw ->
                    val id = raw.filter { it.isDigit() }.toIntOrNull() ?.toString() ?: return@startsWith
                    val cover = images.getCoverById(id)
                    if (cover.exists())
                        reply(musics.getInfoWithDifficulty(id, difficulty).toPlainText() + cover.toImage())
                    else
                        reply(musics.getInfoWithDifficulty(id, difficulty).toPlainText())
                }
            }
            startsWith("随个") { raw ->
                if (raw.isNotEmpty()) {
                    var difficulty: Int? = -1
                    val level = if (!raw[0].isDigit()) {
                        difficulty = name2Difficulty(raw[0])
                        raw.substr(1).filter { it.isDigit() || it == '+' }
                    } else {
                        raw.filter { it.isDigit() || it == '+' }
                    }
                    difficulty ?.let {
                        musics.getRandom(level, difficulty) ?.let {
                            val cover = images.getCoverById(it.id)
                            if (cover.exists())
                                reply(musics.getInfo(it.id).toPlainText() + cover.toImage())
                            else
                                reply(musics.getInfo(it.id))
                        }
                    }
                }
            }
            startsWith("mai什么") { raw ->
                val args = raw.toArgsList()
                val data = getPlayerData(
                    if (args.size > 1) args[1] else "",
                    this
                ) ?: return@startsWith
                if ("加" in raw || "推" in raw) {
                    reply(musics.getRandomForRatingUp(
                        raw.filter {
                            it.isDigit()
                        }.toIntOrNull() ?: 1,
                        1,
                        100.5,
                        data
                    ))
                } else {
                    musics.getRandom("", Random.nextInt(0, 4)) ?.let {
                        val cover = images.getCoverById(it.id)
                        if (cover.exists())
                            reply(musics.getInfo(it.id).toPlainText() + cover.toImage())
                        else
                            reply(musics.getInfo(it.id))
                    }
                }
            }
            startsWith("随机推分金曲") { username ->
                val data = getPlayerData(username,this) ?: return@startsWith
                reply(musics.getRandomForRatingUp(data = data))
            }
            startsWith("随机推分列表") { raw ->
                val args = raw.toArgsList()
                if (args.isEmpty()) {
                    reply(buildString {
                        appendLine("使用方法：随机推分列表 推分分数 [推荐谱面数量] [达成率]")
                        appendLine("例：")
                        appendLine("随机推分列表 2")
                        appendLine("随机推分列表 3 20")
                        appendLine("随机推分列表 1 15 99.5")
                    })
                } else {
                    val data = getPlayerData("", this) ?: return@startsWith
                    val score = args[0].toInt()
                    val amount = args.getOrElse(1) { "10" }.toInt()
                    val acc = args.getOrElse(2) { "100.5" }.toDouble()
                    reply(musics.getRandomForRatingUp(score, amount, acc, data))
                }
            }
            startsWith("info") { raw ->
                val args = raw.toArgsList()
                val music = if (args.isNotEmpty()) args[0] else return@startsWith
                val username = if (args.size > 1) args[1] else ""
                val data = getVersionData("all", username,this) ?: return@startsWith
                if (musics.any { it.id == music }) {
                    images.musicInfo(music, data.verList) ?.let {
                        reply(it.toImage())
                    }
                } else {
                    musics.filter {
                        it.basicInfo.title.lowercase() == music.lowercase()
                    }.firstOrNull() ?.let {
                        images.musicInfo(it.id, data.verList) ?.let { img ->
                            reply(img.toImage())
                        }
                    } ?: run {
                        aliases.findByAlias(music).firstOrNull() ?.let {
                            images.musicInfo(it.id, data.verList) ?.let { img ->
                                reply(img.toImage())
                            }
                        } ?: run {
                            reply("使用方法：info id/歌名/别名")
                        }
                    }
                }
            }
            startsWith("设置猜歌") { option ->
                when (option.trim()) {
                    in listOf("启用", "开启", "允许") ->  {
                        Permissions.setPerm(contextId, "maimai.guess", true)
                        reply("启用成功")
                    }
                    in listOf("禁用", "关闭", "禁止") -> {
                        Permissions.setPerm(contextId, "maimai.guess", false)
                        reply("禁用成功")
                    }
                    else -> reply(buildString {
                        appendLine("您可以使用本命令启用/禁用本群的猜歌功能。")
                        appendLine("例：")
                        appendLine("\t设置猜歌 启用")
                        appendLine("\t设置猜歌 禁用")
                    })
                }
            }
            startsWith("今日舞萌") {
                val time = LocalDateTime.now()
                val hash = ((subjectId + time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).toByteArray())
                    .hashCode()
                var h = hash
                val dailyMusic = musics.getRandom()
                reply(buildString {
                    appendLine(time.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 E")))
                    appendLine("今日幸运指数为 ${hash % 100 + 1}")
                    dailyOps.forEach {
                        val now = h and 3
                        if (now == 3)
                            appendLine("宜 $it")
                        else if (now == 0)
                            appendLine("忌 $it")
                        h = h shr 2
                    }
                    appendLine("今日推荐歌曲：")
                    appendLine(musics.getInfo(dailyMusic.id))
                }.toPlainText() + images.getCoverById(dailyMusic.id).toImage())
            }
            startsWith("设置b50") { raw ->
                val args = raw.toArgsList()
                if (args.size != 2) {
                    reply(ListArk.build {
                        desc { "设置b50" }
                        prompt { "设置b50" }
                        text { "使用方法：/mai 设置b50 选项 值" }
                        text { "例：/mai 设置b50 头像 UI_Icon_003304" }
                        text { "例：/mai 设置b50 牌子 UI_Plate_209502" }
                        text { "例：/mai 设置b50 牌子 使用查分器设置" }
                        text { "例：/mai 设置b50 牌子 晓将" }
                        link("https://otmdb.cn/karenbot/maimai_icons") { "点我查看头像列表" }
                        link("https://otmdb.cn/karenbot/maimai_plates") { "点我查看牌子列表" }
                    })
                    return@startsWith
                }
                val name = args[1]
                when (args[0]) {
                    "头像" -> {
                        images.getImageFilename("themes/${config.theme}/icon", name) ?.let { filename ->
                            transactionWithLock {
                                MaimaiSettings.upsert {
                                    it[this.openid] = subjectId
                                    it[this.name] = "ICON_FILENAME"
                                    it[this.value] = filename
                                }
                            }
                            reply("设置成功。")
                        } ?: run {
                            reply("该头像不存在！")
                        }
                    }
                    "牌子" -> {
                        transactionWithLock {
                            if (name == "使用查分器设置") {
                                MaimaiSettings.upsert {
                                    it[this.openid] = subjectId
                                    it[this.name] = "IS_PREFERRING_PROBER_PLATE"
                                    it[this.value] = "true"
                                }
                                reply("设置成功。")
                            }
                            val filename = getPlateFilename(name) ?: images.getImageFilename(
                                "themes/${config.theme}/plate", name) ?: run {
                                reply("该牌子不存在！")
                                return@transactionWithLock
                            }
                            getPlateByFilename(filename)?.let { (ver, type) ->
                                val data = getVersionData(ver, "", this@startsWith) ?: return@transactionWithLock
                                val remains = musics.getPlateRemains(ver, type, getPlateVerList(ver), data.verList)
                                if (remains[3].isNotEmpty() || remains[4].isNotEmpty()) {
                                    reply("您未达成该牌子的领取条件！")
                                    return@transactionWithLock
                                }
                            }
                            MaimaiSettings.upsert {
                                it[this.openid] = subjectId
                                it[this.name] = "PLATE_FILENAME"
                                it[this.value] = filename
                            }
                            MaimaiSettings.upsert {
                                it[this.openid] = subjectId
                                it[this.name] = "IS_PREFERRING_PROBER_PLATE"
                                it[this.value] = "false"
                            }
                            reply("设置成功。")
                        }
                    }
                    else -> {
                        reply("使用方法有误，请使用“/mai 设置b50”查看帮助。")
                        return@startsWith
                    }
                }
            }
        }
        GlobalEventChannel.subscribePublicMessages("/mai", permName = "maimai.guess") {
            equalsTo(listOf("猜歌", "/猜歌")) {
                launch(Dispatchers.IO) {
                    guessGame.startClassical(this)
                }
            }
            equalsTo(listOf("舞萌开字母", "/舞萌开字母", "出你字母")) {
                launch(Dispatchers.IO) {
                    guessGame.startOpening(this)
                }
            }
        }
    }

}