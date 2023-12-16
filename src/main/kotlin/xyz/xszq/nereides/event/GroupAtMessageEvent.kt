package xyz.xszq.nereides.event

import korlibs.io.file.std.toVfs
import xyz.xszq.bot.ffmpeg.toSilk
import xyz.xszq.bot.image.toJPEG
import xyz.xszq.nereides.NetworkUtils
import xyz.xszq.nereides.QQClient
import xyz.xszq.nereides.filterURL
import xyz.xszq.nereides.message.*
import xyz.xszq.nereides.payload.utils.FileType
import xyz.xszq.nereides.payload.utils.MsgType

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
    override suspend fun reply(content: String) = reply(MessageChain(content.toPlainText()))
    override suspend fun reply(content: Message) = reply(MessageChain(content))
    override suspend fun reply(content: MessageChain): Boolean {
        val files = content.filterIsInstance<LocalRichMedia>().map { media ->
            var file = client.uploadFile(
                groupId = groupId,
                url = NetworkUtils.upload(when (media) {
                    is Image -> media.file
                    is Voice -> media.file.toSilk().toVfs()
                    else -> throw UnsupportedOperationException()
                }),
                fileType = when (media) {
                    is Image -> FileType.IMAGE
                    is Voice -> FileType.VOICE
                    else -> throw UnsupportedOperationException()
                },
                send = false
            )
            if (file == null) {
                if (media is Image) {
                    file = client.uploadFile(
                        groupId = groupId,
                        url = NetworkUtils.uploadBinary(media.file.readBytes().toJPEG()),
                        fileType = FileType.IMAGE,
                        send = false
                    )
                } else if (media is LocalVoice) {
                    file = client.uploadFile(
                        groupId = groupId,
                        url = NetworkUtils.upload(media.file.toSilk().toVfs()),
                        fileType = FileType.VOICE,
                        send = false
                    )
                }
            }
            file
        }
        if (files.any { it == null }) {
            println("重传失败，消息发送不成功")
            return false
        }
        val type = when {
            content.all { it is PlainText } -> MsgType.TEXT
            content.all { it is PlainText || it is RichMedia } -> MsgType.RICH
            content.any { it is Ark } -> MsgType.ARK
            else -> MsgType.TEXT
        }
        var response = client.sendGroupMessage(
            groupId = groupId,
            content = when (type) {
                MsgType.ARK -> ""
                else -> content.text
            },
            msgType = type,
            msgId = msgId,
            msgSeq = replySeq ++,
            media = files.firstOrNull(),
            ark = content.filterIsInstance<Ark>().firstOrNull() ?.ark
        )
        println(response)
        response ?.ret ?.let { ret ->
            when (ret) {
                10009 -> { // 有未加白域名/腾讯错误检测
                    response = client.sendGroupMessage(
                        groupId = groupId,
                        content = content.text.filterURL(),
                        msgType = type,
                        msgId = msgId,
                        msgSeq = replySeq ++,
                        media = files.firstOrNull()
                    )
                }
                22009 -> { // 消息发送超频

                }
                304082 -> { // 富媒体资源拉取失败，请重试

                }
                304983 -> { // 富媒体资源拉取失败，请重试

                }
            }
        }
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