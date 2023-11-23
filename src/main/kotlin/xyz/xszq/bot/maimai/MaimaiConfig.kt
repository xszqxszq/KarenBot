package xyz.xszq.bot.maimai

import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.std.rootLocalVfs
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import net.mamoe.yamlkt.Yaml

@Serializable
data class MaimaiConfig(
    val theme: String = "portrait",
    val zetarakuSite: String = "https://dp4p6x0xfi5o9.cloudfront.net",
    val xrayAliasUrl: String = "https://download.fanyu.site/maimai/alias.json",
) {
    companion object {
        private val yaml = Yaml {  }
        suspend fun load(file: VfsFile): MaimaiConfig {
            return yaml.decodeFromString(file.readString())
        }
    }
}
