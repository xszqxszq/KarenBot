package xyz.xszq.bot.rhythmgame.image

import kotlinx.serialization.Serializable

@Serializable
data class Theme(
    val b50: TemplateProperties,
    val dsList: TemplateProperties,
    val info: TemplateProperties
)
