package xyz.xszq.bot.maimai.music

enum class MusicDifficulty(val value: Int, val names: List<String>) {
    Basic(0, listOf("绿谱", "绿")),
    Advanced(1, listOf("黄谱", "黄")),
    Expert(2, listOf("红谱", "红")),
    Master(3, listOf("紫谱", "紫")),
    ReMaster(4, listOf("白谱", "白")),
    Utage(10, listOf("宴谱", "宴"));

    companion object {
        fun of(value: Int): MusicDifficulty =
            MusicDifficulty.entries.first { it.value == value }
        fun from(name: String): MusicDifficulty? =
            MusicDifficulty.entries.firstOrNull { name in it.names || name in it.name }
    }
    val brief
        get() = names.last()
}