package xyz.xszq.bot.ffmpeg

/**
 * FFMpeg 输出文件的格式类型
 *
 * @param ext 输出文件扩展名
 * @param requiredArgs 必要的附加参数
 */
@Suppress("unused")
data class FFMpegFileType(val ext: String, val requiredArgs: List<Argument> = emptyList()) {
    companion object {
        val MP3 = FFMpegFileType("mp3")
        val WAV = FFMpegFileType("wav")
        val PCM = FFMpegFileType("pcm")
        val GIF = FFMpegFileType("gif")
        val MP4 = FFMpegFileType("mp4")
        val M3U8 = FFMpegFileType("m3u8")
    }
}