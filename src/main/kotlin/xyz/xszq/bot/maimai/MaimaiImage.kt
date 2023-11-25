package xyz.xszq.bot.maimai

import com.soywiz.kmem.toIntCeil
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.bitmap.sliceWithSize
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.font.TtfFont
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.encode
import com.soywiz.korim.format.readNativeImage
import com.soywiz.korim.text.TextAlignment
import com.soywiz.korim.vector.Context2d
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.baseName
import com.soywiz.korio.file.std.tempVfs
import com.soywiz.korio.file.writeToFile
import com.soywiz.korio.net.MimeType
import com.soywiz.korio.net.mimeType
import io.github.oshai.kotlinlogging.KLogger
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.mamoe.yamlkt.Yaml
import xyz.xszq.bot.maimai.MaimaiUtils.acc2rate
import xyz.xszq.bot.maimai.MaimaiUtils.difficulty2Color
import xyz.xszq.bot.maimai.MaimaiUtils.genre2filename
import xyz.xszq.bot.maimai.MaimaiUtils.getNewRa
import xyz.xszq.bot.maimai.MaimaiUtils.levelIndex2Label
import xyz.xszq.bot.maimai.MaimaiUtils.levels
import xyz.xszq.bot.maimai.MaimaiUtils.plateExcluded
import xyz.xszq.bot.maimai.MaimaiUtils.rating2dani
import xyz.xszq.bot.maimai.MaimaiUtils.ratingColor
import xyz.xszq.bot.maimai.payload.*
import xyz.xszq.nereides.ellipsize
import xyz.xszq.nereides.limitDecimal
import xyz.xszq.nereides.toDBC
import xyz.xszq.nereides.toSBC
import kotlin.math.min
import kotlin.math.roundToInt

class MaimaiImage(val musics: MusicsInfo, val logger: KLogger, private val resourceDir: VfsFile) {
    private val imageCache = mutableMapOf<String, Bitmap>()
    private lateinit var theme: Theme

