package xyz.xszq.otomadbot.api

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import kotlinx.serialization.Serializable
import xyz.xszq.OtomadBotCore
import xyz.xszq.otomadbot.SafeYamlConfig


@Serializable
class ApiItem (
    val url: String,
    val apikey: String = "",
    val secret: String = ""
)

@Serializable
class ProxySettings {
    val type: String = "http"
    val addr: String = "127.0.0.1"
    val port: Int = 12308
    val username: String = ""
    val password: String = ""
}

@Serializable
class ApiSettingsValue(
    val list: Map<String, ApiItem> = buildMap {
        put("python_api", ApiItem("http://127.0.0.1:10090"))
        put("saucenao", ApiItem("https://saucenao.com/search.php", "apikey"))
    },
    val proxy: ProxySettings = ProxySettings()
)

const val availableUA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/85.0.4183.83 Safari/537.36"

open class ApiClient(config: HttpClientConfig<*>.() -> Unit = {}, timeout: Long = 600000L) {
    val client = HttpClient {
        install(JsonFeature) {
            serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(HttpTimeout) {
            connectTimeoutMillis = timeout
            requestTimeoutMillis = timeout
            socketTimeoutMillis = timeout
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
                if (ApiSettings.data.proxy.type.lowercase() == "socks")
                    ProxyBuilder.socks(ApiSettings.data.proxy.addr, ApiSettings.data.proxy.port)
                else
                    ProxyBuilder.http("http://${ApiSettings.data.proxy.addr}:${ApiSettings.data.proxy.port}")
        }
        apply(config)
    }
}

object ApiSettings: SafeYamlConfig<ApiSettingsValue>(OtomadBotCore, "api", ApiSettingsValue())