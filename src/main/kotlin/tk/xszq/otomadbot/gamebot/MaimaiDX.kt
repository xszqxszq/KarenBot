@file:Suppress("unused", "EXPERIMENTAL_API_USAGE", "MemberVisibilityCanBePrivate")

package tk.xszq.otomadbot.gamebot

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kds.iterators.fastForEachWithIndex
import com.soywiz.kds.mapDouble
import com.soywiz.kmem.toIntFloor
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.bitmap.effect.BitmapEffect
import com.soywiz.korim.bitmap.effect.applyEffect
import com.soywiz.korim.bitmap.sliceWithSize
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.font.TtfFont
import com.soywiz.korim.font.readTtfFont
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.encode
import com.soywiz.korim.format.readNativeImage
import com.soywiz.korim.text.HorizontalAlign
import com.soywiz.korim.text.TextAlignment
import com.soywiz.korim.text.VerticalAlign
import com.soywiz.korim.vector.Context2d
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.baseName
import com.soywiz.korio.file.baseNameWithoutExtension
import com.soywiz.korio.file.extension
import com.soywiz.korio.file.std.toVfs
import com.soywiz.korio.lang.substr
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.filter
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.gildor.coroutines.okhttp.await
import tk.xszq.otomadbot.*
import tk.xszq.otomadbot.api.ApiSettings
import tk.xszq.otomadbot.api.PythonApi
import tk.xszq.otomadbot.core.OtomadBotCore
import java.io.File
import kotlin.math.roundToInt


@Serializable
class PlayerData(val nickname: String, val rating: Int, val additional_rating: Int, val username: String,
                val charts: Map<String, List<PlayScore>>)

@Serializable
open class PlayScore(val achievements: Double, val ds: Double, var dxScore: Int, val fc: String, val fs: String,
                     val level: String, val level_index: Int, val level_label: String, val ra: Int,
                     val rate: String, val song_id: Int, val title: String, val type: String)
object EmptyPlayRecord: PlayScore(-114.00, .0, 0, "", "", "",
    0, "", -100, "", -114514, "", "")
fun List<PlayScore>.fillEmpty(target: Int): List<PlayScore> {
    val result = toMutableList()
    for (i in (1..(target-size)))
        result.add(EmptyPlayRecord)
    return result
}

@Serializable
open class MaimaiMusicInfo(val id: String, val title: String, val type: String, val ds: List<Double>,
                           val level: List<String>, val cids: List<Int>, val charts: List<MaimaiChartInfo>,
                           val basic_info: MaimaiMusicBasicInfo)
@Serializable
open class MaimaiChartInfo(val notes: List<Int>, val charter: String)
@Serializable
open class MaimaiMusicBasicInfo(val title: String, val artist: String, val genre: String, val bpm: Int,
                                val release_date: String, val from: String, val is_new: Boolean)

val fontDir = File("/usr/share/fonts/ttf/").toVfs()
val fonts = mutableMapOf<String, TtfFont>()
object MaimaiDXHandler: EventHandler("舞萌DX", "maimaidx") {
    @Suppress("MemberVisibilityCanBePrivate")
    var musics = listOf<MaimaiMusicInfo>()
    var aliases = mapOf<String, List<String>>()
    val images = mutableMapOf<String, Bitmap>()

