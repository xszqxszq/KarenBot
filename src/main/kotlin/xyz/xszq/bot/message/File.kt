package xyz.xszq.bot.message

import korlibs.io.file.VfsFile

class File(
    file: VfsFile
): Media(file) {
    override val content: String = "[文件]"
}