package xyz.xszq.bot.message

import korlibs.io.file.VfsFile

/**
 * 图片消息
 *
 * @param file 文件
 * @param remote 图片信息
 */
class Image(
    file: VfsFile,
    val url: String = "",
    val remote: RemoteImage? = null,
): Media(file) {
    override val content: String = "[图片]"
}