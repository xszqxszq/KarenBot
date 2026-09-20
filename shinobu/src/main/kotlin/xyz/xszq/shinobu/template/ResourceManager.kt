package xyz.xszq.shinobu.template

import org.jetbrains.skia.Image
import org.jetbrains.skia.paragraph.FontCollection
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

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
    private val imageCache: MutableMap<String, Image> = ConcurrentHashMap()
    private val externalCache: MutableMap<String, Image> = ConcurrentHashMap()
    private val lruLock = Any()
    private val lruCache = object : LinkedHashMap<String, Image>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Image>?): Boolean {
            if (size > MAX_LRU) {
                eldest ?.value ?.let { retire(it) }
                return true
            }
            return false
        }
    }
    private val largeLruLock = Any()
    private val largeLruCache =
        LinkedHashMap<String, LargeImage>(8, 0.75f, true)
    private var largeLruPixels = 0L

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
     * 先尝试各级缓存，再读取本地文件，小图进入常驻或普通
     * LRU 缓存，大图进入按像素总量淘汰的 LRU 缓存
     *
     * @param src 样式或模板中声明的图片路径
     * @return 解析出的位图
     */
    fun getImage(src: String): Image? {
        val fileName = src.substringAfterLast("/")
        val cacheKey = src.trimStart('.', '/')

        imageCache[fileName]?.let { return it }
        externalCache[cacheKey]?.let { return it }
        lruImage(cacheKey)?.let { return it }

        val file = File(basePath, src)
        if (file.exists() && file.isFile) {
            runCatching {
                val signature = "${file.lastModified()}:${file.length()}"
                largeImage(cacheKey, signature) ?.let { return it }
                val img = Image.makeFromEncoded(file.readBytes())
                val w = img.width
                val h = img.height
                if (w <= THUMBNAIL_MAX_DIM && h <= THUMBNAIL_MAX_DIM) {
                    val cached = externalCache.putIfAbsent(cacheKey, img)
                    if (cached != null) {
                        img.close()
                        return cached
                    }
                } else if (w <= LRU_MAX_DIM && h <= LRU_MAX_DIM) {
                    putLruImage(cacheKey, img)
                } else {
                    return putLargeLruImage(cacheKey, img, signature)
                }
                return img
            }
        }
        return parent?.getImage(src)
    }

    private fun lruImage(key: String): Image? = synchronized(lruLock) {
        lruCache[key]
    }

    private fun putLruImage(key: String, image: Image) {
        synchronized(lruLock) {
            lruCache[key] = image
        }
    }

    private fun largeImage(key: String, signature: String): Image? =
        synchronized(largeLruLock) {
            val cached = largeLruCache[key]
            if (cached == null)
                return@synchronized null
            if (cached.signature != signature) {
                largeLruCache.remove(key)
                largeLruPixels -= cached.pixels
                retire(cached.image)
                return@synchronized null
            }
            cached.image
        }

    private fun putLargeLruImage(
        key: String,
        image: Image,
        signature: String
    ): Image {
        val pixels = image.width.toLong() * image.height.toLong()
        synchronized(largeLruLock) {
            if (pixels > MAX_LARGE_LRU_PIXELS)
                return image

            val cached = largeLruCache[key]
            if (cached ?.signature == signature) {
                image.close()
                return cached.image
            }
            if (cached != null) {
                largeLruCache.remove(key)
                largeLruPixels -= cached.pixels
                retire(cached.image)
            }

            largeLruCache[key] = LargeImage(image, signature, pixels)
            largeLruPixels += pixels
            while (largeLruCache.size > MAX_LARGE_LRU ||
                largeLruPixels > MAX_LARGE_LRU_PIXELS
            ) {
                val eldest = largeLruCache.entries.iterator().next()
                largeLruCache.remove(eldest.key)
                largeLruPixels -= eldest.value.pixels
                retire(eldest.value.image)
            }
            return image
        }
    }

    /**
     * 清除被淘汰的位图
     *
     * @param image 被淘汰的位图
     */
    private fun retire(image: Image) {
        if (activeRenders.get() == 0)
            image.close()
    }

    companion object {
        const val THUMBNAIL_MAX_DIM = 100
        const val LRU_MAX_DIM = 200
        const val MAX_LRU = 200
        const val MAX_LARGE_LRU = 16
        const val MAX_LARGE_LRU_PIXELS = 8_000_000L

        private val activeRenders = AtomicInteger(0)

        private class LargeImage(
            val image: Image,
            val signature: String,
            val pixels: Long
        )

        /**
         * 开启一次渲染会话
         *
         * @return 用于结束会话的句柄
         */
        internal fun renderSession(): AutoCloseable {
            activeRenders.incrementAndGet()
            return AutoCloseable { activeRenders.decrementAndGet() }
        }
    }
}