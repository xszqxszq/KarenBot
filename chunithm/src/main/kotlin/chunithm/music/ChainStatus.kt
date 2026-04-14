package xyz.xszq.bot.chunithm.music

enum class ChainStatus(val values: List<String>) {
    None(listOf("", "none")),
    Gold(listOf("fullchain")),
    Platinum(listOf("fullchain2"));

    companion object {
        fun of(
            value: String?
        ) = ChainStatus.entries.firstOrNull { status ->
            value in status.values
        } ?: None
    }
    fun isFullChain() = this != None
    val resourceId
        get() = values.first()
}