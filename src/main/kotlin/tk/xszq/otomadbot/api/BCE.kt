@file:Suppress("unused")
package tk.xszq.otomadbot.api

import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import tk.xszq.otomadbot.configBCE
import tk.xszq.otomadbot.isSuccessful
import tk.xszq.otomadbot.pass

/* Classes of BCE */
data class BCEToken(
    val refresh_token: String,
    val expires_in: Int,
    val scope: String,
    val session_key: String,
    val access_token: String,
    val session_secret: String
)
data class BCEOCRLine(val words: String)
data class BCEOCR(
    val log_id: Long,
    val words_result: List<BCEOCRLine>,
    val words_result_num: Long
)

/**
 * Get token from BaiduBCE for later queries.
 * @param apikey apikey from BCE.
 * @param secret secret from BCE
 */
suspend fun getBCEToken(apikey: String = configBCE!!.apikey, secret: String = configBCE!!.secret): String {
    val response = HttpClient(CIO).get<HttpResponse>(
        "https://aip.baidubce.com/oauth/2.0/token?" +
                "grant_type=client_credentials&client_id=$apikey&client_secret=$secret")
    return Gson().fromJson(response.readText(), BCEToken::class.java).access_token
}

/**
 * Extract text from an image throguh BCE service.
 * @param image The image which needs to be extracted
 */
suspend fun getOCRText(image: Image): String {
    val token = getBCEToken()
    val client = HttpClient(CIO)
    val imageUrl = image.queryUrl()
    val response = client.submitForm<HttpResponse>(
        url="https://aip.baidubce.com/rest/2.0/ocr/v1/accurate_basic?access_token=$token",
        formParameters = Parameters.build {
            append("imgUrl", imageUrl)
        }
    )
    return if (response.isSuccessful()) {
        val results = Gson().fromJson(response.readText(), BCEOCR::class.java)
        var final = ""
        try {
            results.words_result.forEach {
                final += it.words
            }
        } catch (e: Exception) {
            pass
        }
        final.decapitalize()
    } else {
        ""
    }
}