@file:Suppress("unused")

package xyz.xszq.otomadbot.api

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
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

open class ApiClient(config: HttpClientConfig<*>.() -> Unit = {}) {
    val client = HttpClient {
        install(JsonFeature) {
            serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 600000L
            requestTimeoutMillis = 600000L
            socketTimeoutMillis = 600000L
        }
        expectSuccess = false
        apply(config)
    }
    val clientProxy = HttpClient {
        install(JsonFeature) {
            serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        expectSuccess = false
        engine {
            proxy =
                if (ApiSettings.proxy.type.lowercase() == "socks")
                    ProxyBuilder.socks(ApiSettings.proxy.addr, ApiSettings.proxy.port)
                else
                    ProxyBuilder.http("http://" + ApiSettings.proxy.addr + ":" + ApiSettings.proxy.port)
        }
        apply(config)
    }
}