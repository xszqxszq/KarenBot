package xyz.xszq.karenbot.image

import kotlinx.serialization.Serializable
import xyz.xszq.KarenBot
import xyz.xszq.karenbot.SafeYamlConfig

@Serializable
class MemeConfigData(
    val url: String
)

object MemeConfig: SafeYamlConfig<MemeConfigData>(
    KarenBot,
    "meme",
    MemeConfigData(
        "http://127.0.0.1:2233"
    )
)