    override fun register() {
        GlobalScope.launchImmediately {
            fontDir.listRecursive().filter { it.extension == "ttf" || it.extension == "otf" }.collect {
                kotlin.runCatching {
                    fonts[it.baseName] = it.readTtfFont()
                }.onFailure {
                    println(it.stackTraceToString())
                }
            }
            fetchMusicInfo()
            PythonApi.getMaimaiAliases() ?.let {
                aliases = OtomadBotCore.json.decodeFromString(it.data)
            } ?: run {
                println("警告：获取 maimai DX 歌曲别名失败")
            }
        }
        GlobalEventChannel.subscribeMessages {
            startsWith("b40") { username ->
                onlyContact {
                    requireNot(denied) {
                        if (username.isBlank())
                            handleB40(id = sender.id.toString(), event = this)
                        else
                            handleB40("username", username, this)
                    }
                }
            }
            startsWith("b50") { username ->
                onlyContact {
                    requireNot(denied) {
                        if (username.isBlank())
                            handleB50(id = sender.id.toString(), event = this)
                        else
                            handleB50("username", username, this)
                    }
                }
            }
            startsWithSimple("随个") { raw, _ ->
                requireNot(denied) {
                    onlyContact {
                        if (raw.isNotEmpty()) {
                            var difficulty: Int? = -1
                            val level = if (!raw[0].isDigit()) {
                                difficulty = when (raw[0]) {
                                    '绿' -> 0
                                    '黄' -> 1
                                    '红' -> 2
                                    '紫' -> 3
                                    '白' -> 4
                                    else -> null
                                }
                                raw.substr(1).filter { it.isDigit() || it == '+' }
                            } else {
                                raw.filter { it.isDigit() || it == '+' }
                            }
                            difficulty?.let {
                                handleRandom(difficulty, level, this)
                            }
                        }
                    }
                }
            }
            startsWith("id") { id ->
                requireNot(denied) {
                    onlyContact {
                        searchById(id.filter { it.isDigit() }, this)
                    }
                }
            }
            startsWith("查歌") { name ->
                requireNot(denied) {
                    onlyContact {
                        searchByName(name, this)
                    }
                }
            }
            endsWithSimple("是什么歌") { alias, _ ->
                requireNot(denied) {
                    onlyContact {
                        searchByAlias(alias, this)
                    }
                }
            }
            startsWithSimple("定数查歌") { rawArgs, _ ->
                requireNot(denied) {
                    onlyContact {
                        val args = rawArgs.toArgsList().mapDouble { it.toDouble() }
                        if (args.size == 1)
                            searchByDS(args.first()..args.first(), this)
                        else
                            searchByDS(args.first()..args.last(), this)
                    }
                }
            }
//            endsWithSimple("分数列表") { level, _ ->
//                requireNot(denied) {
//                    onlyContact { // TODO: dev token required
//                        // queryRecordByLevel(level, this)
//                    }
//                }
//            }
        }
        arrayOf("绿", "黄", "红", "紫", "白").fastForEachWithIndex { difficulty, str ->
            GlobalEventChannel.subscribeMessages {
                startsWithSimple(str + "id") { id, _ ->
                    requireNot(denied) {
                        onlyContact {
                            searchByIdAndDifficulty(id, difficulty, this)
                        }
                    }
                }
            }
        }
        super.register()
    }
    suspend fun reload() {
        images.clear()
        OtomadBotCore.configFolder.resolve("image/maimai/cover").toVfs().list().collect {
            kotlin.runCatching {
                images[it.baseNameWithoutExtension] = it.readNativeImage()
            }.onFailure {
                pass
            }
        }
        OtomadBotCore.configFolder.resolve("image/maimai").toVfs().list().collect {
            kotlin.runCatching {
                images[it.baseNameWithoutExtension] = it.readNativeImage()
            }.onFailure {
                pass
            }
        }
    }
    suspend fun fetchMusicInfo() {
        musics = emptyList()
        val request = Request.Builder()
            .url("https://www.diving-fish.com/api/maimaidxprober/music_data")
            .build()
        val response = OkHttpClient().newCall(request).await()
        if (response.isSuccessful) {
            musics = OtomadBotCore.json.decodeFromString(response.body!!.get())
            OtomadBotCore.logger.info("已成功载入maimai DX 国服音乐数据")
        }
    }
    suspend fun fetchPlayerData(type: String = "qq", id: String, b50: Boolean = false): Pair<Int, PlayerData?> {
        val payload = buildJsonObject {
            put(type, id)
            if (b50)
                put("b50", true)
        }
        val request = Request.Builder()
            .url(ApiSettings.list["maimaidxprober"]!!.url)
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        val result = OkHttpClient().newCall(request).await()
        return Pair(result.code, if (result.code == 200)
            OtomadBotCore.json.decodeFromString(result.body!!.get()) else null)
    }
    private suspend fun getMusicInfoForSend(
        selected: MaimaiMusicInfo, event: MessageEvent, builder: MessageChainBuilder = MessageChainBuilder())
            = event.run {
        builder.add(selected.id + ". " + selected.title + "\n")
        resolveCoverFile(selected.title).let {
            if (it.name != "default.jpg")
                builder.add(it.uploadAsImage(subject))
        }
        builder.add("\n艺术家：${selected.basic_info.artist}" +
                "\n分类：${selected.basic_info.genre}" +
                "\n版本：${selected.basic_info.from}" + (if (selected.basic_info.is_new) " （计入b15）" else "") +
                "\nBPM：${selected.basic_info.bpm}" +
                "\n定数：" + selected.ds.joinToString("/"))
        builder
    }


