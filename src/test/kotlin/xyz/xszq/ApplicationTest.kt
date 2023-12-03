package xyz.xszq

import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.encode
import com.soywiz.korim.format.showImageAndWait
import com.soywiz.korio.file.std.localCurrentDirVfs
import kotlinx.coroutines.sync.Mutex
import org.jetbrains.exposed.sql.Database
import xyz.xszq.bot.config.BotConfig
import xyz.xszq.bot.image.BlueArchiveLogo
import xyz.xszq.bot.maimai.Maimai
import xyz.xszq.bot.maimai.MaimaiUtils
import xyz.xszq.bot.maimai.MaimaiUtils.getPlateVerList
import xyz.xszq.nereides.newTempFile
import xyz.xszq.nereides.readAsImage

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
    BlueArchiveLogo.draw("Blue", "Archive").showImageAndWait()
}
suspend fun main() {
    testMaimaiInfo()
}