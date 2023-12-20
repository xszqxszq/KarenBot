@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xszq.nereides

import korlibs.io.async.launch
import korlibs.io.file.std.toVfs
import kotlinx.coroutines.Dispatchers
import xyz.xszq.bot.audio.toSilk
import xyz.xszq.bot.image.toJPEG
import xyz.xszq.nereides.message.*
import xyz.xszq.nereides.payload.message.Media
import xyz.xszq.nereides.payload.response.PostGroupMessageResponse
import xyz.xszq.nereides.payload.utils.FileType
import xyz.xszq.nereides.payload.utils.MsgType
import kotlin.properties.Delegates

class Group(
    override val bot: Bot,
    groupId: String
) : Context {
    override val id: String = groupId
    suspend fun uploadRich(media: LocalRichMedia): Media? {
        var uploadResult = NetworkUtils.upload(when (media) {
            is Image -> media.file
            is Voice -> media.file.toSilk().toVfs()
            else -> throw UnsupportedOperationException()
        })
        return bot.uploadFile(
            groupId = id,
            url = uploadResult.url,
            fileType = when (media) {
                is Image -> FileType.IMAGE
                is Voice -> FileType.VOICE
                else -> throw UnsupportedOperationException()
            },
            send = false
        ).also {
            launch(Dispatchers.IO) {
                NetworkUtils.deleteFromCos(uploadResult.remoteFilename)
            }
        } ?: run { // 重传
            when (media) {
                is LocalImage -> {
                    uploadResult = NetworkUtils.uploadBinary(media.file.readBytes().toJPEG())
                    bot.uploadFile(
                        groupId = id,
                        url = uploadResult.url,
                        fileType = FileType.IMAGE,
                        send = false
                    ).also {
                        launch(Dispatchers.IO) {
                            NetworkUtils.deleteFromCos(uploadResult.remoteFilename)
                        }
                    }
                }
                is LocalVoice -> {
                    uploadResult = NetworkUtils.upload(media.file.toSilk().toVfs())
                    bot.uploadFile(
                        groupId = id,
                        url = uploadResult.url,
                        fileType = FileType.VOICE,
                        send = false
                    ).also {
                        launch(Dispatchers.IO) {
                            NetworkUtils.deleteFromCos(uploadResult.remoteFilename)
                        }
                    }
                }
                else -> null
            }
        }
    }
    override suspend fun sendMessage(
        content: MessageChain
    ): Boolean {
        val rawList = content.filter { it !is Metadata }
        val type = when {
            rawList.all { it is PlainText } -> MsgType.TEXT
            rawList.all { it is PlainText || it is RichMedia } -> MsgType.RICH
            rawList.any { it is Ark } -> MsgType.ARK
            rawList.any { it is Markdown } -> MsgType.MARKDOWN
            else -> MsgType.TEXT
        }
        val text = when (type) {
            in listOf(MsgType.ARK, MsgType.MARKDOWN) -> ""
            MsgType.RICH -> content.text.ifEmpty { " " }
            else -> content.text
        }
        val files = content.filterIsInstance<LocalRichMedia>().mapParallel { media ->
            uploadRich(media)
        }
        val reply = content.reply
        if (files.any { it == null }) {
            println("重传失败，消息发送不成功")
            return false
        }
        val doSendMessage: suspend (String, Media?) -> PostGroupMessageResponse? = { nowText, media ->
            bot.sendGroupMessage(
                groupId = id,
                content = nowText,
                msgType = type,
                msgId = reply ?.msgId,
                msgSeq = reply ?.seq,
                media = media,
                ark = content.filterIsInstance<Ark>().firstOrNull() ?.ark,
                markdown = content.filterIsInstance<Markdown>().firstOrNull() ?.markdown,
                keyboard = content.filterIsInstance<Keyboard>().firstOrNull() ?.keyboard
            )
        }
        var response = doSendMessage(text, files.firstOrNull())
        reply ?.let { it.seq ++ }
        println(response)

        // 错误重传处理
        response ?.ret ?.let { ret ->
            when (ret) {
                10009 -> when {
                    response!!.msg.startsWith("url not allowed") -> { // 有未加白域名
                        response = doSendMessage(content.text.filterURL(), files.firstOrNull())
                        reply ?.let { it.seq ++ }
                    }
                    response!!.msg == "0xc56 ret=241" -> { // seq重复
                        response = doSendMessage(text, files.firstOrNull())
                        reply ?.let { it.seq ++ }
                    }
                    else -> {}
                }
                22009 -> { // 消息发送超频
                    response = doSendMessage(text, files.firstOrNull())
                    reply ?.let { it.seq ++ }
                }
                in listOf(304082, 304983) -> { // 富媒体资源拉取失败，请重试
                    response = doSendMessage(text, files.firstOrNull())
                    reply ?.let { it.seq ++ }
                }
                else -> {}
            }
        }
        if (files.size > 1) {
            files.subList(1, files.size).forEach { media ->
                doSendMessage(" ", media)
                reply ?.let { it.seq ++ }
            }
        }
        response ?.let {
            bot.logger.info { "[$id] <- ${content.contentToString()}" }
        }
        return response != null
    }
}