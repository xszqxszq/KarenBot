@file:Suppress("unused")
package tk.xszq.otomadbot

import com.charleskorn.kaml.Yaml
import io.github.mzdluo123.silk4j.AudioUtils
import kotlinx.serialization.Serializable
import net.mamoe.mirai.utils.MiraiExperimentalApi
import net.mamoe.mirai.utils.MiraiInternalApi
import tk.xszq.otomadbot.database.doCreateH2ImageDatabase
import tk.xszq.otomadbot.database.doInitH2Images
import tk.xszq.otomadbot.media.FFMpegTask
import tk.xszq.otomadbot.media.ReplyPicList
import tk.xszq.otomadbot.media.initFonts
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/* Global Consts */
val pathPrefix = if (Files.exists(Paths.get("/bot"))) "/bot" else Paths.get("./workdir")
    .toAbsolutePath().toString() // Confirm Path Prefix
val regex = HashMap<String, Regex>() // Precompiled regex patterns
val replyPic = ReplyPicList()

/* Classes of Configs */
@Serializable
data class ConfigMain(
    val h2: ConfigH2Database,
    val cooldown: HashMap<String, String>,
    val regex: HashMap<String, String>,
    val text: HashMap<String, String>,
    val config: HashMap<String, String>,
    val database: ConfigDatabase,
    val bin: HashMap<String, String>,
    val listener: HashMap<String, String>,
    val api: HashMap<String, String>,
    val github: ConfigGithub,
    val qnap: ConfigQNAP,
    val sentiment: ConfigSentiment,
    val internal: ConfigInternal
)
@Serializable
data class ConfigAccount(val id: Long, val password: String)
@Serializable
data class ConfigBCE(val apikey: String, val secret: String, val antiporn_apikey: String, val antiporn_secret: String)
@Serializable
data class ConfigDatabase(
    val host: String, val username: String, val password: String,
    val database: String, val args: String
)
@Serializable
data class ConfigEropic(val apikey: String, val limit: HashMap<String, Long>)
@Serializable
data class ConfigGithub(val token: String, val repository: String)
@Serializable
data class ConfigH2Database(val filename: String, val username: String, val password: String)
@Serializable
data class ConfigInternal(val protocol: String, val heartbeatStrategy: String)
@Serializable
data class ConfigQNAP(val addr: String, val username: String, val password: String, val dockerId: String)
@Serializable
data class ConfigSentiment(val threshold: Double, val scale: Double, val maxInQueue: Int, val hint: String)

/* Global Variables */
var configMain: ConfigMain = Yaml.default.decodeFromString(ConfigMain.serializer(),
    File("$pathPrefix/settings.yaml").readText())
var configAccount: ConfigAccount? = null
var configBCE: ConfigBCE? = null
var configEropic: ConfigEropic? = null
var debugMode = false

/**
 * Check if external stuffs are ready.
 */
fun doCheckDependencies() {
    AudioUtils.init()
    FFMpegTask.checkFFMpeg()
    initFonts()
}

fun doCreateFolders() {
    arrayOf("bin", "fonts", "image", "image/reply", "image/afraid", "image/ma", "image/gif", "image/message", "music",
        "voice", "voice/message").forEach {
        Files.createDirectories(Paths.get("$pathPrefix/$it"))
    }
}
fun doTest() {
    "测试文本".toSimple()
    TextAnalyser.analyse("测试文本")
}

/**
 * Initialize the Bot.
 */
@MiraiExperimentalApi
@MiraiInternalApi
fun doInit() {
    doCreateFolders()
    doCheckDependencies()
    doLoadConfigs()
    doInitH2Images("reply")
    doInitH2Images("afraid", "reply")
    doInitH2Images("ma")
    doCreateH2ImageDatabase("eropic_detect_cache")
    replyPic.load("reply")
    replyPic.load("afraid")
    replyPic.load("gif", "reply")
    doCompileRegexes()
    doTest()
}

/**
 * Load configs
 */
fun doLoadConfigs() {
    configMain = Yaml.default.decodeFromString(ConfigMain.serializer(),
        File("$pathPrefix/settings.yaml").readText())
    configAccount = Yaml.default.decodeFromString(ConfigAccount.serializer(),
        File("$pathPrefix/" + configMain.config["account"]!!).readText())
    configBCE = Yaml.default.decodeFromString(ConfigBCE.serializer(),
        File("$pathPrefix/" + configMain.config["bce"]!!).readText())
    configEropic = Yaml.default.decodeFromString(ConfigEropic.serializer(),
        File("$pathPrefix/" + configMain.config["eropic"]!!).readText())
}

/**
 * Compile Regexes for later use.
 */
fun doCompileRegexes() {
    configMain.regex.forEach{ (key, value) -> regex[key.decapitalize()] = value.toRegex(RegexOption.IGNORE_CASE)}
    regex["search1"] = Regex(configMain.text["searchprefix"] + "?" + configMain.text["searchsuffix"])
    regex["search2"] = Regex(configMain.text["searchprefix"] + configMain.text["searchsuffix"] + "?")
}
