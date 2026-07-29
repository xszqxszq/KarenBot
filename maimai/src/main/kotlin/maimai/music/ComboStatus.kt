package xyz.xszq.bot.maimai.music

enum class ComboStatus(val id: Int, val value: String) {
    None(0, "none"),
    FullCombo(1, "fc"),
    FullComboPlus(2, "fcp"),
    AllPerfect(3, "ap"),
    AllPerfectPlus(4, "app");

    companion object {
        fun of(id: Int) = ComboStatus.entries.first { it.id == id }
        fun of(value: String?) = ComboStatus.entries.firstOrNull { it.value == value } ?: None
    }
    fun isAP() = id >= AllPerfect.id
    fun isFC() = id >= FullCombo.id
}