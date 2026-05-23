package xyz.xszq.bot.message

import korlibs.io.file.VfsFile

class Video(
    file: VfsFile
): Media(file) {
    override val content: String = "[视频]"
}