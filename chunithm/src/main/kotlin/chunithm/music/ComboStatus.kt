package xyz.xszq.bot.chunithm.music

/**
 * Combo 状态
 */
enum class ComboStatus(val values: List<String>) {
    None(listOf("", "none")),
    FullCombo(listOf("fullcombo", "fc")),
    AllJustice(listOf("alljustice", "aj")),
    AllJusticeCritical(listOf("alljusticecritical", "ajc"));

    companion object {
        /**
         * 根据名称匹配 Combo 状态
         *
         * @param value 名称
         * @return Combo 状态
         */
        fun of(
            value: String?
        ) = ComboStatus.entries.firstOrNull { status ->
            value in status.values
        } ?: None
    }

    /**
     * 是否 All Justice
     */
    fun isAJ() = this == AllJustice || this == AllJusticeCritical

    /**
     * 是否 Full Combo
     */
    fun isFC() = this != None
    val resourceId
        get() = values.first()
}