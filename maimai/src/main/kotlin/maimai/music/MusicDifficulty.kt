package xyz.xszq.bot.maimai.music

enum class MusicDifficulty(val value: Int, val names: Array<String>) {
    Basic(0, arrayOf("绿谱", "绿")),
    Advanced(1, arrayOf("黄谱", "黄")),
    Expert(2, arrayOf("红谱", "红")),
    Master(3, arrayOf("紫谱", "紫")),
    ReMaster(4, arrayOf("白谱", "白")),
    Utage(10, arrayOf("宴谱", "宴"));

    companion object {
        fun of(value: Int): MusicDifficulty =
            MusicDifficulty.entries.first { it.value == value }
        fun from(name: String): MusicDifficulty? =
            MusicDifficulty.entries.firstOrNull { name in it.names || name in it.name }
    }
    val brief
        get() = names.last()
}