package xyz.xszq.bot

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.*
import korlibs.io.file.VfsFile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import xyz.xszq.bot.message.Image
import xyz.xszq.bot.payload.*
import java.io.File

@Suppress("unused")
class MemeAPI(
    val server: String
) {
    val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                serializersModule = MemeOption.module
            })
        }
    }
    lateinit var memes: Map<String, MemeInfo>
    private suspend fun infos() =
        client.get("$server/meme/infos").body<List<MemeInfo>>()
    private suspend fun upload(url: String) =
        client.post("$server/image/upload") {
            contentType(ContentType.Application.Json)
            setBody(MemeUpload(
                type = "url",
                url = url
            ))
        }.body<MemeImageId>()
    private suspend fun upload(file: File) =
        client.post("$server/image/upload") {
            contentType(ContentType.Application.Json)
            setBody(MemeUpload(
                type = "path",
                path = file.absolutePath
            ))
        }.body<MemeImageId>()
    private suspend fun upload(file: VfsFile) =
        client.post("$server/image/upload") {
            contentType(ContentType.Application.Json)
            setBody(MemeUpload(
                type = "path",
                path = file.absolutePath
            ))
        }.body<MemeImageId>()
    private suspend fun upload(data: ByteArray) =
        client.post("$server/image/upload") {
            contentType(ContentType.Application.Json)
            setBody(MemeUpload(
                type = "data",
                data = data.encodeBase64()
            ))
        }.body<MemeImageId>()
    private suspend fun download(id: String) =
        client.get("$server/image/$id").body<ByteArray>()
    private suspend fun search(query: String, includeTags: Boolean = true) =
        client.get("$server/meme/search?query=$query&include_tags=$includeTags").body<List<String>>()
    private suspend fun preview(key: String) =
        client.get("$server/memes/$key/preview").body<MemeImageId>()
    private suspend fun generate(key: String, request: MemeGenerate) =
        client.post("$server/memes/$key") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

    suspend fun generate(
        key: String,
        images: List<Image>,
        texts: List<String>,
        options: Map<String, JsonPrimitive>
    ): ByteArray {
        val uploadedImages = images.map { MemeImage("", upload(it.file).id) }
        val response = generate(key, MemeGenerate(uploadedImages, texts, options))
        val id = response.body<MemeImageId>().id
        return download(id)
    }

    suspend fun init() {
        memes = infos().associateBy { it.key }
    }
}