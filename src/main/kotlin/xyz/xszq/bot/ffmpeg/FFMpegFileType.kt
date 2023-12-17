package xyz.xszq.bot.ffmpeg

data class FFMpegFileType(val ext: String, val requiredArgs: List<Argument> = emptyList()) {
    companion object {
        val MP3 = FFMpegFileType("mp3")
        val WAV = FFMpegFileType("wav")
        val PCM = FFMpegFileType("pcm")
        val GIF = FFMpegFileType("gif")
    }
}