package tk.xszq.otomadbot.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import ru.gildor.coroutines.okhttp.await
import tk.xszq.otomadbot.core.OtomadBotCore
import tk.xszq.otomadbot.get
import java.io.File

@Serializable
class NsfwJsApiProperty(val className: String, val probability: Double)
@Serializable
class NsfwJsApiRawResult(val props: List<NsfwJsApiProperty>)
@Serializable
class NsfwJsApiResult(val probability: Map<String, Double>)

object NsfwJsApi {
    suspend fun query(image: File): NsfwJsApiResult? {
        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("image", "image", image.asRequestBody()).build()
        val request = Request.Builder()
            .url(ApiSettings.list["nsfwjs"]!!.url + "/nsfw")
            .post(requestBody)
            .build()
        val response = OkHttpClient().newCall(request).await()
        return if (response.isSuccessful) {
            val json = response.body!!.get()
            val raw: NsfwJsApiRawResult =
                OtomadBotCore.json.decodeFromString("{\"props\":$json}")
            val props = mutableMapOf<String, Double>()
            raw.props.forEach { prop ->
                props[prop.className] = prop.probability
            }
            NsfwJsApiResult(props)
        } else null
    }
}