package xyz.xszq.bot.maimai

import com.soywiz.kmem.toIntCeil
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.font.TtfFont
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.encode
import com.soywiz.korim.format.readNativeImage
import com.soywiz.korim.text.TextAlignment
import com.soywiz.korim.vector.Context2d
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.baseName
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
import xyz.xszq.bot.maimai.MaimaiUtils.genre2filename
import xyz.xszq.bot.maimai.MaimaiUtils.getNewRa
import xyz.xszq.bot.maimai.MaimaiUtils.getPlateFilename
import xyz.xszq.bot.maimai.MaimaiUtils.levelIndex2Label
import xyz.xszq.bot.maimai.MaimaiUtils.levels
import xyz.xszq.bot.maimai.MaimaiUtils.plateExcluded
import xyz.xszq.bot.maimai.MaimaiUtils.plateVerToVerId
import xyz.xszq.bot.maimai.MaimaiUtils.rating2dani
import xyz.xszq.bot.maimai.MaimaiUtils.ratingColor
import xyz.xszq.bot.maimai.payload.*
import xyz.xszq.nereides.limitDecimal
import xyz.xszq.nereides.toDBC
import xyz.xszq.nereides.toSBC
import kotlin.math.min

class MaimaiImage(
    private val musics: MusicsInfo,
    private val logger: KLogger,
    private val resourceDir: VfsFile
) {
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

    suspend fun loadThemeConfig(themeName: String) {
        // 加载主题文件
        theme = yaml.decodeFromString(resourceDir["themes/$themeName/theme.yml"].readString())
    }
    suspend fun load(themeName: String) {
        // 加载主题文件
        loadThemeConfig(themeName)
        // 加载字体

        theme.b50.pos.filterNot { it.value.fontName.isBlank() }.forEach {
            fonts[it.value.fontName] = sysFonts.locateFontByName(it.value.fontName) ?: sysFonts.defaultFont()
        }
        theme.dsList.pos.filterNot { it.value.fontName.isBlank() }.forEach {
            fonts[it.value.fontName] = sysFonts.locateFontByName(it.value.fontName) ?: sysFonts.defaultFont()
        }
        // 加载图片
        val mutex = Mutex()
        coroutineScope {
            listOf(resourceDir["themes/$themeName"], resourceDir["covers"]).forEach { dir ->
                dir.listRecursive().collect {
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
    fun contains(name: String): Boolean = imageCache.containsKey(name)

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

    suspend fun generateBest(info: PlayerData, openId: String): ByteArray {
        val config = theme.b50
        val result = getImage(config.bg).clone()
        info.charts.values.forEach { type ->
            type.forEach {
                it.ra = getNewRa(it.ds, it.achievements)
            }
        }
        val realRating = info.charts["sd"]!!.sumOf { it.ra } + info.charts["dx"]!!.sumOf { it.ra }
        val settings = CustomSettings.getSettings(openId)
        return result.context2d {
            kotlin.runCatching {
                drawHeader(info, config, realRating, settings)
                config.pos["ratingInfo"] ?.let {  config ->
                    drawText(
                        "${info.charts["sd"]!!.sumOf { it.ra }} + ${info.charts["dx"]!!.sumOf { it.ra }} = $realRating",
                        config,
                        TextAlignment.CENTER
                    )
                }
                drawCharts(PlayScore.fillEmpty(info.charts["sd"]!!, 35), config.oldCols,
                    config.pos.getValue("oldCharts").x, config.pos.getValue("oldCharts").y, config)
                drawCharts(PlayScore.fillEmpty(info.charts["dx"]!!, 15), config.newCols,
                    config.pos.getValue("newCharts").x, config.pos.getValue("newCharts").y, config)
                config.pos["newIcon"] ?.let { newIcon ->
                    drawImage(getImage("b50_icon_new.png").toBMP32(), newIcon.x, newIcon.y)
                }
                dispose()
            }.onFailure {
                it.printStackTrace()
            }
        }.encode(PNG)
    }
    private suspend fun Context2d.drawHeader(
        info: PlayerData,
        config: TemplateProperties,
        realRating: Int,
        settings: Map<String, String>
    ) {
        config.pos["plate"] ?.let { plateConfig ->
            val plateFilename = settings["PLATE_FILENAME"]
            var plate = if (settings["IS_PREFERRING_PROBER_PLATE"]?.toBoolean() == true)
                getPlateFilename(info.plate) ?: plateFilename ?: "UI_Plate_000011.png"
            else
                plateFilename ?: getPlateFilename(info.plate) ?: "UI_Plate_000011.png"
            if (!contains(plate))
                plate = "UI_Plate_000011.png"
            drawImage(getImage(plate), plateConfig.x, plateConfig.y)
        }
        config.pos["headerBg"] ?.let { header ->
            drawImage(getImage("b50_header_bg.png"), header.x, header.y)
        }
        config.pos["icon"] ?.let { iconConfig ->
            val iconFilename = settings["ICON_FILENAME"]
            var icon = iconFilename ?:  "UI_Icon_000101.png"
            if (!contains(icon))
                icon = "UI_Icon_000101.png"
            drawImage(getImage(icon), iconConfig.x, iconConfig.y)
        }
        drawImage(
            getImage("rating_base_${ratingColor(realRating)}.png"),
            config.pos.getValue("ratingBg").x,
            config.pos.getValue("ratingBg").y
        )
        drawText(info.nickname.toSBC(), config.pos.getValue("name"))
        drawText(
            realRating.toString().toList().joinToString(" "),
            config.pos.getValue("dxrating"),
            TextAlignment.RIGHT
        )
        if (info.additionalRating != null) {
            val dani = config.pos.getValue("dani")
            drawImage(
                getImage("dani_${rating2dani(info.additionalRating)}.png"),
                dani.x,
                dani.y
            )
        }
    }
    private suspend fun Context2d.drawCharts(charts: List<PlayScore>, cols: Int, startX: Int, startY: Int,
                                             config: TemplateProperties, sort: Boolean = true
    ) {
        (if (sort) charts.sortedWith(compareBy({ -it.ra }, { -it.achievements }))
        else charts).forEachIndexed { index, chart ->
            val musicInfo = musics.getById(chart.songId.toString())
            val cover = getCoverBitmap(chart.songId.toString())
                .toBMP32()
                .scaled(config.coverWidth, config.coverWidth, true)
            val bg = getImage("bg_${chart.levelLabel.replace(":", "")}.png").toBMP32()

            val x = startX + (index % cols) * (bg.width + config.gapX)
            val y = startY + (index / cols) * (bg.height + config.gapY)

            drawImage(bg, x, y)
            config.pos["cover"] ?.let { coverConfig ->
                drawImage(cover, x + coverConfig.x, y + coverConfig.y)
            }
            if (chart.title != "") {
                // Details
                drawTextRelative(
                    chart.title,
                    x,
                    y,
                    config.pos.getValue("chTitle"),
                    ellipsizeWidth = config.ellipsizeWidth ?.toDouble()
                )
                drawTextRelative(chart.achievements.toString().limitDecimal(4) + "%", x, y,
                    config.pos.getValue("chAchievements"))
                drawTextRelative("#$index", x, y, config.pos.getValue("chRank"))
                drawTextRelative("${chart.ds} → ${chart.ra}", x, y, config.pos.getValue("chBase"))
                config.pos["chDXScore"] ?.let { config ->
                    drawTextRelative(
                        "${chart.dxScore}/${musicInfo?.getDXScoreMax(chart.levelIndex)}",
                        x,
                        y,
                        config
                    )
                }

                config.pos["type"] ?.let { type ->
                    if (chart.type.uppercase() == "DX") {
                        drawImage(
                            getImage("DX.png").toBMP32(),
                            x + type.x, y + type.y)
                    }
                }

                config.pos["dxStar"] ?.let { rateIcon ->
                    musicInfo ?: return@let
                    drawImage(
                        getImage("b50_icon_dxstar_${musicInfo.getDXStar(chart.dxScore, chart.levelIndex)}.png")
                            .toBMP32(),
                        x + rateIcon.x, y + rateIcon.y
                    )
                }
                config.pos["rateIcon"] ?.let { rateIcon ->
                    drawImage(
                        getImage("b50_icon_${chart.rate}.png").toBMP32(),
                        x + rateIcon.x, y + rateIcon.y
                    )
                }
                config.pos["fcIcon"] ?.let { fcIcon ->
                    drawImage(
                        getImage(if (chart.fc.isEmpty()) "b50_icon_none.png" else "b50_icon_${chart.fc}.png").toBMP32(),
                        x + fcIcon.x, y + fcIcon.y
                    )
                }
                config.pos["fsIcon"] ?.let { fsIcon ->
                    drawImage(
                        getImage(if (chart.fs.isEmpty()) "b50_icon_none.png" else "b50_icon_${chart.fs}.png").toBMP32(),
                        x + fsIcon.x, y + fsIcon.y
                    )
                }
            }
        }
    }
    private fun <A, B> getSize(songs: Map<A, List<B>>, config: TemplateProperties): Pair<Int, Int> {
        val minWidth = 1280
        (15..23).forEach { cols ->
            val realHeight = songs.map { (_, l) ->
                (l.size * 1.0 / cols).toIntCeil() * (config.coverWidth + config.gapY)
            }.sum() + config.pos.getValue("list").y +  +
            config.pos.getValue("lineGap").y * (songs.size - 1)
            val realWidth = minWidth + (cols - 15) * config.pos.getValue("list").x / 2
            if (realHeight <= realWidth - 50)
                return Pair(cols, realWidth)
        }
        return Pair(23, 1850)
    }

    suspend fun drawDsList(level: String) = withContext(Dispatchers.IO) {
        val config = theme.dsList
        var nowY = config.pos.getValue("list").y
        val songs = musics.getSongWithDSByLevel(level)
        val (cols, size) = getSize(songs, config)
        val image =
            if (size != 1280) getImage(theme.dsList.bg).clone().toBMP32().scaled(size, size, true)
            else getImage(theme.dsList.bg).clone()
        val localLock = Mutex()
        image.context2d {
            localLock.withLock {
                val levelConfig = config.pos.getValue("level")
                drawImage(
                    getImage("ds_level_${level.replace('+', 'p')}.png"),
                    levelConfig.x,
                    levelConfig.y
                )
                val botInfoConfig = config.pos.getValue("botInfo")
                drawImage(
                    getImage("ds_bot_info.png"),
                    width - botInfoConfig.x,
                    botInfoConfig.y
                )
            }
            songs.forEach { (ds, l) ->
                localLock.withLock {
                    val dsBg = config.pos.getValue("dsBg")
                    drawImage(
                        getImage("ds_level.png"),
                        config.pos.getValue("list").x + dsBg.x,
                        nowY + dsBg.y
                    )
                    drawTextRelative(
                        ds.toString(),
                        config.pos.getValue("list").x,
                        nowY,
                        config.pos.getValue("dsText"),
                        TextAlignment.CENTER
                    )
                }
                coroutineScope {
                    l.forEachIndexed { index, (m, difficulty) ->
                        launch {
                            val row = index / cols
                            val col = index % cols
                            val cover = getCoverBitmap(m.id)
                                .toBMP32()
                                .scaled(config.coverWidth, config.coverWidth, true)
                            val x = config.pos.getValue("list").x + col * (config.coverWidth + config.gapX)
                            val y = nowY + row * (config.coverWidth + config.gapY)

                            val coverBg = config.pos.getValue("coverBg")
                            localLock.withLock {
                                drawImage(
                                    getImage("ds_bg_${levelIndex2Label(difficulty).replace(":", "")}.png"),
                                    x + coverBg.x,
                                    y + coverBg.y
                                )
                            }
                            localLock.withLock {
                                drawImage(cover, x, y)
                            }
                        }
                    }
                }
                nowY += (l.size * 1.0 / cols).toIntCeil() * (config.coverWidth + config.gapY) +
                        config.pos.getValue("lineGap").y
            }
        }
    }

    suspend fun preGenerateDsList() {
        val lock = Mutex()
        val semaphore = Semaphore(2)
        coroutineScope {
            levels.forEachIndexed { _, level ->
                launch {
                    semaphore.withPermit {
                        val nowDs = drawDsList(level)
                        lock.withLock {
                            imageCache["ds/${level}_raw.png"] = nowDs
                        }
                        lock.withLock {
                            imageCache["ds/${level}.png"] = nowDs.clone().context2d {
                                val titleConfig = theme.dsList.pos.getValue("title")
                                drawImage(
                                    getImage("ds_title.png"),
                                    titleConfig.x,
                                    titleConfig.y
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    private fun getAllClearType(rate: Boolean, song: List<BriefScore>): String? {
        return if (rate) {
            when {
                song.all { it.achievements > 100.4999 } -> "sssp"
                song.all { it.achievements > 99.9999 } -> "sss"
                song.all { it.achievements > 99.4999 } -> "ssp"
                song.all { it.achievements > 98.9999 } -> "ss"
                song.all { it.achievements > 97.9999 } -> "sp"
                song.all { it.achievements > 96.9999 } -> "s"
                else -> null
            }
        } else {
            when {
                song.all { it.fc == "app" } -> "app"
                song.all { it.fc in listOf("ap", "app") } -> "ap"
                song.all { it.fc in listOf("ap", "app", "fcp") } -> "fcp"
                song.all { it.fc.isNotEmpty() } -> "fc"
                else -> null
            }
        }
    }
    suspend fun dsProgress(level: String, data: List<BriefScore>): ByteArray {
        val records = data.filter {
            it.level == level
        }.filter {
            it.achievements > 93.9999
        }

        val img = getImage("ds/${level}_raw.png").clone()
        val songs = musics.getSongWithDSByLevel(level)
        val config = theme.dsList
        val (cols, _) = getSize(songs, config)
        var nowY = config.pos.getValue("list").y
        return img.context2d {
            runCatching {
                val titleConfig = config.pos.getValue("title")
                drawImage(
                    getImage("ds_title_complete.png"),
                    titleConfig.x,
                    titleConfig.y
                )
                getAllClearType(true, records) ?: getAllClearType(false, records) ?.let { clearType ->
                    val allClear = config.pos.getValue("allClear")
                    drawImage(
                        getImage("ds_all_clear_$clearType.png"),
                        allClear.x,
                        allClear.y
                    )
                }
                songs.forEach { (_, l) ->
                    l.forEachIndexed { index, (m, difficulty) ->
                        records.find { it.id == m.id.toInt() && it.levelIndex == difficulty } ?.let { record ->
                            val row = index / cols
                            val col = index % cols
                            val x = config.pos.getValue("list").x + col * (config.coverWidth + config.gapX)
                            val y = nowY + row * (config.coverWidth + config.gapY)
                            val rateIcon = config.pos.getValue("rateIcon")
                            fillStyle = RGBA(0, 0, 0, 0x65)
                            fillRect(x, y, config.coverWidth, config.coverWidth)
                            drawImage(
                                getImage("ds_icon_${acc2rate(record.achievements)}.png"),
                                x + rateIcon.x, y + rateIcon.y)
                        }
                    }
                    nowY += (l.size * 1.0 / cols).toIntCeil() * (config.coverWidth + config.gapY) +
                            config.pos.getValue("lineGap").y
                }
            }.onFailure {
                it.printStackTrace()
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
        val records = data.filter { it.achievements > 93.9999 }
        val config = theme.dsList
        val (cols, size) = getSize(songs, config)
        val image =
            if (size != 1280) getImage(theme.dsList.bg).clone().toBMP32().scaled(size, size, true)
            else getImage(theme.dsList.bg).clone()
        var nowY = config.pos.getValue("list").y
        return image.context2d {
            runCatching {
                val verConfig = config.pos.getValue("ver")
                drawImage(
                    getImage("ds_ver_${plateVerToVerId(version)}.png"),
                    verConfig.x,
                    verConfig.y
                )
                val titleConfig = config.pos.getValue("title")
                drawImage(
                    getImage("ds_title_complete.png"),
                    titleConfig.x,
                    titleConfig.y
                )
                val botInfoConfig = config.pos.getValue("botInfo")
                drawImage(
                    getImage("ds_bot_info.png"),
                    width - botInfoConfig.x,
                    botInfoConfig.y
                )
                getAllClearType(type == "将", records) ?.let { clearType ->
                    val allClear = config.pos.getValue("allClear")
                    drawImage(
                        getImage("ds_all_clear_$clearType.png"),
                        allClear.x,
                        allClear.y
                    )
                }
                songs.forEach { (level, l) ->
                    val dsBg = config.pos.getValue("dsBg")
                    drawImage(
                        getImage("ds_level.png"),
                        config.pos.getValue("list").x + dsBg.x,
                        nowY + dsBg.y
                    )
                    drawTextRelative(
                        level,
                        config.pos.getValue("list").x,
                        nowY,
                        config.pos.getValue("dsText"),
                        TextAlignment.CENTER
                    )
                    l.forEachIndexed { index, m ->
                        val col = index % cols
                        val row = index / cols
                        val x = config.pos.getValue("list").x + col * (config.coverWidth + config.gapX)
                        val y = nowY + row * (config.coverWidth + config.gapY)
                        val cover = getCoverBitmap(m.id).toBMP32()
                            .scaled(config.coverWidth, config.coverWidth, true)
                        val coverBg = config.pos.getValue("coverBg")
                        drawImage(
                            getImage("ds_bg_Master.png"),
                            x + coverBg.x,
                            y + coverBg.y
                        )
                        drawImage(cover, x, y)
                        val rateIcon = config.pos.getValue("rateIcon")
                        records.find { it.id == m.id.toInt() && it.levelIndex == 3 } ?.let { record ->
                            when (type) {
                                "将" -> {
                                    if (record.achievements > 99.9999) {
                                        fillStyle = RGBA(0, 0, 0, 0x65)
                                        fillRect(x, y, config.coverWidth, config.coverWidth)
                                    }
                                    drawImage(
                                        getImage("ds_icon_${acc2rate(record.achievements)}.png"),
                                        x + rateIcon.x, y + rateIcon.y
                                    )
                                }
                                in listOf("极", "神") -> {
                                    if ((type == "极" && record.fc.isNotEmpty()) || (type == "神" && record.fc in listOf("ap", "app"))) {
                                        fillStyle = RGBA(0, 0, 0, 0x65)
                                        fillRect(x, y, config.coverWidth, config.coverWidth)
                                    }
                                    if (record.fc.isEmpty())
                                        return@let
                                    drawImage(
                                        getImage("ds_icon_${record.fc}.png"),
                                        x + rateIcon.x, y + rateIcon.y
                                    )
                                }
                                "舞舞" -> {
                                    if (record.fs in listOf("fsd", "fsdp")) {
                                        fillStyle = RGBA(0, 0, 0, 0x65)
                                        fillRect(x, y, config.coverWidth, config.coverWidth)
                                    }
                                    if (record.fs.isEmpty())
                                        return@let
                                    drawImage(
                                        getImage("ds_icon_${record.fs}.png"),
                                        x + rateIcon.x, y + rateIcon.y
                                    )
                                }
                            }
                        }
                    }
                    nowY += (l.size * 1.0 / cols).toIntCeil() * (config.coverWidth + config.gapY) +
                            config.pos.getValue("lineGap").y
                }
            }.onFailure {
                it.printStackTrace()
            }
        }.encode(PNG)
    }
    suspend fun getLevelRecordList(
        level: String,
        page: Int,
        basicInfo: PlayerData,
        verList: List<BriefScore>,
        openId: String
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
            PlayScore(it.achievements, info.ds[it.levelIndex], 0, it.fc, it.fs,
                info.level[it.levelIndex], it.levelIndex, levelIndex2Label(it.levelIndex),
                getNewRa(info.ds[it.levelIndex], it.achievements), acc2rate(it.achievements),
                it.id, it.title, it.type)
        }.sortedWith(compareBy({ -it.achievements }, { -it.ra }))
        val pages = (data.size / 50.0).toIntCeil()
        val realPage = if (page in 1..pages) page else 1

        val settings = CustomSettings.getSettings(openId)

        return generateList(level, basicInfo, data.subList((realPage - 1) * 50,
            min(realPage * 50, data.size)
        ), realPage, pages, settings)
    }
    private suspend fun generateList(
        level: String,
        info: PlayerData,
        l: List<PlayScore>,
        nowPage: Int,
        totalPage: Int,
        settings: Map<String, String>
    ): ByteArray {
        val config = theme.b50
        val result = getImage(config.bg).clone()
        val realRating = info.charts["sd"]!!.sumOf { it.ra } + info.charts["dx"]!!.sumOf { it.ra }
        return result.context2d {
            kotlin.runCatching {
                drawHeader(info, config, realRating, settings)
            }
            drawText(
                "${level}分数列表，第 $nowPage 页 (共 $totalPage 页)",
                config.pos.getValue("ratingInfo"),
                TextAlignment.CENTER
            )

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
            runCatching {
                val cover = getCoverBitmap(songInfo.id).toBMP32()
                    .scaled(config.coverWidth, config.coverWidth, true)
                val x = config.pos.getValue("cover").x
                val y = config.pos.getValue("cover").y
                drawImage(cover, x, y)
                val header = config.pos.getValue("header")
                drawTextRelative(songInfo.basicInfo.title, header.x, header.y, config.pos.getValue("title"))
                drawTextRelative(songInfo.basicInfo.artist, header.x, header.y, config.pos.getValue("artist"))
                drawTextRelative("ID " + songInfo.id, header.x, header.y, config.pos.getValue("id"))

                val startX = config.pos.getValue("list").x
                val startY = config.pos.getValue("list").y
                for (i in 0 .. 4) {
                    val nowY = startY + i * config.gapY
                    if (i >= songInfo.ds.size) {
                        drawTextRelative("无该难度",
                            startX, nowY, config.pos.getValue("achievements"))
                        break
                    }
                    drawTextRelative(songInfo.ds[i].toString(),
                        startX, nowY, config.pos.getValue("ds"), TextAlignment.RIGHT)
                    songRecords.firstOrNull { it.levelIndex == i } ?.let { record ->
                        drawTextRelative("${record.achievements.toString().limitDecimal(4)}%",
                            startX, nowY, config.pos.getValue("achievements"))

                        val rateIcon = config.pos.getValue("rateIcon")
                        drawImage(
                            getImage("ds_icon_${acc2rate(record.achievements)}.png")
                                .toBMP32().scaleLinear(rateIcon.scale, rateIcon.scale),
                            startX + rateIcon.x, nowY + rateIcon.y
                        )
                        config.pos["fcIcon"] ?.let { fcIcon ->
                            drawImage(
                                getImage(if (record.fc.isEmpty()) "ds_icon_none.png" else "ds_icon_${record.fc}.png").toBMP32(),
                                startX + fcIcon.x, nowY + fcIcon.y
                            )
                        }
                        config.pos["fsIcon"] ?.let { fsIcon ->
                            drawImage(
                                getImage(if (record.fs.isEmpty()) "ds_icon_none.png" else "ds_icon_${record.fs}.png").toBMP32(),
                                startX + fsIcon.x, nowY + fsIcon.y
                            )
                        }
                    } ?: run {
                        drawTextRelative("您未游玩过该谱面",
                            startX, nowY, config.pos.getValue("achievements"))
                    }
                }
            }.onFailure {
                it.printStackTrace()
            }
        }.encode(PNG)
    }
    suspend fun generateAP50(basicInfo: PlayerData, records: List<BriefScore>, openId: String): ByteArray {
        val info = PlayerData(basicInfo.nickname, basicInfo.rating, basicInfo.additionalRating,
            basicInfo.username,
            plate = basicInfo.plate,
            buildMap {
                set("sd",
                    PlayScore.fillEmpty(records.filter {
                        !musics.getById(it.id.toString())!!.basicInfo.isNew && it.fc.startsWith("ap")
                    }.map {
                        val info = musics.getById(it.id.toString())!!
                        PlayScore(it.achievements, info.ds[it.levelIndex], 0, it.fc, it.fs,
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
                        PlayScore(it.achievements, info.ds[it.levelIndex], 0, it.fc, it.fs,
                            info.level[it.levelIndex], it.levelIndex, levelIndex2Label(it.levelIndex),
                            getNewRa(info.ds[it.levelIndex], it.achievements), acc2rate(it.achievements),
                            it.id, it.title, it.type)
                    }.sortedWith(compareBy({ -it.achievements }, { -it.ra })).take(15), 15)
                )
            }
        )
        return generateBest(info, openId)
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