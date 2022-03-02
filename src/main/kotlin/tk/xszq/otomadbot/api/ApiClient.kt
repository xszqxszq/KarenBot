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
    val list: Map<String, ApiItem> by value(mapOf(
        Pair("python_api", ApiItem("http://127.0.0.1:10090")),
        Pair("eropic", ApiItem("https://api.lolicon.app/setu/", "apikey")),
        Pair("maimaidxprober", ApiItem("https://www.diving-fish.com/api/maimaidxprober/query/player")),
        Pair("5000choyen", ApiItem("http://127.0.0.1:10091/api/5000choyen"))
    ))
    val proxy: ProxySettings by value()
}

@Serializable
class ProxySettings {
    val type: String = "http"
    val addr: String = "127.0.0.1"
    val port: Int = 12308
    val username: String = ""
    val password: String = ""
}

open class ApiClient