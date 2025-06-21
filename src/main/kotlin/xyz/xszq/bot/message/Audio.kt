package xyz.xszq.bot.message

import korlibs.io.file.VfsFile
import korlibs.io.file.extensionLC
import xyz.xszq.bot.AudioHandler

/**
 * Voice message.
 * @param file The actual file on disk.
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