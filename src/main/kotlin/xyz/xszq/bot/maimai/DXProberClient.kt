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

    companion object {
        fun getPlateVerList(version: String) = when (version) {
            "真" -> listOf("maimai", "maimai PLUS")
            "超" -> listOf("maimai GreeN")
            "檄" -> listOf("maimai GreeN PLUS")
            "橙" -> listOf("maimai ORANGE")
            "晓" -> listOf("maimai ORANGE PLUS")
            "桃" -> listOf("maimai PiNK")
            "樱" -> listOf("maimai PiNK PLUS")
            "紫" -> listOf("maimai MURASAKi")
            "堇" -> listOf("maimai MURASAKi PLUS")
            "白" -> listOf("maimai MiLK")
            "雪" -> listOf("MiLK PLUS")
            "辉" -> listOf("maimai FiNALE")
            in listOf("熊", "华") -> listOf("maimai でらっくす", "maimai でらっくす PLUS")
            in listOf("爽", "煌") -> listOf("maimai でらっくす Splash")
            in listOf("宙", "星") -> listOf("maimai でらっくす UNiVERSE")
            in listOf("舞", "") -> listOf("maimai", "maimai PLUS", "maimai GreeN", "maimai GreeN PLUS", "maimai ORANGE",
                "maimai ORANGE PLUS", "maimai PiNK", "maimai PiNK PLUS", "maimai MURASAKi", "maimai MURASAKi PLUS",
                "maimai MiLK", "MiLK PLUS", "maimai FiNALE")
            "all" -> listOf("maimai", "maimai PLUS", "maimai GreeN", "maimai GreeN PLUS", "maimai ORANGE",
                "maimai ORANGE PLUS", "maimai PiNK", "maimai PiNK PLUS", "maimai MURASAKi", "maimai MURASAKi PLUS",
                "maimai MiLK", "MiLK PLUS", "maimai FiNALE", "maimai でらっくす", "maimai でらっくす PLUS",
                "maimai でらっくす Splash", "maimai でらっくす Splash PLUS", "maimai でらっくす UNiVERSE",
                "maimai でらっくす UNiVERSE PLUS", "maimai でらっくす FESTiVAL", "maimai でらっくす FESTiVAL PLUS")
            else -> emptyList()
        }
    }
}