package xyz.xszq.otomadbot.api

import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import java.io.File
import kotlin.collections.set

@Serializable
class NsfwJsApiProperty(val className: String, val probability: Double)
@Serializable
class NsfwJsApiResult(val probability: Map<String, Double>)

object NsfwJsApi: ApiClient() {
    suspend fun query(image: File): NsfwJsApiResult {
        val props = mutableMapOf<String, Double>()
        client.submitFormWithBinaryData<List<NsfwJsApiProperty>> (
            url = ApiSettings.list["nsfwjs"]!!.url + "/nsfw",
            formData = formData {
                append("image", image.readBytes(), Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=${image.name}")
                })
            }
        ).forEach { prop ->
            props[prop.className] = prop.probability
        }
        return NsfwJsApiResult(props)
    }
}