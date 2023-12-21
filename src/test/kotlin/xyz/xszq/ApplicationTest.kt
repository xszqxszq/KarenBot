package xyz.xszq

import com.mongodb.kotlin.client.coroutine.MongoClient
import korlibs.image.format.PNG
import korlibs.image.format.encode
import korlibs.image.format.showImageAndWait
import korlibs.io.async.launch
import korlibs.io.file.std.localCurrentDirVfs
import korlibs.io.file.std.rootLocalVfs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import nu.pattern.OpenCV
import okhttp3.internal.toHexString
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.upsert
import xyz.xszq.bot.audio.OttoVoice
import xyz.xszq.bot.config.BotConfig
import xyz.xszq.bot.dao.AccessLogs
import xyz.xszq.bot.dao.TouhouAliases
import xyz.xszq.bot.dao.TouhouMusics
import xyz.xszq.bot.image.BlueArchiveLogo
import xyz.xszq.bot.image.BuildImage
import xyz.xszq.bot.image.MemeGenerator
import xyz.xszq.bot.image.globalFontRegistry
import xyz.xszq.bot.maimai.Maimai
import xyz.xszq.bot.maimai.MaimaiUtils
import xyz.xszq.bot.maimai.MaimaiUtils.getPlateVerList
import xyz.xszq.nereides.Bot
import xyz.xszq.nereides.event.GlobalEventChannel
import xyz.xszq.nereides.event.GroupAtMessageEvent
import xyz.xszq.nereides.message.MessageChain
import xyz.xszq.nereides.message.toPlainText
import xyz.xszq.nereides.newTempFile
import xyz.xszq.nereides.readAsImage
import java.io.File
import java.time.LocalDateTime
import kotlin.random.Random
import kotlin.time.measureTime

suspend fun testMaimaiB50(type: String = "qq", id: String = "943551369") {
    config = BotConfig.load(localCurrentDirVfs["config.yml"])

    mariadb = Database.connect(config.databaseUrl, driver = "org.mariadb.jdbc.Driver",
        config.databaseUser, config.databasePassword)

    Maimai.initBlocking()
    val info = Maimai.prober.getPlayerData(type, id).second!!
    while (true) {
        Maimai.images.loadThemeConfig("brief")
        val file = newTempFile()
        println("${measureTime {
            file.writeBytes(Maimai.images.generateBest(info, "EDC8852148286B84FAB4ECF00D21C378"))
        }.inWholeMilliseconds / 1000.0}秒")
        file.readAsImage().showImageAndWait()
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
        val file = newTempFile()
        file.writeBytes(Maimai.images.getLevelRecordList("13+", 1, info, data.verList, "EDC8852148286B84FAB4ECF00D21C378"))
        file.readAsImage().showImageAndWait()
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
        val file = newTempFile()
        file.writeBytes(Maimai.images.drawDsList(it).encode(PNG))
        file.readAsImage().showImageAndWait()
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
        val file = newTempFile()
        file.writeBytes(Maimai.images.dsProgress("13+", data.verList))
        file.readAsImage().showImageAndWait()
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
        val file = newTempFile()
        file.writeBytes(Maimai.images.plateProgress("堇", "极", vList, data.verList))
        file.readAsImage().showImageAndWait()
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
        val file = newTempFile()
        file.writeBytes(Maimai.images.musicInfo(songId, data.verList)!!)
        file.readAsImage().showImageAndWait()
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
    val file = newTempFile()
    file.writeBytes(this)
    file.readAsImage().showImageAndWait()
    file.deleteOnExit()
    file.delete()
}
suspend fun testMeme() {
    BuildImage.init()
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
    BuildImage.init()
    OpenCV.loadLocally()
    config = BotConfig.load(localCurrentDirVfs["config.yml"])

    mariadb = Database.connect(config.databaseUrl, driver = "org.mariadb.jdbc.Driver",
        config.databaseUser, config.databasePassword)
    initMongo()
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
    bot = Bot(
        appId = config.appId,
        clientSecret = config.clientSecret,
        easyToken = config.token,
        sandbox = config.sandbox
    )
    GlobalEventChannel.subscribePublicMessages {
        always {
            launch(Dispatchers.IO) {
                AccessLogs.saveLog(subjectId, contextId, contentString)
            }
        }
    }
    repeat(1000000) {
        GlobalEventChannel.broadcast(GroupAtMessageEvent(bot, Random.nextLong().toHexString(),
            Random.nextLong().toHexString(), Random.nextLong().toHexString(),
            MessageChain(Random.nextLong().toHexString().toPlainText()),
            timestamp = System.currentTimeMillis()))
    }
    delay(20000L)

}