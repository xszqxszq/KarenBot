package xyz.xszq.karenbot.api

import io.ktor.client.call.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
class PythonApiResult(val status: Boolean, val data: String = "")

// TODO: Batch query with specified function names
// TODO: Change to DSL style
object PythonApi: ApiClient() {
    private suspend fun call(args: List<Pair<String, String>>, path: String): PythonApiResult? =
        client.submitForm(ApiSettings.data.list["python_api"]!!.url + "/" + path,
            formParameters = Parameters.build {
                args.forEach { append(it.first, it.second) }
            }).body()
    suspend fun sentiment(text: String): Boolean? = call(listOf(Pair("text", text)), "sentiment")
        ?.let {it.data.toBoolean()}
    suspend fun getBPM(audio: String) = call(listOf(Pair("audio", audio)), "bpm") ?.data ?.toDouble()
    suspend fun getTTS(text: String) = call(listOf(Pair("text", text)), "tts") ?.data
    suspend fun getTTSCN(text: String) = call(listOf(Pair("text", text)), "tts_cn") ?.data
    suspend fun getOCR(img: File) = call(listOf(Pair("path", img.absolutePath)), "ocr")?.data
    suspend fun isLt(img: File) = call(listOf(Pair("path", img.absolutePath)), "lt") ?.data ?.toBoolean()
    suspend fun isBlonde(img: File) = call(listOf(Pair("path", img.absolutePath)), "blonde") ?.data ?.toBoolean()
}