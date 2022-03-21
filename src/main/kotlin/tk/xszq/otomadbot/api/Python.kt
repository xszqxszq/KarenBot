@file:Suppress("unused", "EXPERIMENTAL_API_USAGE")

package tk.xszq.otomadbot.api

import com.soywiz.krypto.encoding.Base64
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
class PythonApiResult(val status: Boolean, val data: String = "")

object PythonApi: ApiClient() {
    private suspend fun call(args: List<Pair<String, String>>, path: String): PythonApiResult? =
        client.submitForm(ApiSettings.list["python_api"]!!.url + "/" + path,
            formParameters = Parameters.build {
                args.forEach { append(it.first, it.second) }
            })
    suspend fun ocr(image: String): String = call(listOf(Pair("image", image)), "ocr")
        ?.data ?: ""
    suspend fun spherize(base64: String): ByteArray? = call(listOf(Pair("image", base64)), "spherize")
        ?.let {Base64.decode(it.data)}
    suspend fun sentiment(text: String): Boolean? = call(listOf(Pair("text", text)), "sentiment")
        ?.let {it.data.toBoolean()}
    suspend fun getLanguage(text: String) = call(listOf(Pair("text", text), Pair("encode", "none")), "language")
        ?.data
    suspend fun getBPM(audio: String) = call(listOf(Pair("audio", audio)), "bpm") ?.data ?.toDouble()
    suspend fun getHairColor(path: String) = call(listOf(Pair("image", path)), "hair_color") ?.data
    suspend fun isLt(path: String) = call(listOf(Pair("image", path)), "is_lt") ?.data ?.toBoolean()
    suspend fun getScan(path: String) = call(listOf(Pair("file", path)), "scan") ?.data
}