    private suspend fun searchById(id: String, event: MessageEvent) = event.run {
        musics.find { it.id == id } ?.let { selected ->
            quoteReply(getMusicInfoForSend(selected, this).build())
        }
    }
    private suspend fun searchByIdAndDifficulty(id: String, difficulty: Int, event: MessageEvent) = event.run {
        musics.find { it.id == id && it.level.size > difficulty } ?.let { selected ->
            val chart = selected.charts[difficulty]
            val result = MessageChainBuilder()
            result.add("${selected.id}. ${selected.title}\n")
            resolveCoverFile(selected.title).let {
                if (it.name != "default.jpg")
                    result.add(it.uploadAsImage(subject))
            }
            result.add("\n等级: ${selected.level[difficulty]} (${selected.ds[difficulty]})")
            result.add("\nTAP: ${chart.notes[0]}\nHOLD: ${chart.notes[1]}")
            result.add("\nSLIDE: ${chart.notes[2]}")
            if (chart.notes.size == 5) // Interesting api lol
                result.add("\nTOUCH: ${chart.notes[3]}\nBREAK: ${chart.notes[4]}")
            else
                result.add("\nBREAK: ${chart.notes[3]}")
            if (chart.charter != "-")
                result.add("\n谱师：${chart.charter}")
            quoteReply(result.build())
            pass
        }
    }
    private suspend fun searchByName(name: String, event: MessageEvent) = event.run {
        val list = musics.filter { name.lowercase() in it.basic_info.title.lowercase() }.take(50)
        if (list.isEmpty()) {
            quoteReply("未搜索到歌曲，请检查拼写。如需搜索别称请发送“XXX是什么歌”")
        } else {
            var result = ""
            list.forEach { result += "${it.id}. ${it.basic_info.title}\n" }
            quoteReply(result)
        }
    }
    private suspend fun searchByAlias(alias: String, event: MessageEvent) = event.run {
        val names = aliases.filter { (_, value) ->
            var matched = false
            value.fastForEach inner@ {
                if (alias in it) {
                    matched = true
                    return@inner
                }
            }
            matched
        }
        val matched = musics.filter { it.title in names.keys }
        when {
            matched.size > 1 -> {
                var result = "您要找的歌曲可能是："
                matched.fastForEach { result += "\n" + it.id + ". " + it.title }
                quoteReply(result)
            }
            matched.isNotEmpty() -> {
                val selected = matched.first()
                val result = MessageChainBuilder()
                result.add("您要找的歌曲可能是：\n")
                quoteReply(getMusicInfoForSend(selected, this, result).build())
            }
            else -> pass
        }
        pass
    }
    private suspend fun searchByDS(range: ClosedFloatingPointRange<Double>, event: MessageEvent) = event.run {
        var result = ""
        musics.filter { it.ds.any { now -> now in range } }.take(50).fastForEach { selected ->
            val difficulties = mutableListOf<Int>()
            selected.ds.fastForEachWithIndex { index, value ->
                if (value in range)
                    difficulties.add(index)
            }
            difficulties.forEach { difficulty ->
                result += "${selected.id}. ${selected.title} ${MaiDataConverter.difficulty2Name(difficulty)} " +
                        "${selected.level[difficulty]} (${selected.ds[difficulty]})\n"
            }
        }
        quoteReply(if (result == "") "没有找到歌曲。\n使用方法：\n\t定数查歌 定数\n\t定数查歌 下限 上限" else result)
    }

    private suspend fun queryRecordByLevel(level: String, event: MessageEvent) = event.run {
        // TODO: dev token required
        val result = fetchPlayerData("qq", sender.id.toString())
        when (result.first) {
            200 -> {
                val selected = (result.second!!.charts["dx"]!! + result.second!!.charts["sd"]!!)
                    .filter { it.level == level }.sortedByDescending { it.achievements }
                generate40Scores(selected, "$level 分数列表 (前40)", result.second!!).toExternalResource().use {
                    quoteReply(it.uploadAsImage(subject))
                }
            }
            400 -> quoteReply(
                "用户名不存在，请确认用户名对应的玩家在 Diving-Fish 的舞萌 DX 查分器" +
                        "（https://www.diving-fish.com/maimaidx/prober/）上已注册"
            )
            403 -> quoteReply("该玩家设置了禁止查询")
        }
        pass
    }


