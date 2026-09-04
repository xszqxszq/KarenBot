package xyz.xszq.bot.message

import korlibs.io.file.VfsFile

/**
 * 媒体消息
 * @param file 文件
 */
open class Media(
    val file: VfsFile
): MessageElement {
    override val content = "[媒体]"
}