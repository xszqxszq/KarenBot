package xyz.xszq.bot.chunithm.music

enum class ComboStatus(val values: List<String>) {
    None(listOf("", "none")),
    FullCombo(listOf("fullcombo", "fc")),
    AllJustice(listOf("alljustice", "aj")),
    AllJusticeCritical(listOf("alljusticecritical", "ajc"));

    companion object {
        fun of(
            value: String?
        ) = ComboStatus.entries.firstOrNull { status ->
            value in status.values
        } ?: None
    }

    fun isAJ() = this == AllJustice || this == AllJusticeCritical
    fun isFC() = this != None
    val resourceId
        get() = values.first()
}