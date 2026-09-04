@file:Suppress("unused")
package xyz.xszq.bot

import korlibs.image.format.readNativeImage
import korlibs.image.format.showImageAndWait
import korlibs.io.file.extensionLC
import kotlinx.coroutines.DelicateCoroutinesApi
import xyz.xszq.bot.event.*
import xyz.xszq.bot.message.*
import xyz.xszq.bot.payload.FileType
import xyz.xszq.bot.payload.MediaUpload
import xyz.xszq.bot.payload.MsgType
import xyz.xszq.bot.payload.markdown.MarkdownDsl
import xyz.xszq.bot.util.errorLogger
import xyz.xszq.bot.util.forEachParallel
import xyz.xszq.bot.util.sendC2CLogger
import xyz.xszq.bot.util.sendGroupLogger

/**
 * 重试指定次数直至成功
 *
 * @param times 最大重试次数
 * @param block 代码块
 * @return 首次非空结果
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
 * 在当前上下文上传媒体
 *
 * @param media 待上传的媒体
 * @return 上传结果
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

/**
 * 将消息写入当前会话对应的发送日志
 *
 * @param message 待记录的消息
 */
fun ReplyAble.log(message: MessageChain) {
    when (this) {
        is GroupReplyAbleEvent -> sendGroupLogger.info { "[${group.id}] <- ${message.content.replace("\n", "\\n").replace("\r", "\\r")}" }
        is UserReplyAbleEvent -> sendC2CLogger.info { "${user.username}(${user.id}) <- ${message.content.replace("\n", "\\n").replace("\r", "\\r")}" }
    }
}

/**
 * 向当前消息的发送者回复消息链
 *
 * @param message 待回复的消息链
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

/**
 * 以纯文本回复当前消息的发送者
 *
 * @param message 回复的文本
 */
suspend fun ReplyAble.reply(message: String) = reply(PlainText(message))

/**
 * 以单个消息元素回复当前消息的发送者
 *
 * @param message 回复的消息元素
 */
suspend fun ReplyAble.reply(message: MessageElement) = reply(MessageChain(message))

/**
 * 以 Markdown 消息回复当前消息的发送者
 *
 * @param block 描述 Markdown 内容与键盘的构建代码块
 */
suspend fun ReplyAble.reply(block: MarkdownDsl.() -> Unit) {
    reply(MarkdownDsl().apply(block).build())
}

/**
 * 撤回当前消息
 */
suspend fun MessageEvent.recall() = when (this) {
    is GroupMessageEvent -> bot.api.recallGroupMessage(group.id, this.id)
    else -> bot.api.recallC2CMessage(user.id, this.id)
}

/**
 * 将文本包装为纯文本消息
 *
 * @return 纯文本消息
 */
fun String.toPlainText() = PlainText(this)

/**
 * 将单个消息元素包装成消息链
 *
 * @return 包含该元素的消息链
 */
fun MessageElement.chain() = MessageChain(this)

/**
 * 将文本转换为消息链
 *
 * @return 由文本构成的消息链
 */
fun String.chain() = toPlainText().chain()

/**
 * 在文本前添加换行，用于拼接消息时另起一段
 *
 * @return 前置换行后的文本
 */
fun String.newLine() = "\n" + this

/**
 * 将消息元素与另一消息元素拼接成消息链
 *
 * @return 拼接后的消息链
 */
operator fun MessageElement.plus(other: MessageElement) = chain() + other

/**
 * 在消息元素后追加一条消息链
 *
 * @return 拼接后的消息链
 */
operator fun MessageElement.plus(other: MessageChain) = chain() + other

/**
 * 在消息元素后追加文本
 *
 * @return 拼接后的消息链
 */
operator fun MessageElement.plus(other: String) = chain() + other.toPlainText()