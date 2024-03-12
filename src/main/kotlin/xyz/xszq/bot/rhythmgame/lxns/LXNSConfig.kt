import korlibs.io.file.VfsFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import net.mamoe.yamlkt.Yaml

@Serializable
data class LXNSConfig(
    val lxnsSecret: String = ""
) {
    companion object {
        private val yaml = Yaml {  }
        suspend fun load(file: VfsFile): LXNSConfig {
            return yaml.decodeFromString(file.readString())
        }
    }
}
