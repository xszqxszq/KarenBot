package xyz.xszq.otomadbot.api

import com.soywiz.korio.net.URL
import com.soywiz.krypto.encoding.Base64
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.*
import io.ktor.client.request.*

object FiveThousandChoyenApi {
    val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 10000
            connectTimeoutMillis = 10000
            socketTimeoutMillis = 10000
        }
    }
    suspend fun generate(top: String, bottom: String): ByteArray =
        Base64.decode(client.get<String>(
            ApiSettings.list["5000choyen"]!!.url + "?top=" + URL.encodeComponent(top, formUrlEncoded = true)
                    + "&bottom=" + URL.encodeComponent(bottom, formUrlEncoded = true))
            .substringAfter("data:image/png;base64,")
        )
}