package xyz.xszq.bot.maimai

import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.encode
import com.soywiz.korio.file.std.localCurrentDirVfs
import com.soywiz.korio.lang.substr
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import xyz.xszq.bot.dao.MaimaiBinding
import xyz.xszq.bot.dao.Permissions
import xyz.xszq.bot.maimai.DXProberClient.Companion.getPlateVerList
import xyz.xszq.bot.maimai.MaimaiUtils.difficulties
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
import xyz.xszq.nereides.message.plus
import xyz.xszq.nereides.message.toImage
import xyz.xszq.nereides.message.toPlainText
import xyz.xszq.nereides.toArgsList
import kotlin.random.Random

object Maimai {
    private val logger = KotlinLogging.logger("Maimai")

    private val musics = MusicsInfo(logger)
    val prober = DXProberClient()
    val images = MaimaiImage(musics, logger, localCurrentDirVfs["maimai"])
    private val aliases = Aliases(musics)
    private val guessGame = GuessGame(musics, images, aliases)
    private lateinit var config: MaimaiConfig
    private suspend fun queryBindings(openId: String): Pair<String, String>? = suspendedTransactionAsync(Dispatchers.IO) {
        val bindings = MaimaiBinding.findById(openId) ?: return@suspendedTransactionAsync null
        Pair(bindings.type, bindings.credential)
    }.await()
    private suspend fun updateBindings(openId: String, type: String, credential: String) = newSuspendedTransaction(Dispatchers.IO) {
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
    @OptIn(DelicateCoroutinesApi::class)
    fun init() {
        GlobalScope.launch {
            config = MaimaiConfig.load(localCurrentDirVfs["maimai/settings.yml"])

            logger.info { "正在获取歌曲数据……" }
            musics.updateMusicInfo(prober.getMusicList())
            musics.updateStats(prober.getChartStat())
            musics.updateHot()

            logger.info { "正在更新别名数据……" }
            aliases.updateXrayAliases(config.xrayAliasUrl)

            logger.info { "正在缓存歌曲封面中……" }
            images.downloadCovers(config, musics.getAll())

            logger.info { "正在加载图片中……" }
            images.load(config.theme)

            logger.info { "正在生成定数表中……" }
            images.preGenerateDsList()
            logger.info { "maimai 功能加载完成。" }
        }
    }
    private suspend fun getCredential(arg: String, event: MessageEvent): Pair<String, String>? = event.run {
        if (arg.isNotBlank()) Pair("username", arg)
        else queryBindings(subjectId) ?: run {
            reply(buildString {
                appendLine("使用方法：/mai b50 用户名")
                appendLine("您也可以使用 /mai bind 绑定查分器账号进行快速查询 (此绑定与查分器绑定无关)")
            })
            null
        }
    }
    private suspend fun getVersionData(version: String, arg: String, event: MessageEvent): PlateResponse? = event.run {
        val (credentialType, credential) = getCredential(arg, event) ?: return@run null
        val (status, data) = prober.getDataByVersion(credentialType, credential, getPlateVerList(version))
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
                    append("当前支持的子指令：查歌 bind b50 ap50 id info 是什么歌 有什么别名 定数查歌 分数线 进度")
                    append(" 完成表 分数列表 随个 mai什么推分 猜歌")
                })
            }
        }
        GlobalEventChannel.subscribePublicMessages("/mai", permName = "maimai") {
            startsWith("bind") { raw ->
                if (this !is GroupAtMessageEvent)
                    return@startsWith
                val args = raw.toArgsList()
                if (args.size != 2) {
                    reply(buildString {
                        appendLine("使用方法（二选一，建议绑qq）：")
                        appendLine("/mai bind qq qq号")
                        appendLine("/mai bind username 用户名")
                    })
                    return@startsWith
                }
                when (args[0]) {
                    "qq" -> updateBindings(subjectId, "qq", args[1])
                    "username" -> updateBindings(subjectId, "username", args[1])
                    else -> {
                        reply(buildString {
                            appendLine("使用方法（二选一，建议绑qq）：")
                            appendLine("/mai bind qq qq号")
                            appendLine("/mai bind username 用户名")
                        })
                        return@startsWith
                    }
                }
                reply("绑定成功。")
            }
            startsWith(listOf("b50", "/b50")) { arg ->
                val (type, credential) =
                    if (arg.isNotBlank()) Pair("username", arg)
                    else queryBindings(subjectId) ?: run {
                        reply(buildString {
                            appendLine("使用方法：/mai b50 用户名")
                            appendLine("您也可以使用 /mai bind 绑定查分器账号进行快速查询 (此绑定与查分器绑定无关)")
                        })
                        return@startsWith
                    }
                val (status, data) = prober.getPlayerData(type, credential)
                when (status) {
                    HttpStatusCode.OK -> {
                        reply(images.generateBest(data!!).toImage())
                    }
                    HttpStatusCode.BadRequest -> {
                        reply("您的QQ未绑定查分器账号或所查询的用户名不存在，" +
                                "请确认用户名对应的玩家在 Diving-Fish 的舞萌 DX 查分器上已注册")
                    }
                    HttpStatusCode.Forbidden -> {
                        reply("该玩家已禁止他人查询成绩")
                    }
                }
            }
            startsWith(listOf("ap50", "/ap50")) { arg ->
                val (type, credential) =
                    if (arg.isNotBlank()) Pair("username", arg)
                    else queryBindings(subjectId) ?: run {
                        reply(buildString {
                            appendLine("使用方法：/mai ap50 用户名")
                            appendLine("您也可以使用 /mai bind 绑定查分器账号进行快速查询 (此绑定与查分器绑定无关)")
                        })
                        return@startsWith
                    }
                val (status, data) = prober.getPlayerData(type, credential)
                val records = prober.getDataByVersion(type, credential, getPlateVerList("all"))
                when (status) {
                    HttpStatusCode.OK -> {
                        reply(images.generateAP50(data!!, records.second!!.verList).toImage())
                    }
                    HttpStatusCode.BadRequest -> {
                        reply("您的QQ未绑定查分器账号或所查询的用户名不存在，" +
                                "请确认用户名对应的玩家在 Diving-Fish 的舞萌 DX 查分器上已注册")
                    }
                    HttpStatusCode.Forbidden -> {
                        reply("该玩家已禁止他人查询成绩")
                    }
                }
            }
            startsWith("id") { raw ->
                if (this !is GroupAtMessageEvent)
                    return@startsWith
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
                if (this !is GroupAtMessageEvent)
                    return@startsWith
                val result = musics.findByName(name)
                when (result.size) {
                    0 -> {
                        reply("未搜索到歌曲，请检查拼写。")
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
                        reply(buildString {
                            appendLine("您要找的歌曲可能是：")
                            result.forEach { music ->
                                appendLine("${music.id}. ${music.title}")
                            }
                        })
                    }
                }
            }
            endsWith("是什么歌") { alias ->
                if (this !is GroupAtMessageEvent)
                    return@endsWith
                val result = aliases.findByAlias(alias)
                when (result.size) {
                    0 -> {
                        reply("未找到相关歌曲。\n使用方法：XXX是什么歌")
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
                        reply(buildString {
                            appendLine("您要找的歌曲可能是：")
                            result.forEach { music ->
                                appendLine("${music.id}. ${music.title}")
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
                reply(buildString {
                    appendLine("${music.id}. ${music.title} 有如下别名：")
                    aliases.getAllAliases(id).forEach {
                        appendLine(it)
                    }
                })
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
                            reply(images.plateProgress(ver, type, getPlateVerList(ver),data.verList).toImage())
                        }
                }
            }
            levels.forEach { level ->
                recordCategories.forEach { type ->
                    startsWith("${level}${type}进度") { arg ->
                        val data = getVersionData("all", arg, this) ?: return@startsWith
                        reply(musics.dsProgress(level, type, data.verList))
                    }
                }
                startsWith("${level}定数表") {
                    reply(images.getImage("ds/${level}.png").encode(PNG).toImage())
                }
                startsWith("${level}完成表") { arg ->
                    val data = getVersionData("all", arg, this) ?: return@startsWith
                    reply(images.dsProgress(level, data.verList).toImage())
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
                    reply(images.getLevelRecordList(level, page, basicInfo, records.second!!.verList).toImage())
                }
            }
            difficulties.forEachIndexed { difficulty, name ->
                startsWith("${name}id") { raw ->
                    if (this !is GroupAtMessageEvent)
                        return@startsWith
                    val id = raw.filter { it.isDigit() }.toIntOrNull() ?.toString() ?: return@startsWith
                    val cover = images.getCoverById(id)
                    if (cover.exists())
                        reply(musics.getInfoWithDifficulty(id, difficulty).toPlainText() + cover.toImage())
                    else
                        reply(musics.getInfo(id))
                }
            }
            startsWith("随个") { raw ->
                if (this !is GroupAtMessageEvent)
                    return@startsWith
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
                if (this !is GroupAtMessageEvent)
                    return@startsWith
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
        }
        GlobalEventChannel.subscribePublicMessages("/mai", permName = "maimai.guess") {
            equalsTo("猜歌") {
                if (this is GroupAtMessageEvent)
                    guessGame.start(this)
            }
        }
    }

}