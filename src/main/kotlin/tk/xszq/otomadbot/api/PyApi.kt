@file:Suppress("unused")
package tk.xszq.otomadbot.api

import com.google.gson.Gson
import com.soywiz.krypto.encoding.Base64
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.mamoe.mirai.message.data.Image
import tk.xszq.otomadbot.configMain
import tk.xszq.otomadbot.isSuccessful
import tk.xszq.otomadbot.media.getFile

open class PyApiResponse(val status: Boolean, val data: Any, val message: String = "")
data class Sentiment(val positive: Boolean, val value: Double)
data class SentimentResponse(val status: Boolean, val data: Sentiment, val message: String = "")

class PyApi {
    private val client = HttpClient(CIO)
    private val whiteList = arrayOf(
        "压力马斯内", "这个可以有", "鬼畜", "音mad", "可以", "*不*", "*什么*", "[", "://", "www", "好耶", "对"
    )
    private val blackList = arrayOf("司马", "nmsl", "嗷", "\uD83D\uDE05", "属实")
    suspend fun getSentiment(text: String, encode: String = "none"): Sentiment? {
        if (text.first() in arrayOf('.', '/', '!', '！', '。', '#') || text.last() in arrayOf('（', '）', '(', ')', '!',
                '！' , '.', '…'))
            return Sentiment(true, 1.0)
        var result: Sentiment ?= null
        whiteList.forEach {
            if (it in text.toLowerCase()) {
                result = Sentiment(true, 1.0)
                return@forEach
            }
        }
        blackList.forEach {
            if (it in text.toLowerCase()) {
                result = Sentiment(false, 0.9)
                return@forEach
            }
        }
        return result ?: run {
            val response = client.submitForm<HttpResponse>(
                url = configMain.api["pyapi"] + "sentiment",
                formParameters = Parameters.build {
                    append("text", text)
                    append("encode", encode)
                })
            if (response.isSuccessful())
                Gson().fromJson(response.readText(), SentimentResponse::class.java).data
            else null
        }
    }
    @SuppressWarnings("WeakerAcess")
    suspend fun getStringResult(queryPath: String, text: String, encode: String = "none"): String? {
        if (text.isBlank()) return ""
        val response = client.submitForm<HttpResponse>(
            url = configMain.api["pyapi"] + queryPath,
            formParameters = Parameters.build {
                append("text", text)
                append("encode", encode)
            })
        return if (response.isSuccessful())
            Gson().fromJson(response.readText(), PyApiResponse::class.java).data.toString()
        else null
    }
    suspend fun getSpherizedImage(image: Image): ByteArray? {
        val before = image.getFile()
        val response = client.submitForm<HttpResponse>(
            url = configMain.api["pyapi"] + "spherize",
            formParameters = Parameters.build {
                append("image", Base64.encode(before.readBytes()))
            })
        before.delete()
        return if (response.isSuccessful())
            Base64.decode(Gson().fromJson(response.readText(), PyApiResponse::class.java).data.toString())
        else null
    }
    suspend fun getLanguage(text: String, encode: String = "none") = getStringResult("language", text, encode)
    suspend fun getPinyin(text: String, encode: String = "none") = getStringResult("pinyin", text, encode)
    suspend fun getBPM(file: String): Double? {
        val response = client.submitForm<HttpResponse>(
            url = configMain.api["pyapi"] + "bpm",
            formParameters = Parameters.build {
                append("audio", file)
            })
        return if (response.isSuccessful())
            Gson().fromJson(response.readText(), PyApiResponse::class.java).data.toString().toDouble()
        else null
    }
}