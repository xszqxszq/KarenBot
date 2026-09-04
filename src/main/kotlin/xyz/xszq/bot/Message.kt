@file:Suppress("unused")
package xyz.xszq.bot

import korlibs.image.format.readNativeImage
import korlibs.image.format.showImageAndWait
import korlibs.io.file.extensionLC
import kotlinx.coroutines.DelicateCoroutinesApi
import xyz.xszq.bot.event.*
import xyz.xszq.bot.message.*
import xyz.xszq.bot.payload.FileResponse
import xyz.xszq.bot.payload.FileType
import xyz.xszq.bot.payload.MsgType
import xyz.xszq.bot.payload.markdown.MarkdownDsl
import xyz.xszq.bot.util.errorLogger
import xyz.xszq.bot.util.forEachParallel
import xyz.xszq.bot.util.sendC2CLogger
import xyz.xszq.bot.util.sendGroupLogger

class MediaUpload(
    val response: FileResponse,
    val filename: String
)

/**
 * 重试指定次数直至成功
 * @param times 最大重试次数
 * @param block 代码块
 */
inline fun <T> retry(times: Int, block: () -> T): T? {
    (1..times).forEach { attempt ->
        block() ?.let {
            return it
        }
    }
    return null
}

/**
 * 在指定消息上下文上传媒体
 * @param media 待上传的媒体
 */
@OptIn(DelicateCoroutinesApi::class)
suspend fun ReplyAble.uploadMedia(media: Media): MediaUpload? {
    if (eventId.isBlank()) {
        if (media is Image) media.file.readNativeImage().showImageAndWait()
        return null
    }
    val remoteFile = bot.cos.upload(media.file)
    val fileType = when (media) {
        is Image -> FileType.IMAGE
        is Video -> FileType.VIDEO
        is Audio -> FileType.AUDIO
        is File -> FileType.FILE
        else -> throw Exception()
    }
    val response = retry(3) {
        when (this) {
            is GroupReplyAbleEvent -> {
                bot.api.uploadGroupFile(
                    group = group.id,
                    fileType = fileType,
                    url = remoteFile.url,
                    srvSendMsg = false
                )
            }
            is UserReplyAbleEvent -> {
                bot.api.uploadC2CFile(
                    user = user.id,
                    fileType = fileType,
                    url = remoteFile.url,
                    srvSendMsg = false
                )
            }
        }
    } ?: run {
        errorLogger.error { "图片上传失败" }
        bot.cos.deleteFromCOS(remoteFile.filename)
        return null
    }

    if (media.file.extensionLC == "silk")
        media.file.delete()

    return MediaUpload(response, remoteFile.filename)
}

fun ReplyAble.log(message: MessageChain) {
    when (this) {
        is GroupReplyAbleEvent -> sendGroupLogger.info { "[${group.id}] <- ${message.content.replace("\n", "\\n").replace("\r", "\\r")}" }
        is UserReplyAbleEvent -> sendC2CLogger.info { "${user.username}(${user.id}) <- ${message.content.replace("\n", "\\n").replace("\r", "\\r")}" }
    }
}

/**
 * 向发送者回复消息
 * @param message 回复的消息
 */
suspend fun ReplyAble.reply(message: MessageChain) {
    val mediaList = message.filterIsInstance<Media>().mapNotNull {
        uploadMedia(it)
    }
    if (this is MessageEvent && eventId.isBlank()) {
        log(message)
        return
    }
    val msgType = when {
        message.any { it is Markdown } -> MsgType.MARKDOWN
        mediaList.isNotEmpty() -> MsgType.MEDIA
        else -> MsgType.TEXT
    }
    val markdown = message.filterIsInstance<Markdown>().firstOrNull()
    val content = when (msgType) {
        MsgType.MEDIA -> message.textToSend().ifBlank { " " }
        MsgType.TEXT -> message.textToSend()
        else -> " "
    }
    val total = mediaList.size.coerceAtLeast(1)
    repeat(total) { index ->
        val media = mediaList.getOrNull(index)
        val first = index == 0
        when (this) {
            is GroupReplyAbleEvent -> bot.api.sendGroupMessage(
                group = group.id,
                content = if (first) content else " ",
                msgType = if (first) msgType else MsgType.MEDIA,
                markdown = if (first) markdown?.markdown else null,
                keyboard = if (first) markdown?.keyboard else null,
                eventId = this.eventId,
                msgId = id,
                msgSeq = seq,
                media = media?.response
            )
            is UserReplyAbleEvent -> bot.api.sendC2CMessage(
                user = user.id,
                content = if (first) content else " ",
                msgType = if (first) msgType else MsgType.MEDIA,
                markdown = if (first) markdown?.markdown else null,
                keyboard = if (first) markdown?.keyboard else null,
                eventId = this.eventId,
                msgId = id,
                msgSeq = seq,
                media = media?.response
            )
        }
        seq += 1
    }
    log(message)
    mediaList.forEachParallel { bot.cos.deleteFromCOS(it.filename) }
}
suspend fun ReplyAble.reply(message: String) = reply(PlainText(message))
suspend fun ReplyAble.reply(message: MessageElement) = reply(MessageChain(message))
suspend fun ReplyAble.reply(block: MarkdownDsl.() -> Unit) {
    reply(MarkdownDsl().apply(block).build())
}
suspend fun MessageEvent.recall() = when (this) {
    is GroupMessageEvent -> bot.api.recallGroupMessage(group.id, this.id)
    else -> bot.api.recallC2CMessage(user.id, this.id)
}

fun String.toPlainText() = PlainText(this)
fun MessageElement.chain() = MessageChain(this)
fun String.chain() = toPlainText().chain()
fun String.newLine() = "\n" + this

operator fun MessageElement.plus(other: MessageElement) = chain() + other
operator fun MessageElement.plus(other: MessageChain) = chain() + other
operator fun MessageElement.plus(other: String) = chain() + other.toPlainText()