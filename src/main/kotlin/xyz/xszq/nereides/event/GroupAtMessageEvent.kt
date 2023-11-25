package xyz.xszq.nereides.event

import com.soywiz.korio.file.VfsFile
import xyz.xszq.nereides.payload.utils.FileType
import xyz.xszq.nereides.payload.utils.MsgType
import xyz.xszq.nereides.NetworkUtils
import xyz.xszq.nereides.QQClient
import xyz.xszq.nereides.payload.message.QQAttachment
import xyz.xszq.nereides.payload.response.PostGroupMessageResponse
import java.io.File

class GroupAtMessageEvent(
    override val client: QQClient,
    override val msgId: String,
    override val groupId: String,
    override val subjectId: String,
    override val content: String,
    override val timestamp: Long,
    val attachments: List<QQAttachment>
): GroupEvent, PublicMessageEvent(groupId) {
    override suspend fun reply(content: String): PostGroupMessageResponse? {
        return client.sendGroupMessage(groupId, content, MsgType.TEXT, msgId)
    }
    suspend fun reply(content: String, msgSeq: Int): PostGroupMessageResponse? {
        return client.sendGroupMessage(groupId, content, MsgType.TEXT, msgId, msgSeq = msgSeq)
    }

    suspend fun replyWithImage(content: String, image: File): PostGroupMessageResponse? {
        return client.sendGroupMessage(groupId, content, MsgType.RICH, msgId,
            media = client.uploadFile(groupId, NetworkUtils.upload(image), FileType.IMAGE, false)
        )
    }

    suspend fun replyWithImage(content: String, image: VfsFile): PostGroupMessageResponse? {
        return client.sendGroupMessage(groupId, content, MsgType.RICH, msgId,
            media = client.uploadFile(groupId, NetworkUtils.upload(File(image.absolutePath)), FileType.IMAGE, false)
        )
    }

    suspend fun replyWithImage(content: String, image: ByteArray): PostGroupMessageResponse? {
        return client.sendGroupMessage(groupId, content, MsgType.RICH, msgId,
            media = client.uploadFile(groupId, NetworkUtils.uploadBinary(image), FileType.IMAGE, false)
        )
    }

    suspend fun replyWithImage(content: String, image: VfsFile, msgSeq: Int, retry: Boolean = false): PostGroupMessageResponse? {
        repeat(if (retry) 3 else 1) {
            try {
                return client.sendGroupMessage(
                    groupId, content, MsgType.RICH, msgId,
                    media = client.uploadFile(
                        groupId,
                        NetworkUtils.upload(File(image.absolutePath)),
                        FileType.IMAGE,
                        false
                    ),
                    msgSeq = msgSeq
                )
            } catch (e: Exception) {

            }
        }
        return null
    }

    suspend fun replyWithImage(content: String, image: ByteArray, msgSeq: Int): PostGroupMessageResponse? {
        return client.sendGroupMessage(groupId, content, MsgType.RICH, msgId,
            media = client.uploadFile(groupId, NetworkUtils.uploadBinary(image), FileType.IMAGE, false),
            msgSeq = msgSeq
        )
    }

    override suspend fun sendImage(file: File) {
        client.sendGroupImage(groupId, NetworkUtils.upload(file), msgId)
    }
    override suspend fun sendImage(file: VfsFile) {
        client.sendGroupImage(groupId, NetworkUtils.upload(File(file.absolutePath)), msgId)
    }
    override suspend fun sendImage(binary: ByteArray) {
        client.sendGroupImage(groupId, NetworkUtils.uploadBinary(binary), msgId)
    }
}