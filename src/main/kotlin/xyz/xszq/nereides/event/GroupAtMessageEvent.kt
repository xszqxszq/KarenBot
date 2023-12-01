package xyz.xszq.nereides.event

import xyz.xszq.nereides.NetworkUtils
import xyz.xszq.nereides.payload.utils.FileType
import xyz.xszq.nereides.payload.utils.MsgType
import xyz.xszq.nereides.QQClient
import xyz.xszq.nereides.message.*
import xyz.xszq.nereides.payload.response.PostGroupMessageResponse

class GroupAtMessageEvent(
    override val client: QQClient,
    override val msgId: String,
    override val groupId: String,
    override val subjectId: String,
    override val message: MessageChain,
    override val contentString: String = message.contentToString(),
    override val timestamp: Long
): GroupEvent, PublicMessageEvent(groupId) {
    private var replySeq = 1
    override suspend fun reply(content: String): Boolean {
        return client.sendGroupMessage(
            groupId = groupId,
            content = content,
            msgType = MsgType.TEXT,
            msgId = msgId,
            msgSeq = replySeq ++
        ) != null
    }
    override suspend fun reply(content: Message) = reply(MessageChain(content))
    override suspend fun reply(content: MessageChain): Boolean {
        val files = content.filterIsInstance<LocalRichMedia>().map {
            client.uploadFile(
                groupId = groupId,
                url = NetworkUtils.upload(it.file),
                fileType = when (it) {
                    is Image -> FileType.IMAGE
                    is Voice -> FileType.VOICE
                    else -> throw UnsupportedOperationException()
                },
                send = false
            )
        }
        val response = client.sendGroupMessage(
            groupId = groupId,
            content = content.text,
            msgType = if (files.isEmpty()) MsgType.TEXT else MsgType.RICH,
            msgId = msgId,
            msgSeq = replySeq ++,
            media = files.firstOrNull()
        )
        if (files.size > 1) {
            files.subList(1, files.size).forEach { media ->
                client.sendGroupMessage(
                    groupId = groupId,
                    content = " ",
                    msgType = MsgType.RICH,
                    msgId = msgId,
                    msgSeq = replySeq ++,
                    media = media
                )
            }
        }
        return response != null
    }
}