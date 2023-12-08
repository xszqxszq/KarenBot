package xyz.xszq

import com.sksamuel.scrimage.filter.Filter
import com.soywiz.korau.sound.readAudioStream
import com.soywiz.korau.sound.toData
import com.soywiz.korau.sound.toSound
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.encode
import com.soywiz.korim.format.showImageAndWait
import com.soywiz.korio.file.std.localCurrentDirVfs
import com.soywiz.korio.file.std.rootLocalVfs
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import nu.pattern.OpenCV
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.upsert
import xyz.xszq.bot.audio.OttoVoice
import xyz.xszq.bot.config.BotConfig
import xyz.xszq.bot.dao.TouhouAliases
import xyz.xszq.bot.dao.TouhouMusics
import xyz.xszq.bot.image.*
import xyz.xszq.bot.maimai.Maimai
import xyz.xszq.bot.maimai.MaimaiUtils
import xyz.xszq.bot.maimai.MaimaiUtils.getPlateVerList
import xyz.xszq.nereides.*
import java.io.File

suspend fun testMaimaiB50(type: String = "qq", id: String = "943551369") {
    config = BotConfig.load(localCurrentDirVfs["config.yml"])

    database = Database.connect(config.databaseUrl, driver = "org.mariadb.jdbc.Driver",
        config.databaseUser, config.databasePassword)

    Maimai.testLoad()
    val info = Maimai.prober.getPlayerData(type, id).second!!
    while (true) {
        Maimai.images.loadThemeConfig("brief")
        val file = newTempFile()
        file.writeBytes(Maimai.images.generateBest(info, "EDC8852148286B84FAB4ECF00D21C378"))
        file.readAsImage().showImageAndWait()
    }
}
suspend fun testMaimaiScoreList(type: String = "qq", id: String = "943551369") {
    config = BotConfig.load(localCurrentDirVfs["config.yml"])
    database = Database.connect(config.databaseUrl, driver = "org.mariadb.jdbc.Driver",
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
    database = Database.connect(config.databaseUrl, driver = "org.mariadb.jdbc.Driver",
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
    database = Database.connect(config.databaseUrl, driver = "org.mariadb.jdbc.Driver",
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
    database = Database.connect(config.databaseUrl, driver = "org.mariadb.jdbc.Driver",
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
    database = Database.connect(config.databaseUrl, driver = "org.mariadb.jdbc.Driver",
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
    database = Database.connect(config.databaseUrl, driver = "org.mariadb.jdbc.Driver",
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
    OpenCV.loadLocally()
    config = BotConfig.load(localCurrentDirVfs["config.yml"])
//    rootLocalVfs["D:/Temp/test.gif"].writeBytes(MemeGenerator.handle("我想上的",
////        args = listOf("发种子"),
//        images = listOf(BuildImage.open(localCurrentDirVfs["E:\\Workspace\\meme-generator\\test.jpg"]))
//    ))
    MemeGenerator.handle("注意力涣散",
        args = listOf("ads"),
        images = listOf(BuildImage.open(localCurrentDirVfs["E:\\Workspace\\meme-generator\\test.jpg"]),BuildImage.open(localCurrentDirVfs["E:\\Workspace\\meme-generator\\test.jpg"]))
    ).showImageAndWait()
//    BuildImage.open(localCurrentDirVfs["E:\\Workspace\\meme-generator\\test.jpg"]).rotate(-25.0).saveJpg().showImageAndWait()
}
fun showFonts() {
    globalFontRegistry.listFontNames().forEach {
        println(it)
    }
}
suspend fun main() {
    println()
}