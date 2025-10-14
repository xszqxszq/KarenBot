package xyz.xszq.bot.music

enum class MusicType(val value: String, val full: String) {
    Standard("SD", "standard"),
    Deluxe("DX", "dx");

    companion object {
        fun of(value: String): MusicType =
            MusicType.entries.first { it.value == value }
    }
}