    private suspend fun handleRandom(difficulty: Int = -1, level: String, event: MessageEvent) = event.run {
        val selected = musics.filter {
            (level in it.level && it.level.size > difficulty && it.level[difficulty] == level)
                    || (level == "" && it.level.size > difficulty)
                    || (difficulty == -1 && level in it.level)}.randomOrNull()
        selected ?.let {
            quoteReply(PlainText("${selected.id}. ${selected.title}\n") +
                    resolveCoverFile(it.title).uploadAsImage(subject) + "\n定数：" +
                    PlainText(selected.ds.joinToString("/")))
        } ?: run {
            quoteReply("没有这样的乐曲。")
        }
    }
    private suspend fun handleB40(type: String = "qq", id: String, event: MessageEvent) = event.run {
        val result = fetchPlayerData(type, id)
        when (result.first) {
            200 -> {
                generateB40Image(result.second!!).toExternalResource().use { img ->
                    quoteReply(img.uploadAsImage(subject))
                }
            }
            400 -> quoteReply("用户名不存在，请确认用户名对应的玩家在 Diving-Fish 的舞萌 DX 查分器" +
                    "（https://www.diving-fish.com/maimaidx/prober/）上已注册")
            403 -> quoteReply("该玩家已禁止他人查询成绩")
        }
        pass
    }
    private suspend fun handleB50(type: String = "qq", id: String, event: MessageEvent) = event.run {
        val result = fetchPlayerData(type, id, true)
        when (result.first) {
            200 -> {
                generateB50Image(result.second!!).toExternalResource().use { img ->
                    quoteReply(img.uploadAsImage(subject))
                }
            }
            400 -> quoteReply("用户名不存在，请确认用户名对应的玩家在 Diving-Fish 的舞萌 DX 查分器" +
                    "（https://www.diving-fish.com/maimaidx/prober/）上已注册")
            403 -> quoteReply("该玩家已禁止他人查询成绩")
        }
        pass
    }
    private suspend fun generateB40Image(info: PlayerData): ByteArray {
        val b40 = images[MaimaiConfig.b40.bg]!!.clone()
        val realRating = info.rating + info.additional_rating
        return b40.context2d {
            val ratingBg = images["rating_base_${MaiDataConverter.ratingColor(realRating)}"]!!
            drawImage(ratingBg, MaimaiConfig.b40.ratingBg.x, MaimaiConfig.b40.ratingBg.y)

            fillText(info.nickname.toSBC(), MaimaiConfig.b40.name)
            fillText(realRating.toString().toList().joinToString(" "), MaimaiConfig.b40.dxrating,
                Colors.YELLOW, TextAlignment(HorizontalAlign.RIGHT, VerticalAlign.MIDDLE))
            fillText("底分：${info.rating} + 段位分：${info.additional_rating}", MaimaiConfig.b40.ratingDetail,
                align=TextAlignment.CENTER)

            drawCharts(info.charts["sd"]!!.fillEmpty(25), 5,
                MaimaiConfig.b40.leftPart.x, MaimaiConfig.b40.leftPart.y, 8, MaimaiConfig.b40)
            drawCharts(info.charts["dx"]!!.fillEmpty(15), 3,
                MaimaiConfig.b40.rightPart.x, MaimaiConfig.b40.rightPart.y, 8, MaimaiConfig.b40)

            dispose()
        }.encode(PNG)
    }
    private suspend fun generate40Scores(list: List<PlayScore>, title: String, info: PlayerData): ByteArray {
        val b40 = images[MaimaiConfig.b40.bg]!!.clone()
        val realRating = info.rating + info.additional_rating
        return b40.context2d {
            val ratingBg = images["rating_base_${MaiDataConverter.ratingColor(realRating)}"]!!
            drawImage(ratingBg, MaimaiConfig.b40.ratingBg.x, MaimaiConfig.b40.ratingBg.y)

            fillText(info.nickname.toSBC(), MaimaiConfig.b40.name)
            fillText(realRating.toString().toList().joinToString(" "), MaimaiConfig.b40.dxrating,
                Colors.YELLOW, TextAlignment(HorizontalAlign.RIGHT, VerticalAlign.MIDDLE))
            fillText(title, MaimaiConfig.b40.ratingDetail, align=TextAlignment.CENTER)

            drawCharts(list.take(25).fillEmpty(25), 5,
                MaimaiConfig.b40.leftPart.x, MaimaiConfig.b40.leftPart.y, 8, MaimaiConfig.b40)
            drawCharts(list.subList(25, 40).fillEmpty(15), 3,
                MaimaiConfig.b40.rightPart.x, MaimaiConfig.b40.rightPart.y, 8, MaimaiConfig.b40)

            dispose()
        }.encode(PNG)
    }
    private suspend fun generateB50Image(info: PlayerData): ByteArray {
        val b50 = images[MaimaiConfig.b50.bg]!!.clone()
        info.charts.values.forEach { type ->
            type.fastForEach {
                it.dxScore = MaiDataConverter.getNewRa(it.ds, it.achievements)
            }
        }
        val realRating = info.charts["sd"]!!.sumOf { it.dxScore } + info.charts["dx"]!!.sumOf { it.dxScore }
        return b50.context2d {
            val ratingBg = images["rating_base_${MaiDataConverter.ratingColor(realRating, true)}"]!!
            drawImage(ratingBg, MaimaiConfig.b50.ratingBg.x, MaimaiConfig.b50.ratingBg.y)

            fillText(info.nickname.toSBC(), MaimaiConfig.b50.name)
            fillText(realRating.toString().toList().joinToString(" "), MaimaiConfig.b50.dxrating,
                Colors.YELLOW, TextAlignment(HorizontalAlign.RIGHT, VerticalAlign.MIDDLE))
            fillText("根据海外maimai拟构的b50", MaimaiConfig.b50.ratingDetail,
                align=TextAlignment.CENTER)

            drawCharts(info.charts["sd"]!!.fillEmpty(35), 7,
                MaimaiConfig.b50.leftPart.x, MaimaiConfig.b50.leftPart.y, 8, MaimaiConfig.b50)
            drawCharts(info.charts["dx"]!!.fillEmpty(15), 3,
                MaimaiConfig.b50.rightPart.x, MaimaiConfig.b50.rightPart.y, 8, MaimaiConfig.b50)

            dispose()
        }.encode(PNG)
    }
    fun resolveCover(title: String): Bitmap {
        return images[title.md5()] ?: images["default"]!!
    }
    fun resolveCoverFile(title: String): File {
        val file = OtomadBotCore.configFolder.resolve("image/maimai/cover/${title.md5()}.jpg")
        return if (file.exists()) file else  OtomadBotCore.configFolder.resolve("image/maimai/cover/default.jpg")
    }
}

