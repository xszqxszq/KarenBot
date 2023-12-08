package xyz.xszq.nereides

import io.github.oshai.kotlinlogging.KLogger
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import xyz.xszq.nereides.message.Face
import xyz.xszq.nereides.message.MessageChain
import xyz.xszq.nereides.message.PlainText
import xyz.xszq.nereides.message.RemoteImage
import xyz.xszq.nereides.payload.event.GroupAtMessageCreate
import xyz.xszq.nereides.payload.message.Media
import xyz.xszq.nereides.payload.message.MessageArk
import xyz.xszq.nereides.payload.message.MessageMarkdownC2C
import xyz.xszq.nereides.payload.post.PostGroupFile
import xyz.xszq.nereides.payload.post.PostGroupMessage
import xyz.xszq.nereides.payload.response.PostGroupMessageResponse
import xyz.xszq.nereides.payload.utils.FileType
import xyz.xszq.nereides.payload.utils.MsgType

interface C2CApi {
    val logger: KLogger
    suspend fun call(method: HttpMethod, api: String, payload: Any?): HttpResponse
    suspend fun post(api: String, payload: Any): HttpResponse

    suspend fun sendGroupMessage(
        groupId: String,
        content: String,
        msgType: Int,
        msgId: String,
        markdown: MessageMarkdownC2C? = null,
        media: Media? = null,
        ark: MessageArk? = null,
        msgSeq: Int? = null
    ): PostGroupMessageResponse? {
        val response = post(
            "/v2/groups/${groupId}/messages",
            PostGroupMessage(
                content = content,
                msgType = msgType,
                msgId = msgId,
                markdown = markdown,
                media = media,
                ark = ark,
                msgSeq = msgSeq
            )
        )
        logger.info { "[$groupId] <- ${media?.fileUUID?.let { " ${media.fileUUID}" } ?: ""}$content" }
        return try {
            val msg = response.body<PostGroupMessageResponse>()
            msg
        } catch (e: Exception) {
            null
        }
    }
    suspend fun uploadFile(
        groupId: String,
        url: String,
        fileType: Int,
        send: Boolean = false
    ): Media? = kotlin.runCatching {
        val response = call(
            HttpMethod.Post,
            "/v2/groups/${groupId}/files",
            PostGroupFile(fileType, url, false)
        )
//        println(response.bodyAsText())
        response.body<Media>()
    }.onFailure {
        logger.error { "上传图片失败！" }
    }.getOrNull()

    fun parseContent(data: GroupAtMessageCreate): MessageChain {
        val result = MessageChain()

        var str = data.content.trim()
        val base64 = "[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{4}"
        val regex = Regex("<faceType=(\\d),faceId=\"(\\d+)\",ext=\"($base64)\">")
        while (true) {
            regex.find(str) ?.let {
                val faceType = it.groupValues[1]
                val faceId = it.groupValues[2]
                // val ext = it.groupValues[3] // UNUSED
                if (it.range.first != 0) {
                    result += PlainText(str.substring(0 until it.range.first).trim())
                }
                result += Face(faceType.toIntOrNull() ?: 1, faceId)
                str = str.substring(it.range.last + 1 until str.length).trim()
            } ?: break
        }
        if (str.isNotEmpty())
            result += PlainText(str)

        if (data.attachments?.isNotEmpty() == true)
            data.attachments.forEach { att ->
                if (att.isImage()) {
                    result += RemoteImage(att.filename, att.url)
                }
            }

        return result
    }
}