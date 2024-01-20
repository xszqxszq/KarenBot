@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xszq.nereides

import korlibs.io.async.launch
import kotlinx.coroutines.Dispatchers
import xyz.xszq.bot.audio.toSilk
import xyz.xszq.bot.image.toJPEG
import xyz.xszq.nereides.message.*
import xyz.xszq.nereides.message.ark.Ark
import xyz.xszq.nereides.message.Keyboard
import xyz.xszq.nereides.payload.message.Media
import xyz.xszq.nereides.payload.response.PostGroupMessageResponse
import xyz.xszq.nereides.payload.utils.FileType
import xyz.xszq.nereides.payload.utils.MsgType

class Group(
    override val bot: Bot,
    groupId: String
) : Context {
    override val id: String = groupId
    suspend fun uploadRich(media: LocalRichMedia): Media? {
        var uploadResult = when (media) {
            is LocalImage -> media.file ?.let {
                NetworkUtils.upload(it)
            } ?: run {
                NetworkUtils.uploadBinary(media.bytes!!)
            }
            is LocalVoice -> media.file.useTempFile { NetworkUtils.upload(it.toSilk()) }
            else -> throw UnsupportedOperationException()
        }
        return bot.uploadFile(
            groupId = id,
            url = uploadResult.url,
            fileType = when (media) {
                is LocalImage -> FileType.IMAGE
                is LocalVoice -> FileType.VOICE
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
                    uploadResult = NetworkUtils.uploadBinary((media.file?.readBytes()?:media.bytes!!).toJPEG())
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
                    uploadResult = media.file.useTempFile { NetworkUtils.upload(it.toSilk()) }
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
        val type = when {
            content.all { it is PlainText } -> MsgType.TEXT
            content.all { it is PlainText || it is RichMedia } -> MsgType.RICH
            content.any { it is Ark } -> MsgType.ARK
            content.any { it is Markdown } -> MsgType.MARKDOWN
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
            bot.logger.error { "重传失败，消息发送不成功" }
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
        response ?.code ?.let { ret ->
            when (ret) {
                10009 -> when {
                    response!!.message ?.startsWith("url not allowed") == true -> { // 有未加白域名
                        response = doSendMessage(content.text.filterURL(), files.firstOrNull())
                        reply ?.let { it.seq ++ }
                    }
                    response!!.message == "0xc56 ret=241" -> { // seq重复
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