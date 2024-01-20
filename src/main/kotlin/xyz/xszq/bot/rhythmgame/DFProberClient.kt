package xyz.xszq.bot.rhythmgame

import io.github.oshai.kotlinlogging.KLogger
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

open class DFProberClient(open val logger: KLogger) {
    protected val server = "https://www.diving-fish.com"
    protected val json = Json {
        prettyPrint = true; isLenient = true; ignoreUnknownKeys = true
    }
    protected val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
        expectSuccess = false
    }
    protected suspend inline fun <reified T> getInfo(
        api: String,
        type: String = "qq",
        id: String,
        block: JsonObjectBuilder.() -> Unit
    ): Pair<HttpStatusCode, T?> {
        val payload = buildJsonObject {
            put(type, id)
            block()
        }
        kotlin.runCatching {
            val result: HttpResponse = client.post("$server/$api") {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            return Pair(result.status,
                if (result.status == HttpStatusCode.OK) result.body() else null)
        }.onFailure {
            return Pair(HttpStatusCode.BadGateway, null)
        }
        return Pair(HttpStatusCode.BadGateway, null)
    }
}