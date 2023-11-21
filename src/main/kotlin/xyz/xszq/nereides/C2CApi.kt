package xyz.xszq.nereides

import io.github.oshai.kotlinlogging.KLogger
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
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
        messageArk: MessageArk? = null,
        messageMarkdown: MessageMarkdownC2C? = null
    ) {
        post(
            "/v2/groups/${groupId}/messages",
            PostGroupMessage(content, msgType, msgId, messageArk = messageArk)
        )
        logger.info { "[$groupId] <- $content" }
    }
    suspend fun sendGroupFile(
        groupId: String,
        url: String,
        fileType: Int,
        msgId: String
    ): Boolean {
        repeat(3) {
            val response: JsonObject = call(
                HttpMethod.Post,
                "/v2/groups/${groupId}/files",
                PostGroupFile(fileType, url, true)
            ).body()
            if (response.containsKey("code") || response.containsKey("ret") || response.containsKey("msg")) {
                delay(500L)
                return@repeat
            }
            return true
        }
        sendGroupMessage(
            groupId,
            "图片发送三次后仍然失败：msg limit exceed，code=22009\n此错误由腾讯服务器导致，非bot故障",
            MsgType.TEXT,
            msgId
        )
        return false
    }
}