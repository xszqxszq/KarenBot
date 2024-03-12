package xyz.xszq.bot.rhythmgame.lxns.payload

import kotlinx.serialization.Serializable

@Serializable
enum class FullComboType(val value: String) {
    AllJustice("alljustice"), FullCombo("fullcombo")
}