@file:Suppress("unused")

package xyz.xszq.bot.text

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import xyz.xszq.bot.payload.LibreTranslateRequest
import xyz.xszq.bot.payload.LibreTranslateResponse

object LibreTranslate {
    enum class Language(val code: String) {
        Auto("auto"),
        English("en"),
        Albanian("sq"),
        Arabic("ar"),
        Azerbaijani("az"),
        Bengali("bn"),
        Bulgarian("bg"),
        Catalan("ca"),
        Chinese("zh"),
        ChineseTraditional("zt"),
        Czech("cs"),
        Danish("da"),
        Dutch("nl"),
        Esperanto("eo"),
        Estonian("et"),
        Finnish("fi"),
        French("fr"),
        German("de"),
        Greek("el"),
        Hebrew("he"),
        Hindi("hi"),
        Hungarian("hu"),
        Indonesian("id"),
        Irish("ga"),
        Italian("it"),
        Japanese("ja"),
        Korean("ko"),
        Latvian("lv"),
        Lithuanian("lt"),
        Malay("ms"),
        Norwegian("nb"),
        Persian("fa"),
        Polish("pl"),
        Portuguese("pt"),
        Romanian("ro"),
        Russian("ru"),
        Serbian("sr"),
        Slovak("sk"),
        Slovenian("sl"),
        Spanish("es"),
        Swedish("sv"),
        Tagalog("tl"),
        Thai("th"),
        Turkish("tr"),
        Ukrainian("uk"),
        Urdu("ur"),
        Vietnamese("vi")
    }
    private val json = Json {
        isLenient = true
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
        }
        engine {
            config {
                followRedirects(true)
            }
        }
    }
    suspend fun translate(text: String, source: Language, target: Language): String {
        val response = client.post("http://127.0.0.1:5000/translate") {
            contentType(ContentType.Application.Json)
            setBody(LibreTranslateRequest(
                text,
                source.code,
                target.code,
                "text"
            ))
        }.body<LibreTranslateResponse>()
        return response.translatedText
    }
}