object MaiDataConverter {
    fun ratingColor(rating: Int, b50: Boolean = false): String = if (b50) {
        when (rating) {
            in (0..1999) -> "blue" // Actually 0~1000 another color
            in (2000..3999) -> "green"
            in (4000..6999) -> "orange"
            in (7000..9999) -> "red"
            in (10000..11999) -> "purple"
            in (12000..12999) -> "bronze"
            in (13000..13999) -> "silver"
            in (14000..14499) -> "gold"
            in (14500..14999) -> "gold" // Actually another color
            in (15000..40000) -> "rainbow"
            else -> "blue"
        }
    } else when (rating) { // TODO: 找到白框文件名
        in (0..1999) -> "blue"
        in (2000..2999) -> "green"
        in (3000..3999) -> "orange"
        in (4000..4999) -> "red"
        in (5000..5999) -> "purple"
        in (6000..6999) -> "bronze"
        in (7000..7999) -> "silver"
        in (8000..8499) -> "gold"
        in (8500..20000) -> "rainbow"
        else -> "blue"
    }
    fun difficulty2Name(id: Int, english: Boolean = true): String {
        return if (english) {
            when (id) {
                0 -> "Bas"
                1 -> "Adv"
                2 -> "Exp"
                3 -> "Mst"
                4 -> "ReM"
                else -> ""
            }
        } else {
            when (id) {
                0 -> "绿"
                1 -> "黄"
                2 -> "红"
                3 -> "紫"
                4 -> "白"
                else -> ""
            }
        }
    }
    fun getNewRa(ds: Double, achievement: Double): Int {
        val baseRa = when (achievement) {
            in 0.0..49.9999 ->  7.0
            in 50.0..59.9999 -> 8.0
            in 60.0..69.9999 -> 9.6
            in 70.0..74.9999 -> 11.2
            in 75.0..79.9999 -> 12.0
            in 80.0..89.9999 -> 13.6
            in 90.0..93.9999 -> 15.2
            in 94.0..96.9999 -> 16.8
            in 97.0..97.9999 -> 20.0
            in 98.0..98.9999 -> 20.3
            in 99.0..99.4999 -> 20.8
            in 99.5..99.9999 -> 21.1
            in 100.0..100.4999 -> 21.6
            in 100.5..101.0 -> 22.4
            else -> 0.0
        }
        return (ds * (minOf(100.5, achievement) / 100) * baseRa).toIntFloor()
    }
}

