@file:Suppress("unused", "EXPERIMENTAL_API_USAGE")

package tk.xszq.otomadbot.api

import com.soywiz.krypto.encoding.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.gildor.coroutines.okhttp.await
import tk.xszq.otomadbot.core.OtomadBotCore
import tk.xszq.otomadbot.get

@Serializable
class PythonApiResult(val status: Boolean, val data: String)

object PythonApi: ApiClient() {
    private suspend fun call(args: List<Pair<String, String>>, path: String): PythonApiResult? {
        val formBuilder = FormBody.Builder()
        args.forEach { (key, value) -> formBuilder.add(key, value) }
        val form = formBuilder.build()
        val request = Request.Builder()
            .url(ApiSettings.list["python_api"]!!.url + "/" + path)
        if (args.isNotEmpty())
            request.post(form)
        val response = OkHttpClient().newCall(request.build()).await()
        return if (response.isSuccessful)
            OtomadBotCore.json.decodeFromString(response.body!!.get())
        else
            null
    }
    suspend fun ocr(url: String): String = call(listOf(Pair("url", url)), "ocr") ?.data ?: ""
    suspend fun spherize(base64: String): ByteArray? = call(listOf(Pair("image", base64)), "spherize")
        ?.let {Base64.decode(it.data)}
    suspend fun sentiment(text: String): Boolean? = call(listOf(Pair("text", text)), "sentiment")
        ?.let {it.data.toBoolean()}
    suspend fun getLanguage(text: String) = call(listOf(Pair("text", text), Pair("encode", "none")), "language")
        ?.data
    suspend fun getBPM(audio: String) = call(listOf(Pair("audio", audio)), "bpm") ?.data ?.toDouble()
    suspend fun getHairColor(path: String) = call(listOf(Pair("image", path)), "hair_color") ?.data
    suspend fun isLt(path: String) = call(listOf(Pair("image", path)), "is_lt") ?.data ?.toDouble()
}