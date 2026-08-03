package xyz.xszq.bot.message

import korlibs.io.file.VfsFile

/**
 * Image message.
 * @param file The actual file on disk.
 * @param remote The remote image info.
 */
class Image(
    file: VfsFile,
    val url: String = "",
    val remote: RemoteImage? = null,
): Media(file) {
    override val content: String = "[图片]"
}
