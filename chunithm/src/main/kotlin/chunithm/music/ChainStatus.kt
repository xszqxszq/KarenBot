package xyz.xszq.bot.chunithm.music

/**
 * Chain 状态
 */
enum class ChainStatus(val values: List<String>) {
    None(listOf("", "none")),
    Gold(listOf("fullchain")),
    Platinum(listOf("fullchain2"));

    companion object {
        /**
         * 根据名称匹配 Chain 状态
         *
         * @param value 名称
         * @return Chain 状态
         */
        fun of(
            value: String?
        ) = ChainStatus.entries.firstOrNull { status ->
            value in status.values
        } ?: None
    }

    /**
     * 是否 Full Chain
     */
    fun isFullChain() = this != None
    val resourceId
        get() = values.first()
}