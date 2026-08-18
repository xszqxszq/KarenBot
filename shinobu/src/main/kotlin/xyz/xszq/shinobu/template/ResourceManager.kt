package xyz.xszq.shinobu.template

import org.jetbrains.skia.Image
import org.jetbrains.skia.paragraph.FontCollection
import java.io.File

class ResourceManager(
    val basePath: File,
    val parent: ResourceManager? = null,
    preloadLocal: Boolean = false,
    val fontCollection: FontCollection
) {
    private val imageCache = mutableMapOf<String, Image>()
    private val externalCache = mutableMapOf<String, Image>()
    private val lruCache = object : LinkedHashMap<String, Image>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Image>?): Boolean {
            if (size > MAX_LRU) {
                eldest?.value?.close()
                return true
            }
            return false
        }
    }

    init {
        if (preloadLocal && basePath.exists() && basePath.isDirectory) {
            basePath.listFiles()?.filter {
                it.isFile && it.name.matches(Regex(".*\\.(png|jpe?g)$", RegexOption.IGNORE_CASE))
            }?.forEach { file ->
                runCatching {
                    imageCache[file.name] = Image.makeFromEncoded(file.readBytes())
                }
            }
        }
    }

    fun getImage(src: String): Image? {
        val fileName = src.substringAfterLast("/")
        val cacheKey = src.trimStart('.', '/')

        imageCache[fileName]?.let { return it }
        externalCache[cacheKey]?.let { return it }
        lruCache[cacheKey]?.let { return it }

        val file = File(basePath, src)
        if (file.exists() && file.isFile) {
            runCatching {
                val img = Image.makeFromEncoded(file.readBytes())
                val w = img.width
                val h = img.height
                if (w <= THUMBNAIL_MAX_DIM && h <= THUMBNAIL_MAX_DIM)
                    externalCache[cacheKey] = img
                else if (w <= LRU_MAX_DIM && h <= LRU_MAX_DIM)
                    lruCache[cacheKey] = img
                return img
            }
        }
        return parent?.getImage(src)
    }

    companion object {
        const val THUMBNAIL_MAX_DIM = 100
        const val LRU_MAX_DIM = 200
        const val MAX_LRU = 200
    }
}