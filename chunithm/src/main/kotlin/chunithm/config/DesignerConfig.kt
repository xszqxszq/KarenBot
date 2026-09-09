package xyz.xszq.bot.chunithm.config

import kotlinx.serialization.Serializable

/**
 * 谱师别名
 *
 * @param aliases 谱师有什么别名
 * @param includes 谱师有哪些其他名义/参与的合作
 * @param collabs 谱师参与合作的谱面，格式为 `id#difficulty`
 */
@Serializable
data class DesignerConfig(
    val aliases: Map<String, List<String>>,
    val includes: Map<String, List<String>>,
    val collabs: Map<String, List<String>>
)