package xyz.xszq.bot.image.template

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

        imageCache[fileName]?.let { return it }

        val file = File(basePath, src)
        if (file.exists() && file.isFile) {
            runCatching {
                val img = Image.makeFromEncoded(file.readBytes())
                imageCache[fileName] = img
                return img
            }
        }
        return parent?.getImage(src)
    }
}