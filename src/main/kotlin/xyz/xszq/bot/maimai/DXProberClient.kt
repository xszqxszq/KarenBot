@file:Suppress("unused")

package xyz.xszq.bot.maimai

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*
import xyz.xszq.bot.maimai.payload.*

class DXProberClient {
    private val server = "https://www.diving-fish.com"
    private val json = Json {
        prettyPrint = true; isLenient = true; ignoreUnknownKeys = true
    }
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
        expectSuccess = false
    }

    suspend fun getMusicList(): List<MusicInfo> {
        kotlin.runCatching {
            return client.get("$server/api/maimaidxprober/music_data").body()
        }.onFailure {
            it.printStackTrace()
        }
        return emptyList()
    }

    suspend fun getChartStat(): HashMap<String, List<ChartStat>> {
        kotlin.runCatching {
            return client.get("$server/api/maimaidxprober/chart_stats").body<ChartStatResponse>().charts
        }.onFailure {
            it.printStackTrace()
        }
        return hashMapOf()
    }

    private suspend inline fun <reified T> getInfo(
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

    suspend fun getPlayerData(
        type: String = "qq",
        id: String
    ): Pair<HttpStatusCode, PlayerData?> = getInfo("api/maimaidxprober/query/player", type, id) {
        put("b50", true)
    }

    suspend fun getDataByVersion(
        type: String = "qq",
        id: String,
        versions: List<String>
    ): Pair<HttpStatusCode, PlateResponse?> = getInfo("api/maimaidxprober/query/plate", type, id) {
        put("version", JsonArray(versions.map { JsonPrimitive(it) }))
    }
}