fun Context2d.fillText(text: String, attr: MaiPicPosition, color: RGBA = Colors.BLACK,
                       align: TextAlignment = TextAlignment.LEFT) {
    font(fonts[attr.fontName], align.horizontal, align.vertical, attr.fontSize.toDouble()) {
        fillStyle(color) {
            drawText(text, attr.x.toDouble(), attr.y.toDouble(), fill=true)
        }
    }
}
fun Context2d.fillTextRelative(text: String, x: Int, y: Int, attr: MaiPicPosition,
                               color: RGBA = Colors.BLACK, align: TextAlignment = TextAlignment.LEFT) {
    font(fonts[attr.fontName], align.horizontal, align.vertical, attr.fontSize.toDouble()) {
        fillStyle(color) {
            drawText(text, x + attr.x.toDouble(), y + attr.y.toDouble(), fill=true)
        }
    }
}
fun Context2d.drawCharts(charts: List<PlayScore>, cols: Int, startX: Int, startY: Int, gap: Int
                                 , config: MaimaiBestPicConfig) {
    charts.sortedWith(compareBy({ -it.ra }, { it.achievements })).forEachIndexed { index, chart ->
        val coverRaw = MaimaiDXHandler.resolveCover(chart.title).toBMP32()
            .scaleLinear(config.coverScale, config.coverScale)
        val newHeight = (coverRaw.width / config.coverRatio).roundToInt()
        val cover = coverRaw.sliceWithSize(0, (coverRaw.height - newHeight) / 2, coverRaw.width, newHeight)
            .extract().blurFixedSize(4).brightness(-0.05f)
        val x = startX + (index % cols) * (coverRaw.width + gap)
        val y = startY + (index / cols) * (newHeight + gap)

        state.fillStyle = Colors.BLACK // MAGIC COLOR(?)
        fillRect(x + config.shadow.x, y + config.shadow.y, coverRaw.width, newHeight)
        drawImage(cover, x, y)

        if (chart.title != "") {
            drawImage(
                MaimaiDXHandler.images["label_${chart.level_label.replace(":", "")}"]!!
                    .toBMP32().scaleLinear(config.label.scale, config.label.scale),
                x+config.label.x, y+config.label.y) // Difficulty

            // Details
            fillTextRelative(chart.title.ellipsize(12), x, y, config.chTitle, Colors.WHITE)
            fillTextRelative(chart.achievements.toString().limitDecimal(4) + "%", x, y,
                config.chAchievements, Colors.WHITE)
            fillTextRelative("Base: ${chart.ds} -> ${chart.ra}", x, y, config.chBase, Colors.WHITE)
            fillTextRelative("#${index + 1}(${chart.type})", x, y, config.chRank, Colors.WHITE)

            drawImage(MaimaiDXHandler.images["music_icon_${chart.rate}"]!!.toBMP32()
                .scaleLinear(config.rateIcon.scale, config.rateIcon.scale),
                x + config.rateIcon.x, y + config.rateIcon.y)
            if (chart.fc.isNotEmpty()) {
                drawImage(MaimaiDXHandler.images["music_icon_${chart.fc}"]!!.toBMP32()
                    .scaleLinear(config.fcIcon.scale, config.fcIcon.scale),
                    x + config.fcIcon.x, y + config.fcIcon.y)
            }
        }
    }
}
fun Bitmap32.blurFixedSize(radius: Int) = applyEffect(BitmapEffect(radius))
    .sliceWithSize(radius, radius, width, height).extract()
