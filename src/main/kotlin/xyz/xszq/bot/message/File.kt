package xyz.xszq.bot.message

import korlibs.io.file.VfsFile

/**
 * 文件消息
 *
 * @param file 文件
 */
class File(
    file: VfsFile
): Media(file) {
    override val content: String = "[文件]"
}