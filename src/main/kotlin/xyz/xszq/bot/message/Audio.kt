package xyz.xszq.bot.message

import korlibs.io.file.VfsFile
import korlibs.io.file.extensionLC
import xyz.xszq.bot.util.AudioHandler

/**
 * 语音消息
 * @param file 文件
 */
class Audio(
    file: VfsFile
): Media(run {
    if (file.extensionLC == "pcm")
        AudioHandler.pcmToSilk(file)
    else
        file
}) {
    override val content: String = "[语音]"
}