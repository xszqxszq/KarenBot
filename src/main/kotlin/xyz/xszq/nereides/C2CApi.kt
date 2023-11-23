package xyz.xszq.nereides

import io.github.oshai.kotlinlogging.KLogger
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import xyz.xszq.nereides.payload.message.Media
import xyz.xszq.nereides.payload.message.MessageArk
import xyz.xszq.nereides.payload.message.MessageMarkdownC2C
import xyz.xszq.nereides.payload.post.PostGroupFile
import xyz.xszq.nereides.payload.post.PostGroupMessage

interface C2CApi {
    val logger: KLogger
    suspend fun call(method: HttpMethod, api: String, payload: Any?): HttpResponse
    suspend fun post(api: String, payload: Any)

    suspend fun sendGroupMessage(
        groupId: String,
        content: String,
        msgType: Int,
        msgId: String,
        markdown: MessageMarkdownC2C? = null,
        media: Media? = null,
        ark: MessageArk? = null
    ) {
        post(
            "/v2/groups/${groupId}/messages",
            PostGroupMessage(
                content = content,
                msgType = msgType,
                msgId = msgId,
                markdown = markdown,
                media = media,
                ark = ark
            )
        )
        logger.info { "[$groupId] <- $content" }
    }
    suspend fun uploadFile(
        groupId: String,
        url: String,
        fileType: Int,
        send: Boolean = false
    ): Media? =
        kotlin.runCatching {
            call(
                HttpMethod.Post,
                "/v2/groups/${groupId}/files",
                PostGroupFile(fileType, url, false)
            ).body<Media>()
        }.onFailure {
            it.printStackTrace()
            return null
        }.getOrNull()
    suspend fun sendGroupImage(
        groupId: String,
        url: String,
        msgId: String
    ): Boolean {
        uploadFile(groupId, url, FileType.IMAGE, false) ?.let { media ->
            sendGroupMessage(groupId, " ", MsgType.RICH, msgId, media = Media(fileInfo = media.fileInfo))
        }
        return false
    }
}