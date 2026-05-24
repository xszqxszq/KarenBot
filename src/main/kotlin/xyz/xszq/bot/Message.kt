package xyz.xszq.bot

import korlibs.image.format.readNativeImage
import korlibs.image.format.showImageAndWait
import korlibs.io.file.extensionLC
import kotlinx.coroutines.DelicateCoroutinesApi
import xyz.xszq.bot.event.GroupReplyAbleEvent
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.event.ReplyAble
import xyz.xszq.bot.event.UserReplyAbleEvent
import xyz.xszq.bot.message.*
import xyz.xszq.bot.payload.FileResponse
import xyz.xszq.bot.payload.FileType
import xyz.xszq.bot.payload.MsgType
import xyz.xszq.bot.payload.markdown.MarkdownDsl

/**
 * Retry the block for specified times.
 * @param times Max times to retry.
 * @param block The code to execute.
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
 * Upload a media based on the Message context.
 * @param media The Media to upload.
 */
@OptIn(DelicateCoroutinesApi::class)
suspend fun ReplyAble.uploadMedia(media: Media): FileResponse? {
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
        bot.api.logger.error { "图片上传失败" }
        null
    }

    if (media.file.extensionLC == "silk")
        media.file.delete()

    bot.cos.deleteFromCos(remoteFile.filename)
    return response
}

fun ReplyAble.log(message: MessageChain) {
    bot.api.logger.info {
        when (this) {
            is GroupReplyAbleEvent -> "[${group.id}] <- ${message.content.replace("\n", "\\n").replace("\r", "\\r")}"
            is UserReplyAbleEvent -> "${user.username}(${user.id}) <- ${message.content.replace("\n", "\\n").replace("\r", "\\r")}"
        }
    }
}

/**
 * Reply a message to the sender.
 * @param message The message to reply.
 */
suspend fun ReplyAble.reply(message: MessageChain) {
    val mediaList = message.filterIsInstance<Media>().mapNotNull {
        uploadMedia(it)
    }
    // Debug
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
    when (this) {
        is GroupReplyAbleEvent -> bot.api.sendGroupMessage(
            group = group.id,
            content = content,
            msgType = msgType,
            markdown = markdown ?.markdown,
            keyboard = markdown ?.keyboard,
            eventId = this.eventId,
            msgId = id,
            msgSeq = seq,
            media = mediaList.firstOrNull()
        )
        is UserReplyAbleEvent -> bot.api.sendC2CMessage(
            user = user.id,
            content = content,
            msgType = msgType,
            markdown = markdown ?.markdown,
            keyboard = markdown ?.keyboard,
            eventId = this.eventId,
            msgId = id,
            msgSeq = seq,
            media = mediaList.firstOrNull()
        )
    }
    log(message)
    seq += 1

    if (mediaList.size <= 1)
        return
    mediaList.subList(1, mediaList.size).forEachIndexed { i, media ->
        when (this) {
            is GroupReplyAbleEvent -> bot.api.sendGroupMessage(
                group = group.id,
                content = " ",
                msgType = MsgType.MEDIA,
                eventId = this.eventId,
                msgId = id,
                msgSeq = seq,
                media = media
            )
            is UserReplyAbleEvent -> bot.api.sendC2CMessage(
                user = user.id,
                content = " ",
                msgType = MsgType.MEDIA,
                eventId = this.eventId,
                msgId = id,
                msgSeq = seq,
                media = media
            )
        }
        seq += 1
    }
}
suspend fun ReplyAble.reply(message: String) = reply(PlainText(message))
suspend fun ReplyAble.reply(message: MessageElement) = reply(MessageChain(message))
suspend fun ReplyAble.reply(block: MarkdownDsl.() -> Unit) {
    reply(MarkdownDsl().apply(block).build())
}

fun String.toPlainText() = PlainText(this)
fun MessageElement.chain() = MessageChain(this)
fun String.chain() = toPlainText().chain()
fun String.newLine() = "\n" + this

operator fun MessageElement.plus(other: MessageElement) = chain() + other
operator fun MessageElement.plus(other: MessageChain) = chain() + other
operator fun MessageElement.plus(other: String) = chain() + other.toPlainText()