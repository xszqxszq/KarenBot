package xyz.xszq.bot.chunithm.record

enum class ComboStatus(val value: String) {
    None(""),
    FullCombo("fullcombo"),
    AllJustice("alljustice"),
    AllJusticeCritical("alljusticecritical");

    companion object {
        fun of(value: String) = ComboStatus.entries.firstOrNull { it.value == value } ?: None
    }
    fun isAJ() = this == AllJustice || this == AllJusticeCritical
    fun isFC() = this != None
}