package xyz.xszq.bot.maimai.music

/**
 * Combo 状态
 */
enum class ComboStatus(val id: Int, val value: String) {
    None(0, "none"),
    FullCombo(1, "fc"),
    FullComboPlus(2, "fcp"),
    AllPerfect(3, "ap"),
    AllPerfectPlus(4, "app");

    companion object {
        /**
         * 根据 ID 匹配 Combo 状态
         *
         * @param id ID
         * @return Combo 状态
         */
        fun of(id: Int) = ComboStatus.entries.first { it.id == id }

        /**
         * 根据名称匹配 Combo 状态
         *
         * @param value 名称
         * @return Combo 状态
         */
        fun of(value: String?) = ComboStatus.entries.firstOrNull { it.value == value } ?: None
    }

    /**
     * 是否 All Perfect
     */
    fun isAP() = id >= AllPerfect.id

    /**
     * 是否 Full Combo
     */
    fun isFC() = id >= FullCombo.id
}