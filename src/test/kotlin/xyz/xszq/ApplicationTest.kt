package xyz.xszq

import korlibs.image.format.*
import korlibs.io.file.std.localCurrentDirVfs
import korlibs.io.file.std.rootLocalVfs
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import nu.pattern.OpenCV
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.upsert
import xyz.xszq.bot.config.BotConfig
import xyz.xszq.bot.dao.TouhouAliases
import xyz.xszq.bot.dao.TouhouMusics
import xyz.xszq.bot.image.*
import xyz.xszq.bot.rhythmgame.maimai.Maimai
import xyz.xszq.bot.rhythmgame.maimai.MaimaiUtils
import xyz.xszq.bot.rhythmgame.maimai.MaimaiUtils.getPlateVerList
import xyz.xszq.bot.text.WikiQuery
import xyz.xszq.nereides.*
import java.io.File
import kotlin.time.measureTime

suspend fun testMaimaiB50(type: String = "qq", id: String = "943551369") {
    config = BotConfig.load(localCurrentDirVfs["config.yml"])

    mariadb = Database.connect(config.databaseUrl, driver = "org.mariadb.jdbc.Driver",
        config.databaseUser, config.databasePassword)

    Maimai.init()
    val info = Maimai.prober.getPlayerData(type, id).second!!
    while (true) {
        Maimai.images.loadThemeConfig("brief")
        useTempFile {
            println("${measureTime {
                it.writeBytes(Maimai.images.generateBest(info, "EDC8852148286B84FAB4ECF00D21C378"))
            }.inWholeMilliseconds / 1000.0}秒")
            it.readNativeImage().showImageAndWait()
        }
    }
}
suspend fun testMaimaiAP50(type: String = "qq", id: String = "1035371650") {
    config = BotConfig.load(localCurrentDirVfs["config.yml"])

    mariadb = Database.connect(config.databaseUrl, driver = "org.mariadb.jdbc.Driver",
        config.databaseUser, config.databasePassword)

    Maimai.init()
    val info = Maimai.prober.getPlayerData(type, id).second!!
    val records = Maimai.prober.getDataByVersion(type, id, getPlateVerList("all"))
    while (true) {
        Maimai.images.loadThemeConfig("brief")
        useTempFile {
            println("${measureTime {
                it.writeBytes(Maimai.images.generateAP50(info, records.second!!.verList, "EDC8852148286B84FAB4ECF00D21C378"))
            }.inWholeMilliseconds / 1000.0}秒")
            it.readNativeImage().showImageAndWait()
        }
    }
}
suspend fun testMaimaiScoreList(type: String = "qq", id: String = "943551369") {
    config = BotConfig.load(localCurrentDirVfs["config.yml"])
    mariadb = Database.connect(config.databaseUrl, driver = "org.mariadb.jdbc.Driver",
        config.databaseUser, config.databasePassword)

    Maimai.testLoad()
    val info = Maimai.prober.getPlayerData(type, id).second!!
    val data = Maimai.prober.getDataByVersion(type, id,
        getPlateVerList("all")).second!!
    while (true) {
        Maimai.images.loadThemeConfig("brief")
        useTempFile { file ->
            file.writeBytes(Maimai.images.getLevelRecordList("13+", 1, info, data.verList, "EDC8852148286B84FAB4ECF00D21C378"))
            file.readNativeImage().showImageAndWait()
        }
    }
}
suspend fun testMaimaiDsList() {
    config = BotConfig.load(localCurrentDirVfs["config.yml"])
    mariadb = Database.connect(config.databaseUrl, driver = "org.mariadb.jdbc.Driver",
        config.databaseUser, config.databasePassword)

    Maimai.testLoad()
    MaimaiUtils.levels.reversed().forEach {
//    while (true) {
        Maimai.images.loadThemeConfig("brief")
        useTempFile { file ->
            file.writeBytes(Maimai.images.drawDsList(it).encode(PNG))
            file.readNativeImage().showImageAndWait()
        }
    }
}
suspend fun testMaimaiDsProgress(type: String = "qq", id: String = "943551369") {
    config = BotConfig.load(localCurrentDirVfs["config.yml"])
    mariadb = Database.connect(config.databaseUrl, driver = "org.mariadb.jdbc.Driver",
        config.databaseUser, config.databasePassword)

    Maimai.testLoad()
    Maimai.images.preGenerateDsList()
    val data = Maimai.prober.getDataByVersion(type, id,
        getPlateVerList("all")).second!!
    while (true) {
        Maimai.images.loadThemeConfig("brief")
        useTempFile { file ->
            file.writeBytes(Maimai.images.dsProgress("13+", data.verList))
            file.readNativeImage().showImageAndWait()
        }
    }
}
suspend fun testMaimaiPlateProgress(type: String = "qq", id: String = "943551369") {
    config = BotConfig.load(localCurrentDirVfs["config.yml"])
    mariadb = Database.connect(config.databaseUrl, driver = "org.mariadb.jdbc.Driver",
        config.databaseUser, config.databasePassword)

    Maimai.testLoad()
    val vList = getPlateVerList("堇")
    val data = Maimai.prober.getDataByVersion(type, id, vList).second!!
    while (true) {
        Maimai.images.loadThemeConfig("brief")
        useTempFile { file ->
            file.writeBytes(Maimai.images.plateProgress("堇", "极", vList, data.verList))
            file.readNativeImage().showImageAndWait()
        }
    }
}
suspend fun testMaimaiInfo(type: String = "username", id: String = "xszqxszq", songId: String = "11451") {
    config = BotConfig.load(localCurrentDirVfs["config.yml"])
    mariadb = Database.connect(config.databaseUrl, driver = "org.mariadb.jdbc.Driver",
        config.databaseUser, config.databasePassword)

    Maimai.testLoad()
    val data = Maimai.prober.getDataByVersion(type, id, getPlateVerList("all")).second!!
    while (true) {
        Maimai.images.loadThemeConfig("brief")
        useTempFile { file ->
            file.writeBytes(Maimai.images.musicInfo(songId, data.verList)!!)
            file.readNativeImage().showImageAndWait()
        }
    }
}
suspend fun testBA() {
    config = BotConfig.load(localCurrentDirVfs["config.yml"])
    BlueArchiveLogo.draw("蔚藍", "檔案").showImageAndWait()
}
suspend fun importTouhou() {
    @Serializable
    data class Music(val name: String, val alias: List<String>)
    config = BotConfig.load(localCurrentDirVfs["config.yml"])
    mariadb = Database.connect(config.databaseUrl, driver = "org.mariadb.jdbc.Driver",
        config.databaseUser, config.databasePassword)
    newSuspendedTransaction {
        val result = Json.decodeFromString<List<Map<String, Music>>>(File("E:\\Workspace\\KarenBot7.0\\result.json").readText())
        val files = Json.decodeFromString<List<String>>(File("E:\\Workspace\\KarenBot7.0\\files.json").readText())
        result.forEach { th ->
            val version = th.keys.first().substringBefore("_")
            th.forEach { (id, music) ->
                println("$version/$id")
                TouhouMusics.upsert {
                    it[this.id] = id
                    it[this.name] = music.name
                    it[this.version] = version
                    it[this.filename] = files.find { f -> f.startsWith("$version/$id") }!!
                }
                music.alias.forEach { alias ->
                    TouhouAliases.upsert {
                        it[this.id] = id
                        it[this.alias] = alias
                    }
                }
            }
        }
    }
}
suspend fun ByteArray.showImageAndWait() {
    useTempFile { file ->
        file.writeBytes(this)
        file.readNativeImage().showImageAndWait()
    }
}
suspend fun testMeme() {
    OpenCV.loadLocally()
    config = BotConfig.load(localCurrentDirVfs["config.yml"])

//    MemeGenerator.handle("阿尼亚喜欢",
//        args = listOf("可怜Bot"),
//        images = listOf(BuildImage.open(localCurrentDirVfs["E:\\Workspace\\meme-generator\\test.jpg"]),BuildImage.open(localCurrentDirVfs["E:\\Workspace\\meme-generator\\test.jpg"]))
//    ).showImageAndWait()
    rootLocalVfs["D:/Temp/test.jpg"].writeBytes(MemeGenerator.handle("虹夏举牌",
        args = listOf("阿斯蒂芬"),
        images = listOf(BuildImage.open(localCurrentDirVfs["E:\\Workspace\\meme-generator\\test.jpg"]), BuildImage.open(localCurrentDirVfs["E:\\Workspace\\meme-generator\\test.jpg"]))
    ))
}
fun printFonts() {
    println(globalFontRegistry.listFontNames())
}
suspend fun main() {
//    runBlocking {
//        init()
//    }
    println(WikiQuery.query("抖M")!!.text)
//    bot = Bot(
//        appId = config.appId,
//        clientSecret = config.clientSecret,
//        easyToken = config.token,
//        sandbox = config.sandbox
//    )
//    bot.logger.info { "正在设置监听……" }
//    subscribe()
////    GlobalEventChannel.subscribePublicMessages {
////        always {
////            println("[$contextId] $subjectId -> $contentString")
////            GlobalScope.launch {
////                AccessLogs.saveLog(subjectId, contextId, contentString)
////            }
////        }
////    }
//
//    bot.logger.info { "回放流量中……" }
//    rootLocalVfs["D:/Temp/test.csv"].readString().split("\r\n", "\n", "\r").forEachParallel {
//        val (openId, context, date, content) = it.split("|", limit = 4)
//        GlobalEventChannel.broadcast(GroupAtMessageEvent(bot, UUID.randomUUID().toString(), UUID.randomUUID().toString(), openId, content.toPlainText().let {
//            if ("[image" in content)
//                it + RemoteImage(id = "result.png", url = "https://www.baidu.com/img/flexible/logo/pc/result.png")
//            else
//                MessageChain(it)
//        }, timestamp = date.toLong()))
////        delay(50L)
//    }
//    println("回放完成。")
//    delay(20000L)
//    OpenCV.loadLocally()
//    config = BotConfig.load(localCurrentDirVfs["config.yml"])
//
//    mariadb = Database.connect(config.databaseUrl, driver = "org.mariadb.jdbc.Driver",
//        config.databaseUser, config.databasePassword)
//    initMongo()
//    PJSKSticker.config = PJSKConfig.load(localCurrentDirVfs["image/pjsk/characters.json"])
//    rootLocalVfs["D:/Temp/pjsk.png"].writeBytes(PJSKSticker.draw(PJSKSticker.config.characters.first { it.id == "257" }, "我有四只手").savePng())
//    rootLocalVfs["D:/Temp/test.gif"].writeBytes(MemeGenerator.handle("唐可可举牌",
//        args = listOf("阿斯蒂芬"),
//        images = listOf(BuildImage.open(localCurrentDirVfs["E:\\Workspace\\meme-generator\\test.jpg"]), BuildImage.open(localCurrentDirVfs["E:\\Workspace\\meme-generator\\test.jpg"]))
//    ))
//    BuildImage.open(localCurrentDirVfs["E:\\Workspace\\meme-generator\\test.jpg"]).toMat().toBufferedImage().toAwtNativeImage().showImageAndWait()
//    BuildImage.open(localCurrentDirVfs["E:\\Workspace\\meme-generator\\test.jpg"]).toMatOld().toBufferedImage().toAwtNativeImage().showImageAndWait()
//    val mat = BuildImage.open(localCurrentDirVfs["E:\\Workspace\\meme-generator\\test.jpg"]).toMat()
//    mat.toBufferedImage().toAwtNativeImage().showImageAndWait()

//    rootLocalVfs["D:/Temp/test1.gif"].writeBytes(MemeGenerator.handle("添乱",
//        args = listOf("阿斯蒂芬", "dawef"),
//        images = listOf(BuildImage.open(localCurrentDirVfs["D:/Temp/test.gif"]), BuildImage.open(localCurrentDirVfs["E:\\Workspace\\meme-generator\\test.jpg"]))
//    ))
//    val new = measureTime {
//        repeat(5) {
//            MemeGenerator.handle("可达鸭",
//                args = listOf("阿斯蒂芬", "dawef"),
//                images = listOf(BuildImage.open(localCurrentDirVfs["E:\\Workspace\\meme-generator\\test.jpg"]), BuildImage.open(localCurrentDirVfs["E:\\Workspace\\meme-generator\\test.jpg"]))
//            )
//        }
//    }.inWholeMilliseconds / 1000.0
//    println("新版耗时：${new / 5}s")
//    bot = Bot(
//        appId = config.appId,
//        clientSecret = config.clientSecret,
//        easyToken = config.token,
//        sandbox = config.sandbox
//    )
//    GlobalEventChannel.subscribePublicMessages {
//        always {
//            launch(Dispatchers.IO) {
//                AccessLogs.saveLog(subjectId, contextId, contentString)
//            }
//        }
//    }
//    subscribe()
//    repeat(1000000) {
//        GlobalEventChannel.broadcast(GroupAtMessageEvent(bot, Random.nextLong().toHexString(),
//            Random.nextLong().toHexString(), Random.nextLong().toHexString(),
//            MessageChain("/来点黄毛".toPlainText()),
//            timestamp = System.currentTimeMillis()))
//    }
//    delay(20000L)
//    testMaimaiAP50()
}