package xyz.xszq.bot.message

import korlibs.io.file.VfsFile

/**
 * 视频消息
 *
 * @param file 文件
 */
class Video(
    file: VfsFile
): Media(file) {
    override val content: String = "[视频]"
}