package xyz.xszq.bot.config

import com.soywiz.korio.file.VfsFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import net.mamoe.yamlkt.Yaml

@Serializable
data class BotConfig(
    val appId: String,
    val clientSecret: String,
    val token: String,
    val uploadServer: String,
    val uploadSecret: String,
    val databaseUrl: String,
    val databaseUser: String,
    val databasePassword: String,
    val domainWhitelist: List<String>,
    val gifMaxSize: Double,
    val gifMaxFrames: Int
) {
    companion object {
        private val yaml = Yaml {  }
        suspend fun load(file: VfsFile): BotConfig {
            return yaml.decodeFromString(file.readString())
        }
    }
}
