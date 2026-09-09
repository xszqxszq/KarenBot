package xyz.xszq.bot.maimai.music

/**
 * 谱面类型
 */
enum class MusicType(val value: String, val full: String) {
    Standard("SD", "standard"),
    Deluxe("DX", "dx");

    companion object {
        /**
         * 根据名称得到谱面类型
         *
         * @param value 名称
         * @return 谱面类型
         */
        fun of(value: String): MusicType =
            MusicType.entries.first { it.value == value }
    }
}