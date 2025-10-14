package xyz.xszq.bot.theme

import korlibs.image.bitmap.Bitmap
import korlibs.image.font.Font
import korlibs.image.format.readNativeImage
import korlibs.io.async.launch
import korlibs.io.file.VfsFile
import korlibs.io.file.extensionLC
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import nl.adaptivity.xmlutil.serialization.XmlValue
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class Theme(
    @XmlValue(true)
    val templates: List<Container>
) {
    @Transient
    lateinit var baseDir: VfsFile
    @Transient
    val fontCache = mutableMapOf<String, Font>()
    operator fun get(id: String) = templates.first { it.id == id }.deepCopy()
    fun main() = get("main")
    fun loadFonts() {
        fontCache.clear()
        templates.forEach { container ->
            fontCache += Renderer.loadFonts(container)
        }
    }
    enum class ImageLoadStrategy {
        Decoded, Bytes
    }
    @Transient
    val strategy = ImageLoadStrategy.Decoded
    @Transient
    private val bitmapCache = ConcurrentHashMap<String, Bitmap>()
    @Transient
    private val bytesCache = ConcurrentHashMap<String, ByteArray>()
    suspend fun loadImages() = coroutineScope {
        bitmapCache.clear()
        bytesCache.clear()
        baseDir.list().collect { file ->
            if (file.extensionLC == "png" || file.extensionLC == "jpg") {
                launch(coroutineContext) {
                    val path = file.relativePathTo(baseDir)!!
                    when (strategy) {
                        ImageLoadStrategy.Decoded -> bitmapCache[path] = file.readNativeImage()
                        ImageLoadStrategy.Bytes -> bytesCache[path] = file.readBytes()
                    }
                }
            }
        }
    }
    fun fetchCache(src: String): Bitmap? {
        return when (strategy) {
            ImageLoadStrategy.Decoded -> bitmapCache[src]
            ImageLoadStrategy.Bytes -> bytesCache[src] ?.readBitmap()
        }
    }
}
