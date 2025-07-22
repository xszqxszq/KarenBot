package xyz.xszq.bot.ffmpeg

import xyz.xszq.bot.ffmpeg.Argument

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