fun Bitmap32.brightness(ratio: Float = 0.6f): Bitmap32 {
    if (ratio > 1f || ratio < -1f)
        throw IllegalArgumentException("Ratio must be in [-1, 1]")
    val real = ratio / 2f + 0.5f
    updateColors {
        it.times(RGBA.float(real, real, real, 1f))
    }
    return this
}

fun String.ellipsize(max: Int): String {
    var result = ""
    var cnt = 0
    forEach {
        cnt += if (it.isDBC()) 1 else 2
        if (cnt > max) return@forEach
        result += it
    }
    return result + if (result.length != length) "…" else ""
}

@Serializable
class MaiPicPosition(val fontName: String = "", val fontSize: Int = 0, val x: Int, val y: Int,
                     val hAlign: TextHAlign = TextHAlign.LEFT, val scale: Double = 1.0)

object MaimaiConfig: AutoSavePluginConfig("maimai") {
    val b40 by value(MaimaiBestPicConfig("dx2021_otmbot",
        MaiPicPosition("FZYouHei 513B.ttf", 30, 84, 45),
        MaiPicPosition("ariblk.ttf", 16, 529, 39, TextHAlign.RIGHT),
        MaiPicPosition("bb4171.ttf", 20, 225, 102, hAlign = TextHAlign.CENTER),
        MaiPicPosition("FZYouHei 513B.ttf", 18, 8, 22),
        MaiPicPosition("FZYouHei 513B.ttf", 16, 8, 44),
        MaiPicPosition("FZYouHei 513B.ttf", 16, 8, 60),
        MaiPicPosition("FZYouHei 513B.ttf", 18, 8, 80),
        MaiPicPosition(x = -19, y = 0, scale = 1.0),
        MaiPicPosition(x = 85, y = 25, scale = 0.8),
        MaiPicPosition(x = 130, y = 25, scale = 0.5),
        MaiPicPosition(x = 2, y = 2),
        MaiPicPosition(x = 69, y = 210),
        MaiPicPosition(x = 936, y = 210),
        MaiPicPosition(x = 451, y = 16),
        16.0 / 9, 16.0 / 19
    ))
    val b50 by value(MaimaiBestPicConfig("universe_otmbot",
        MaiPicPosition("FZYouHei 513B.ttf", 30, 484, 60),
        MaiPicPosition("ariblk.ttf", 16, 920, 55, TextHAlign.RIGHT),
        MaiPicPosition("bb4171.ttf", 20, 600, 124, hAlign = TextHAlign.CENTER),
        MaiPicPosition("FZYouHei 513B.ttf", 24, 10, 28),
        MaiPicPosition("FZYouHei 513B.ttf", 20, 10, 51),
        MaiPicPosition("FZYouHei 513B.ttf", 20, 10, 71),
        MaiPicPosition("FZYouHei 513B.ttf", 24, 10, 95),
        MaiPicPosition(x = -21, y = 0, scale = 19.0 / 16),
        MaiPicPosition(x = 110, y = 30, scale = 0.9),
        MaiPicPosition(x = 160, y = 28, scale = 0.7),
        MaiPicPosition(x = 4, y = 4),
        MaiPicPosition(x = 10, y = 258),
        MaiPicPosition(x = 1444, y = 258),
        MaiPicPosition(x = 858, y = 33),
        16.0 / 9, 1.0
    ))
}
@Serializable
class MaimaiBestPicConfig(
    val bg: String, val name: MaiPicPosition, val dxrating: MaiPicPosition, val ratingDetail: MaiPicPosition,
    val chTitle: MaiPicPosition, val chAchievements: MaiPicPosition, val chBase: MaiPicPosition,
    val chRank: MaiPicPosition, val label: MaiPicPosition, val rateIcon: MaiPicPosition, val fcIcon: MaiPicPosition,
    val shadow: MaiPicPosition, val leftPart: MaiPicPosition, val rightPart: MaiPicPosition,
    val ratingBg: MaiPicPosition, val coverRatio: Double, val coverScale: Double
)