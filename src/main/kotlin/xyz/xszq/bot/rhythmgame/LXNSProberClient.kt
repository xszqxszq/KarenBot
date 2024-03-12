package xyz.xszq.bot.rhythmgame

import io.github.oshai.kotlinlogging.KLogger
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.utils.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObjectBuilder
import xyz.xszq.bot.rhythmgame.lxns.payload.Response

open class LXNSProberClient(open val logger: KLogger) {
    protected val server = "https://maimai.lxns.net"
    protected val json = Json {
        prettyPrint = true; isLenient = true; ignoreUnknownKeys = true
    }
    protected val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
        expectSuccess = false
    }
    protected suspend inline fun <reified T> getPlayerInfo(
        clientType: String,
        api: String,
        friendCode: String
    ): Response<T> {
        kotlin.runCatching {
            val result: HttpResponse = client.get("$server/api/v0/$clientType/player/$friendCode/$api") {
                buildHeaders {
                    append("Authorization", RhythmGame.lxnsConfig.lxnsSecret)
                }
            }
            return result.body()
        }
        return Response(false, 0, "", null)
    }
    protected suspend inline fun <reified T> getInfo(
        clientType: String,
        api: String
    ): T? {
        kotlin.runCatching {
            val result: HttpResponse = client.get("$server/api/v0/$clientType/$api")
            return result.body()
        }
        return null
    }
}