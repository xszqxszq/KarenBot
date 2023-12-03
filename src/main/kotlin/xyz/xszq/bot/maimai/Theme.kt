package xyz.xszq.bot.maimai

import kotlinx.serialization.Serializable

@Serializable
data class Theme(
    val b50: TemplateProperties,
    val dsList: TemplateProperties,
    val info: TemplateProperties
)
