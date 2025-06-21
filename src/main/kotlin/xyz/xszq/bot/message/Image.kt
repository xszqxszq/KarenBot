package xyz.xszq.bot.message

import korlibs.io.file.VfsFile

/**
 * Image message.
 * @param file The actual file on disk.
 */
class Image(
    file: VfsFile
): Media(file) {
    override val content: String = "[图片]"
}