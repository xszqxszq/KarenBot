package xyz.xszq.otomadbot.image

import kotlinx.serialization.Serializable
import xyz.xszq.OtomadBotCore
import xyz.xszq.otomadbot.SafeYamlConfig

@Serializable
class MemeConfigData(
    val url: String
)

object MemeConfig: SafeYamlConfig<MemeConfigData>(
    OtomadBotCore,
    "meme",
    MemeConfigData(
        "http://127.0.0.1:2233"
    )
)