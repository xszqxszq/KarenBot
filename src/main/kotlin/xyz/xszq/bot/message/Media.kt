package xyz.xszq.bot.message

import korlibs.io.file.VfsFile

/**
 * Media message.
 * @param file The actual file on disk.
 */
open class Media(
    val file: VfsFile
): MessageElement {
    override val content = "[媒体]"
}