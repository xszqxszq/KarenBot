package xyz.xszq.otomadbot.api

import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
class PythonApiResult(val status: Boolean, val data: String = "")

// Will be deprecated in next version
object PythonApi: ApiClient() {
    private suspend fun call(args: List<Pair<String, String>>, path: String): PythonApiResult? =
        client.submitForm(ApiSettings.data.list["python_api"]!!.url + "/" + path,
            formParameters = Parameters.build {
                args.forEach { append(it.first, it.second) }
            })
    suspend fun sentiment(text: String): Boolean? = call(listOf(Pair("text", text)), "sentiment")
        ?.let {it.data.toBoolean()}
    suspend fun getBPM(audio: String) = call(listOf(Pair("audio", audio)), "bpm") ?.data ?.toDouble()
    suspend fun getTTS(text: String) = call(listOf(Pair("text", text)), "tts") ?.data
}