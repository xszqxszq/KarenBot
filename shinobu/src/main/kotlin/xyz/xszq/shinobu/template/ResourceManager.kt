package xyz.xszq.shinobu.template

import org.jetbrains.skia.Image
import org.jetbrains.skia.paragraph.FontCollection
import java.io.File

/**
 * 图片资源管理器
 *
 * 以目录为根解析图片路径并分级缓存，未命中时回退到父管理器逐级
 * 查找，`preloadLocal` 开启时会预先载入目录下的本地位图
 */
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

    /**
     * 按图片路径解析位图
     *
     * 先尝试文件名与路径缓存，再读取本地文件，边长不超过阈值的
     * 图片分别进入常驻缓存或 LRU 缓存
     *
     * @param src 样式或模板中声明的图片路径
     * @return 解析出的位图
     */
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