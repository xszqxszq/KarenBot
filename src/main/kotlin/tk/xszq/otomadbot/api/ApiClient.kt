@file:Suppress("unused")

package tk.xszq.otomadbot.api

import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value

@Serializable
class ApiItem (
    val url: String,
    val apikey: String = "",
    val secret: String = ""
)

object ApiSettings: AutoSavePluginConfig("api") {
    val list by value(mutableMapOf(
        Pair("python_api", ApiItem("http://127.0.0.1:10090")),
        Pair("eropic", ApiItem("https://api.lolicon.app/setu/", "apikey"))
    ))
}

open class ApiClient