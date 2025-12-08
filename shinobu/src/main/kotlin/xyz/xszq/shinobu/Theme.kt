package xyz.xszq.shinobu

import dev.matrixlab.webp4j.WebPCodec
import korlibs.image.awt.toAwtNativeImage
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
    @Transient
    lateinit var renderer: Renderer
    operator fun get(id: String) = templates.first { it.id == id }.deepCopy()
    fun main() = get("main")
    fun loadFonts() {
        fontCache.clear()
        templates.forEach { container ->
            fontCache += renderer.loadFonts(container)
        }
    }
    @Transient
    private val bitmapCache = ConcurrentHashMap<String, Bitmap>()
    @Transient
    private val bytesCache = ConcurrentHashMap<String, ByteArray>()
    suspend fun loadImages() = coroutineScope {
        bitmapCache.clear()
        bytesCache.clear()
        baseDir.list().collect { file ->
            val path = file.relativePathTo(baseDir)!!
            if (file.extensionLC in listOf("png", "jpg", "webp"))
                bitmapCache[path] = file.readAsImage()!!
        }
    }
    fun fetchCache(src: String): Bitmap? {
        return bitmapCache[src]
    }
    suspend fun render(main: Container): Bitmap {
        main.loadImage(this)
        return renderer.renderElement(main, fontCache)!!
    }
}
