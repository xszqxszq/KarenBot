package xyz.xszq.bot.config

import korlibs.io.file.VfsFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import net.mamoe.yamlkt.Yaml

@Serializable
data class BinConfig(
    val ffmpeg: String,
    val ffmpegPath: String
) {
    companion object {
        private val yaml = Yaml {  }
        suspend fun load(file: VfsFile): BinConfig {
            return yaml.decodeFromString(file.readString())
        }
    }
}
