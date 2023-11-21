package xyz.xszq.nereides.event

import com.soywiz.korio.file.VfsFile
import xyz.xszq.nereides.FileType
import xyz.xszq.nereides.MsgType
import xyz.xszq.nereides.NetworkUtils
import xyz.xszq.nereides.QQClient
import xyz.xszq.nereides.payload.message.MessageArk
import xyz.xszq.nereides.payload.message.QQAttachment
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
    override suspend fun reply(content: String) {
        client.sendGroupMessage(groupId, content, MsgType.TEXT, msgId)
    }

    override suspend fun sendImage(url: String) {
        client.sendGroupFile(groupId, url, FileType.IMAGE, msgId)
    }
    override suspend fun sendImage(file: File) {
        client.sendGroupFile(groupId, NetworkUtils.upload(file), FileType.IMAGE, msgId)
    }
    override suspend fun sendImage(file: VfsFile) {
        client.sendGroupFile(groupId, NetworkUtils.upload(File(file.absolutePath)), FileType.IMAGE, msgId)
    }
    override suspend fun sendImage(binary: ByteArray) {
        client.sendGroupFile(groupId, NetworkUtils.uploadBinary(binary), FileType.IMAGE, msgId)
    }

    override suspend fun sendArk(messageArk: MessageArk) {
        client.sendGroupMessage(groupId, "", MsgType.ARK, msgId, messageArk=messageArk)
    }
}