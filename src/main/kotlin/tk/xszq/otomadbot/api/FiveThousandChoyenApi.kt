@file:Suppress("UNUSED")
package tk.xszq.otomadbot.api

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.utils.io.jvm.javaio.*
import net.mamoe.mirai.utils.ExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import tk.xszq.otomadbot.configMain

class FiveThousandChoyenApi(
    private val host: String = configMain.api["fiveThousandChoyen"] ?: "") : HTTPApi(HttpClient(CIO) {
    install(HttpTimeout) {
        requestTimeoutMillis = 10000
        connectTimeoutMillis = 10000
        socketTimeoutMillis = 10000
    }
}) {
    suspend fun generate(top: String, bottom: String?): ExternalResource = client.get<HttpResponse>(
        url {
            protocol = URLProtocol.HTTP
            host = this@FiveThousandChoyenApi.host
            path("api", "v1", "gen")
            parameters.append("top", top)
            parameters.append("bottom", bottom ?: "")
        }
    ).content.toByteArray().toExternalResource()
}