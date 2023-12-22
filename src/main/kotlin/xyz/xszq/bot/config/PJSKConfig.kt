package xyz.xszq.bot.config

import korlibs.io.file.VfsFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import net.mamoe.yamlkt.Yaml

@Serializable
data class PJSKConfig(
    val characters: List<PJSKCharacter>
) {
    companion object {
        private val yaml = Yaml {  }
        suspend fun load(file: VfsFile): PJSKConfig {
            return PJSKConfig(yaml.decodeFromString<List<PJSKCharacter>>(file.readString()))
        }
    }
}
