package xyz.xszq.bot.component

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import xyz.xszq.bot.config.LocalConfig
import xyz.xszq.bot.payload.*

class LocalConnector {
    lateinit var config: LocalConfig

    val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }

    val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 120000
            connectTimeoutMillis = 120000
            socketTimeoutMillis = 120000
        }
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 3)
            retryIf { _, response ->
                response.status.value == 500
            }
            exponentialDelay()
        }
        install(ContentNegotiation) {
            json(json)
        }
    }

    @OptIn(ExperimentalHoplite::class)
    fun load() {
        config = ConfigLoaderBuilder.Companion.default()
            .addFileSource("./config/maimai-local.yml")
            .withExplicitSealedTypes()
            .build()
            .loadConfigOrThrow<LocalConfig>()
    }

    fun withSuffix(url: String): String {
        var suffix = config.token ?.let { "token=${config.token}" } ?: ""
        suffix = if ("?" in url)
            "&$suffix"
        else
            "?$suffix"
        return url + suffix
    }

    suspend fun qr(encoded: String) =
        client.get(withSuffix("${config.server}/qr?encoded=SGWC$encoded")).body<MaimaiQRCodeResponse>()

    suspend fun region(userId: Long) =
        client.get(withSuffix("${config.server}/region?userId=$userId"))
            .body<List<MaimaiRegionResponse>>()

    suspend fun info(userId: Long) =
        client.get(withSuffix("${config.server}/info?userId=$userId"))
            .body<MaimaiPlayerInfo>()

    suspend fun rating(userId: Long) =
        client.get(withSuffix("${config.server}/rating?userId=$userId"))
            .body<MaimaiRatingInfo>()

    suspend fun musics(userId: Long) =
        client.get(withSuffix("${config.server}/musics?userId=$userId"))
            .body<List<MaimaiRecord>>()

    suspend fun update(userId: Long, importToken: String) =
        client.get(withSuffix("${config.server}/update?userId=$userId&importToken=$importToken"))
            .body<DivingFishUpdateResponse?>()

    suspend fun ngMusics() =
        client.get(withSuffix("${config.server}/ng_musics"))
            .body<List<Int>>()
}