    private val sysFonts = MultiPlatformNativeSystemFontProvider(resourceDir["font"].absolutePath)
    private val json = Json {
        prettyPrint = true; isLenient = true; ignoreUnknownKeys = true
    }
    private val yaml = Yaml { }
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
        expectSuccess = false
    }

    suspend fun load(themeName: String) {
        // 加载主题文件
        theme = yaml.decodeFromString(resourceDir["themes/$themeName/theme.yml"].readString())
        resourceDir["themes/$themeName"].listRecursive().collect {
            if (it.mimeType() !in listOf(MimeType.IMAGE_JPEG, MimeType.IMAGE_PNG))
                return@collect
            runCatching {
                imageCache[it.baseName] = it.readNativeImage()
            }.onFailure { e ->
                e.printStackTrace()
            }
        }
        // 加载字体

        theme.b50.pos.filterNot { it.value.fontName.isBlank() }.forEach {
            fonts[it.value.fontName] = sysFonts.locateFontByName(it.value.fontName) ?: sysFonts.defaultFont()
        }
        theme.scoreList.pos.filterNot { it.value.fontName.isBlank() }.forEach {
            fonts[it.value.fontName] = sysFonts.locateFontByName(it.value.fontName) ?: sysFonts.defaultFont()
        }
        theme.dsList.pos.filterNot { it.value.fontName.isBlank() }.forEach {
            fonts[it.value.fontName] = sysFonts.locateFontByName(it.value.fontName) ?: sysFonts.defaultFont()
        }
        // 加载封面
        val mutex = Mutex()
        coroutineScope {
            resourceDir["covers"].listRecursive().collect {
                if (it.mimeType() !in listOf(MimeType.IMAGE_JPEG, MimeType.IMAGE_PNG))
                    return@collect
                launch {
                    runCatching {
                        val image = it.readNativeImage()
                        mutex.withLock {
                            imageCache[it.baseName] = image
                        }
                    }.onFailure { e ->
                        e.printStackTrace()
                    }
                }
            }
        }
        System.gc()
    }

    fun getCoverById(id: String) =
        resourceDir["covers"]["$id.jpg"]

    suspend fun getCoverBitmap(id: String): Bitmap {
        if (imageCache.containsKey("$id.jpg"))
            return imageCache["$id.jpg"]!!

        return getImage("default_cover")
    }

    suspend fun getImage(name: String): Bitmap {
        listOf(name, "$name.png", "$name.jpg", name.split(".").first()).forEach {
            if (imageCache.containsKey(it))
                return imageCache[it]!!
        }
        return resourceDir[name].readNativeImage()
    }

    // TODO: 嵌套太多好丑
    suspend fun downloadCovers(config: MaimaiConfig, music: List<MusicInfo>) {
        val coversDir = resourceDir["covers"]
        if (!coversDir.exists())
            coversDir.mkdir()
        val semaphore = Semaphore(32)
        coroutineScope {
            val songs = client.get("${config.zetarakuSite}/maimai/data.json").body<ZetarakuResponse>()
            music.forEach { info ->
                val target = coversDir["${info.id}.jpg"]
                if (!target.exists()) {
                    launch {
                        semaphore.withPermit {
                            runCatching {
                                val imageName = songs.songs.first {
                                    it.title.clean() == info.title.clean() &&
                                            it.artist.clean() == info.basicInfo.artist.clean()
                                }.imageName
                                val response = client.get(
                                    "${config.zetarakuSite}/maimai/img/cover/$imageName"
                                ).body<HttpResponse>()
                                if (response.status == HttpStatusCode.OK) {
                                    response.readBytes().writeToFile(target)
                                    logger.info { "${info.id}. ${info.title} 封面下载完成" }
                                }
                            }.onFailure {
                                it.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun generateBest(info: PlayerData): ByteArray {
        val config = theme.b50
        val result = getImage(config.bg).clone()
        info.charts.values.forEach { type ->
            type.forEach {
                it.ra = getNewRa(it.ds, it.achievements)
            }
        }
        val realRating = info.charts["sd"]!!.sumOf { it.ra } + info.charts["dx"]!!.sumOf { it.ra }
        return result.context2d {
            drawImage(getImage("rating_base_${ratingColor(realRating)}.png"), config.pos.getValue("ratingBg").x, config.pos.getValue("ratingBg").y)
            drawText(info.nickname.toSBC(), config.pos.getValue("name"))
            drawText(realRating.toString().toList().joinToString(" "), config.pos.getValue("dxrating"),
                Colors.YELLOW, TextAlignment.RIGHT)
            if (info.additionalRating != null) {
                val dani = config.pos.getValue("dani")
                drawImage(getImage("dani_${rating2dani(info.additionalRating)}.png")
                    .toBMP32().scaleLinear(dani.scale, dani.scale), dani.x, dani.y)
            }

            drawCharts(PlayScore.fillEmpty(info.charts["sd"]!!, 35), config.oldCols,
                config.pos.getValue("oldCharts").x, config.pos.getValue("oldCharts").y, config)
            drawCharts(PlayScore.fillEmpty(info.charts["dx"]!!, 15), config.newCols,
                config.pos.getValue("newCharts").x, config.pos.getValue("newCharts").y, config)
            dispose()
        }.encode(PNG)
    }
    private suspend fun Context2d.drawCharts(charts: List<PlayScore>, cols: Int, startX: Int, startY: Int,
                                             config: ItemProperties, sort: Boolean = true
    ) {
        (if (sort) charts.sortedWith(compareBy({ -it.ra }, { -it.achievements }))
        else charts).forEachIndexed { index, chart ->
            val coverRaw = getCoverBitmap(chart.songId.toString())
                .toBMP32()
                .scaled(config.coverWidth, config.coverWidth, true)
            val newHeight = (coverRaw.width / config.coverRatio).roundToInt()
            var cover = coverRaw.sliceWithSize(0, (coverRaw.height - newHeight) / 2,
                coverRaw.width, newHeight).extract()
            cover = cover.blurFixedSize(2).brightness(-0.01f)
            val x = startX + (index % cols) * (cover.width + config.gapX)
            val y = startY + (index / cols) * (cover.height + config.gapY)

            state.fillStyle = Colors.BLACK // TODO: Make color changeable
            fillRect(x + config.pos.getValue("shadow").x, y + config.pos.getValue("shadow").y,
                cover.width, cover.height)
            drawImage(cover, x, y)
            if (chart.title != "") {
                val label = config.pos.getValue("label")
                drawImage(getImage("label_${chart.levelLabel.replace(":", "")}.png").toBMP32().scaleLinear(label.scale, label.scale), x + label.x, y + label.y)

                // Details
                drawTextRelative(chart.title.ellipsize(16), x, y, config.pos.getValue("chTitle"), Colors.WHITE)
                drawTextRelative(chart.achievements.toString().limitDecimal(4) + "%", x, y,
                    config.pos.getValue("chAchievements"), Colors.WHITE)
                drawTextRelative("Base: ${chart.ds} -> ${chart.ra}", x, y, config.pos.getValue("chBase"), Colors.WHITE)

                config.pos["type"] ?.let { type ->
                    drawImage(
                        getImage("type_${chart.type.lowercase()}.png").toBMP32()
                            .scaleLinear(type.scale, type.scale),
                        x + type.x, y + type.y)
                }

                config.pos["rateIcon"] ?.let { rateIcon ->
                    drawImage(
                        getImage("music_icon_${chart.rate}.png").toBMP32()
                            .scaleLinear(rateIcon.scale, rateIcon.scale),
                        x + rateIcon.x, y + rateIcon.y
                    )
                }
                if (chart.fc.isNotEmpty()) {
                    config.pos["fcIcon"] ?.let { fcIcon ->
                        drawImage(
                            getImage("music_icon_${chart.fc}.png").toBMP32()
                                .scaleLinear(fcIcon.scale, fcIcon.scale),
                            x + fcIcon.x, y + fcIcon.y
                        )
                    }
                }
                if (chart.fs.isNotEmpty()) {
                    config.pos["fsIcon"] ?.let { fsIcon ->
                        drawImage(
                            getImage("music_icon_${chart.fs}.png").toBMP32()
                                .scaleLinear(fsIcon.scale, fsIcon.scale),
                            x + fsIcon.x, y + fsIcon.y
                        )
                    }
                }
            }
        }
    }

    suspend fun generateDsList(level: String, bg: Bitmap, lock: Mutex) = withContext(Dispatchers.IO) {
        val raw = musics.map {
            it.level.mapIndexed { index, s -> if (s == level) Pair(it, index) else null }.filterNotNull()
        }.flatten()
        val songs = raw.map { it.first.ds[it.second] }.distinct().sortedDescending().associateWith { d ->
            raw.filter { m -> m.first.ds[m.second] == d }
        }
        val config = theme.dsList
        var nowY = config.pos.getValue("list").y
        bg.context2d {
            lock.withLock {
                drawText(level + "定数表", config.pos.getValue("title"), align = TextAlignment.CENTER)
            }
            songs.forEach { (ds, l) ->
                lock.withLock {
                    drawTextRelative(ds.toString(), config.pos.getValue("list").x, nowY, config.pos.getValue("ds"))
                }
                l.forEachIndexed { index, (m, difficulty) ->
                    val row = index / config.oldCols
                    val col = index % config.oldCols
                    val coverRaw = getCoverBitmap(m.id).toBMP32()
                        .scaled(config.coverWidth, config.coverWidth, true)
                    val newHeight = (coverRaw.width / config.coverRatio).roundToInt()
                    val cover = coverRaw.sliceWithSize(
                        0, (coverRaw.height - newHeight) / 2,
                        coverRaw.width, newHeight
                    ).extract()
                    val x = config.pos.getValue("list").x + col * (config.coverWidth + config.gapX)
                    val y = nowY + row * (config.coverWidth + config.gapY)
                    fillStyle = difficulty2Color[difficulty]
                    fillRect(x - 3, y - 3, cover.width + 6, cover.height + 6)
                    drawImage(cover, x, y)
                }
                nowY += (l.size * 1.0 / config.oldCols).toIntCeil() * (config.coverWidth + config.gapY) + config.gapY
            }
        }.sliceWithSize(0, 0, bg.width, nowY + config.gapY).extract()
    }

    suspend fun preGenerateDsList() {
        val lock = Mutex()
        val semaphore = Semaphore(2)
        coroutineScope {
            levels.forEachIndexed { _, level ->
                launch {
                    semaphore.withPermit {
                        val bg = lock.withLock {
                            getImage(theme.dsList.bg).clone()
                        }
                        val nowDs = generateDsList(level, bg, lock)
                        lock.withLock {
                            imageCache["ds/${level}.png"] = nowDs
                        }
                    }
                }
            }
        }
    }

    suspend fun dsProgress(level: String, data: List<BriefScore>): ByteArray {
        val records = data.filter {
            it.level == level
        }.filter {
            it.achievements > 79.9999
        }

        val img = getImage("ds/$level.png").clone()
        val raw = musics.map {
            it.level.mapIndexed { index, s -> if (s == level) Pair(it, index) else null }.filterNotNull()
        }.flatten()
        val songs = raw.map { it.first.ds[it.second] }.distinct().sortedDescending().associateWith { d ->
            raw.filter { m -> m.first.ds[m.second] == d }
        }
        val config = theme.dsList
        var nowY = config.pos.getValue("list").y
        return img.context2d {
            songs.forEach { (ds, l) ->
                drawTextRelative(ds.toString(), config.pos.getValue("list").x, nowY, config.pos.getValue("ds"))
                l.forEachIndexed { index, (m, difficulty) ->
                    records.find { it.id == m.id.toInt() && it.levelIndex == difficulty } ?.let { record ->
                        val row = index / config.oldCols
                        val col = index % config.oldCols
                        val x = config.pos.getValue("list").x + col * (config.coverWidth + config.gapX)
                        val y = nowY + row * (config.coverWidth + config.gapY)
                        val rateIcon = config.pos.getValue("rateIcon")
                        fillStyle = RGBA(0, 0, 0, 128)
                        fillRect(x, y, config.coverWidth, config.coverWidth)
                        drawImage(
                            getImage("music_icon_${acc2rate(record.achievements)}.png")
                                .toBMP32().scaleLinear(rateIcon.scale, rateIcon.scale),
                            x + rateIcon.x, y + rateIcon.y)
                    }
                }
                nowY += (l.size * 1.0 / config.oldCols).toIntCeil() * (config.coverWidth + config.gapY) + config.gapY
            }
        }.encode(PNG)
    }

    suspend fun plateProgress(version: String, type: String, vList: List<String>, data: List<BriefScore>): ByteArray {
        val raw = musics.filter { it.basicInfo.from in vList }.toMutableList()
        if (version == "真")
            raw.removeIf { it.id == "56" }
        raw.removeIf { it.id in plateExcluded }
        val songs = raw.map { it.level[3] }.distinct().sortedDescending().associateWith { d ->
            raw.filter { m -> m.level[3] == d }
        }
        val records = data.filter { it.achievements > 79.9999 }
        val config = theme.dsList
        val img = getImage(config.bg).clone()
        var nowY = config.pos.getValue("list").y
        return img.context2d {
            drawText("${version}${type}完成表", config.pos.getValue("title"), align=TextAlignment.CENTER)
            songs.forEach { (level, l) ->
                drawTextRelative(level, config.pos.getValue("list").x, nowY, config.pos.getValue("ds"))
                l.forEachIndexed { index, m ->
                    val row = index / config.oldCols
                    val col = index % config.oldCols
                    val coverRaw = getCoverBitmap(m.id).toBMP32()
                        .scaled(config.coverWidth, config.coverWidth, true)
                    val newHeight = (coverRaw.width / config.coverRatio).roundToInt()
                    val cover = coverRaw.sliceWithSize(0, (coverRaw.height - newHeight) / 2,
                        coverRaw.width, newHeight).extract()
                    val x = config.pos.getValue("list").x + col * (config.coverWidth + config.gapX)
                    val y = nowY + row * (config.coverWidth + config.gapY)
                    drawImage(cover, x, y)
                    records.find { it.id == m.id.toInt() && it.levelIndex == 3 } ?.let { record ->
                        when (type) {
                            "将" -> {
                                if (record.achievements > 99.9999) {
                                    fillStyle = RGBA(0, 0, 0, 128)
                                    fillRect(x, y, config.coverWidth, config.coverWidth)
                                }
                                val rateIcon = config.pos.getValue("rateIcon")
                                drawImage(
                                    getImage("music_icon_${acc2rate(record.achievements)}.png").toBMP32().scaleLinear(rateIcon.scale, rateIcon.scale),
                                    x + rateIcon.x, y + rateIcon.y
                                )
                            }
                            in listOf("极", "神") -> {
                                if ((type == "极" && record.fc.isNotEmpty()) || (type == "神" && record.fc in listOf("ap", "app"))) {
                                    fillStyle = RGBA(0, 0, 0, 128)
                                    fillRect(x, y, config.coverWidth, config.coverWidth)
                                }
                                if (record.fc.isEmpty())
                                    return@let
                                val rateIcon = config.pos.getValue("fcIcon")
                                drawImage(
                                    getImage("music_icon_${record.fc}.png")
                                        .toBMP32().scaleLinear(rateIcon.scale, rateIcon.scale),
                                    x + rateIcon.x, y + rateIcon.y
                                )
                            }
                            "舞舞" -> {
                                if (record.fs in listOf("fsd", "fsdp")) {
                                    fillStyle = RGBA(0, 0, 0, 128)
                                    fillRect(x, y, config.coverWidth, config.coverWidth)
                                }
                                if (record.fs.isEmpty())
                                    return@let
                                val rateIcon = config.pos.getValue("fcIcon")
                                drawImage(
                                    getImage("music_icon_${record.fs}.png")
                                        .toBMP32().scaleLinear(rateIcon.scale, rateIcon.scale),
                                    x + rateIcon.x, y + rateIcon.y
                                )
                            }
                        }
                    }
                }
                nowY += (l.size * 1.0 / config.oldCols).toIntCeil() * (config.coverWidth + config.gapY) + config.gapY
            }
        }.sliceWithSize(0, 0, img.width, nowY + config.gapY).extract().encode(PNG)
    }
    suspend fun getLevelRecordList(
        level: String,
        page: Int,
        basicInfo: PlayerData,
        verList: List<BriefScore>
    ): ByteArray {
        val leastDs =
            if (level.last() == '+') level.substringBefore('+').toInt() + 0.7
            else level.toDouble()
        val highestDs =
            if (level.last() == '+') level.substringBefore('+').toInt() + 0.91
            else leastDs + 0.61
        val data = verList.filter {
            musics.getById(it.id.toString())!!.ds[it.levelIndex] in leastDs .. highestDs
        }.map {
            val info = musics.getById(it.id.toString())!!
            PlayScore(it.achievements, info.ds[it.levelIndex], it.fc, it.fs,
                info.level[it.levelIndex], it.levelIndex, levelIndex2Label(it.levelIndex),
                getNewRa(info.ds[it.levelIndex], it.achievements), acc2rate(it.achievements),
                it.id, it.title, it.type)
        }.sortedWith(compareBy({ -it.achievements }, { -it.ra }))
        val pages = (data.size / 50.0).toIntCeil()
        val realPage = if (page in 1..pages) page else 1

        return generateList(level, basicInfo, data.subList((realPage - 1) * 50,
            min(realPage * 50, data.size)
        ), realPage, pages)
    }
    private suspend fun generateList(level: String, info: PlayerData, l: List<PlayScore>,
                                     nowPage: Int, totalPage: Int): ByteArray {
        val config = theme.scoreList
        val result = getImage(config.bg).clone()
        val realRating = info.charts["sd"]!!.sumOf { it.ra } + info.charts["dx"]!!.sumOf { it.ra }
        return result.context2d {
            drawText(info.nickname.toSBC(), config.pos.getValue("name"))
            drawImage(getImage("rating_base_${ratingColor(realRating)}.png"), config.pos.getValue("ratingBg").x, config.pos.getValue("ratingBg").y)
            drawText(info.nickname.toSBC(), config.pos.getValue("name"))
            drawText(realRating.toString().toList().joinToString(" "), config.pos.getValue("dxrating"),
                Colors.YELLOW, TextAlignment.RIGHT)
            if (info.additionalRating != null) {
                val dani = config.pos.getValue("dani")
                drawImage(getImage("dani_${rating2dani(info.additionalRating)}.png")
                    .toBMP32().scaleLinear(dani.scale, dani.scale), dani.x, dani.y)
            }
            drawText("${level}分数列表，第 $nowPage 页 (共 $totalPage 页)", config.pos.getValue("info"))

            drawCharts(PlayScore.fillEmpty(l, 50), config.oldCols,
                config.pos.getValue("oldCharts").x, config.pos.getValue("oldCharts").y, config)
            dispose()
        }.encode(PNG)
    }
    suspend fun musicInfo(id: String, records: List<BriefScore>) = withContext(Dispatchers.IO) {
        val songInfo = musics.getById(id) ?: return@withContext null
        val songRecords = records.filter { it.id.toString() == id }
        val config = theme.info
        val result = getImage(config.bg).clone()
        result.context2d {
            val cover = getCoverBitmap(songInfo.id).toBMP32()
                .scaled(config.coverWidth, config.coverWidth, true)
            val x = config.pos.getValue("cover").x
            val y = config.pos.getValue("cover").y
            drawImage(cover, x, y)
            drawText(songInfo.basicInfo.title, config.pos.getValue("title"))
            drawText(songInfo.basicInfo.artist, config.pos.getValue("artist"))
            val details = buildString {
                append("ID: $id")
                append("　　")
                append("BPM: " + songInfo.basicInfo.bpm)
            }
            drawText(details, config.pos.getValue("details"))

            val genre = config.pos.getValue("genre")
            drawImage(getImage(
                "category_${genre2filename(songInfo.basicInfo.genre)}.png").toBMP32()
                .scaleLinear(genre.scale, genre.scale), genre.x, genre.y)

            val startX = config.pos.getValue("list").x
            val startY = config.pos.getValue("list").y
            for (i in 0 .. 4) {
                val nowY = startY + i * config.gapY
                drawTextRelative(songInfo.ds[i].toString(),
                    startX, nowY, config.pos.getValue("ds"), Colors.WHITE, TextAlignment.CENTER)
                if (i >= songInfo.ds.size) {
                    drawTextRelative("无该难度",
                        startX, nowY, config.pos.getValue("diffInfo"), Colors.WHITE)
                    continue
                }
                songRecords.firstOrNull { it.levelIndex == i } ?.let { record ->
                    drawTextRelative("${record.achievements.toString().limitDecimal(4)}%",
                        startX, nowY, config.pos.getValue("diffInfo"), Colors.WHITE)

                    val rateIcon = config.pos.getValue("rateIcon")
                    getImage("music_icon_${acc2rate(record.achievements)}.png").let {
                        drawImage(it.toBMP32().scaleLinear(rateIcon.scale, rateIcon.scale),
                            startX + rateIcon.x, nowY + rateIcon.y)
                    }
                    if (record.fc.isNotEmpty()) {
                        val fcIcon = config.pos.getValue("fcIcon")
                        drawImage(
                            getImage("music_icon_${record.fc}.png")
                                .toBMP32().scaleLinear(fcIcon.scale, fcIcon.scale),
                            startX + fcIcon.x, nowY + fcIcon.y)
                    }
                    if (record.fs.isNotEmpty()) {
                        val fsIcon = config.pos.getValue("fsIcon")
                        drawImage(
                            getImage("music_icon_${record.fs}.png")
                                .toBMP32().scaleLinear(fsIcon.scale, fsIcon.scale),
                            startX + fsIcon.x, nowY + fsIcon.y)
                    }
                } ?: run {
                    drawTextRelative("您未游玩过该谱面",
                        startX, nowY, config.pos.getValue("diffInfo"), Colors.WHITE)
                }
            }
        }.encode(PNG)
    }
    suspend fun generateAP50(basicInfo: PlayerData, records: List<BriefScore>): ByteArray {
        val config = theme.b50
        val result = getImage(config.bg).clone()
        val info = PlayerData(basicInfo.nickname, basicInfo.rating, basicInfo.additionalRating,
            basicInfo.username,
            buildMap {
                set("sd",
                    PlayScore.fillEmpty(records.filter {
                        !musics.getById(it.id.toString())!!.basicInfo.isNew && it.fc.startsWith("ap")
                    }.map {
                        val info = musics.getById(it.id.toString())!!
                        PlayScore(it.achievements, info.ds[it.levelIndex], it.fc, it.fs,
                            info.level[it.levelIndex], it.levelIndex, levelIndex2Label(it.levelIndex),
                            getNewRa(info.ds[it.levelIndex], it.achievements), acc2rate(it.achievements),
                            it.id, it.title, it.type)
                    }.sortedWith(compareBy({ -it.achievements }, { -it.ra })).take(35), 35)
                )
                set("dx",
                    PlayScore.fillEmpty(records.filter {
                        musics.getById(it.id.toString())!!.basicInfo.isNew && it.fc.startsWith("ap")
                    }.map {
                        val info = musics.getById(it.id.toString())!!
                        PlayScore(it.achievements, info.ds[it.levelIndex], it.fc, it.fs,
                            info.level[it.levelIndex], it.levelIndex, levelIndex2Label(it.levelIndex),
                            getNewRa(info.ds[it.levelIndex], it.achievements), acc2rate(it.achievements),
                            it.id, it.title, it.type)
                    }.sortedWith(compareBy({ -it.achievements }, { -it.ra })).take(15), 15)
                )
            }
        )
        info.charts.values.forEach { type ->
            type.forEach {
                it.ra = getNewRa(it.ds, it.achievements)
            }
        }
        val realRating = info.charts["sd"]!!.sumOf { it.ra } + info.charts["dx"]!!.sumOf { it.ra }
        return result.context2d {
            drawImage(getImage("rating_base_${ratingColor(realRating)}.png"), config.pos.getValue("ratingBg").x, config.pos.getValue("ratingBg").y)
            drawText(info.nickname.toSBC(), config.pos.getValue("name"))
            drawText(realRating.toString().toList().joinToString(" "), config.pos.getValue("dxrating"),
                Colors.YELLOW, TextAlignment.RIGHT)
            if (info.additionalRating != null) {
                val dani = config.pos.getValue("dani")
                drawImage(getImage("dani_${rating2dani(info.additionalRating)}.png")
                    .toBMP32().scaleLinear(dani.scale, dani.scale), dani.x, dani.y)
            }

            drawCharts(PlayScore.fillEmpty(info.charts["sd"]!!, 35), config.oldCols,
                config.pos.getValue("oldCharts").x, config.pos.getValue("oldCharts").y, config)
            drawCharts(PlayScore.fillEmpty(info.charts["dx"]!!, 15), config.newCols,
                config.pos.getValue("newCharts").x, config.pos.getValue("newCharts").y, config)
            dispose()
        }.encode(PNG)
    }

    private fun String.clean(): String {
        if (this == "Link(CoF)")
            return "Link"
        var result = this.toDBC()
        while ("  " in result)
            result = result.replace("  ", " ")
        if (result.isBlank())
            return ""
        return result
    }

    companion object {
        val fonts = mutableMapOf<String, TtfFont>()
    }
}