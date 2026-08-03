package xyz.xszq.bot.message

import korlibs.io.file.VfsFile
import xyz.xszq.bot.Bot
import xyz.xszq.bot.User
import xyz.xszq.bot.json
import xyz.xszq.bot.payload.ArkData
import xyz.xszq.bot.payload.Attachment
import xyz.xszq.bot.payload.FaceExt
import xyz.xszq.bot.payload.Member
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Suppress("unused")
class MessageChain() {
    private val list = mutableListOf<MessageElement>()

    constructor(elements: List<MessageElement>) : this() {
        list.addAll(elements)
    }

    constructor(raw: String, images: List<Triple<String, VfsFile, RemoteImage?>> = emptyList(),
                bot: Bot? = null, mentions: List<Member>? = null,
                ark: ArkData? = null,
                attachments: List<Attachment> = emptyList()) : this() {
        list.addAll(raw.parseMessageElements(bot, mentions))
        list.addAll(images.map { Image(it.second, it.first, it.third) })
        list.addAll(attachments.mapNotNull { it.toMessageElement() })
        ark ?.let { list.add(Ark(it)) }
    }

    constructor(message: MessageElement) : this(mutableListOf(message))

    operator fun plus(message: MessageElement): MessageChain {
        return MessageChain((list + listOf(message)))
    }
    operator fun plus(chain: MessageChain): MessageChain {
        return MessageChain(list + chain.list)
    }

    operator fun plusAssign(message: MessageElement) {
        list.add(message)
    }

    fun filter(block: (MessageElement) -> Boolean) = MessageChain(list.filter(block))
    fun any(block: (MessageElement) -> Boolean) = list.any(block)
    fun none(block: (MessageElement) -> Boolean) = list.none(block)
    fun all(block: (MessageElement) -> Boolean) = list.all(block)
    fun find(block: MessageElement.() -> Boolean) = list.find(block)
    fun findLast(block: MessageElement.() -> Boolean) = list.findLast(block)
    fun first(block: MessageElement.() -> Boolean) = list.first(block)
    fun firstOrNull() = list.firstOrNull()
    fun firstOrNull(block: MessageElement.() -> Boolean) = list.firstOrNull(block)
    fun reverse() = MessageChain(list.reversed())
    inline fun <reified R: MessageElement> filterIsInstance() = filterIsInstanceTo<R>(mutableListOf())
    inline fun <reified R: MessageElement> filterIsInstanceTo(destination: MutableList<R>): MutableList<R> {
        forEach { element ->
            if (element is R) destination.add(element)
        }
        return destination
    }
    fun forEach(action: (MessageElement) -> Unit) = list.forEach(action)
    fun isEmpty() = list.isEmpty()

    fun clear() = list.clear()
    val content: String get() {
        return list.joinToString(separator = "") { it.content }
    }
    val text: String get() {
        return list.filterIsInstance<PlainText>().joinToString(separator = "") { it.content }
    }
    fun textToSend() = list.filter { it is PlainText || it is Face }.joinToString("") { it.toString() }

    @OptIn(ExperimentalEncodingApi::class)
    private fun Attachment.toMessageElement(): RemoteMedia? = when {
        "image" in contentType -> null
        "video" in contentType -> RemoteVideo(
            url, filename, contentType, width ?: 0, height ?: 0
        )
        "file" in contentType || contentType == "file" -> RemoteFile(
            url, filename, contentType
        )
        "voice" in contentType -> RemoteVoice(
            url, filename, contentType, voiceWavUrl ?: ""
        )
        else -> null
    }

    private fun String.parseMessageElements(bot: Bot? = null, mentions: List<Member>? = null): List<MessageElement> {
        val faceRegex = Regex("""<faceType=(\d+),faceId="(\d+)",ext="([^"]+)">""")
        val atRegex = Regex("<@([A-Fa-f0-9]+|all)>")
        val elements = mutableListOf<MessageElement>()
        var index = 0
        while (index < length) {
            val faceMatch = faceRegex.find(this, index)
            val atMatch = atRegex.find(this, index)
            val candidates = listOfNotNull(faceMatch, atMatch)
            if (candidates.isEmpty()) {
                elements.add(PlainText(substring(index)))
                break
            }
            val next = candidates.minBy { it.range.first }
            if (index < next.range.first)
                elements.add(PlainText(substring(index, next.range.first)))
            when (next) {
                faceMatch -> {
                    val type = next.groups[1]?.value?.toInt() ?: 0
                    if (type == 6) { index = next.range.last + 1; continue }
                    val id = next.groups[2]?.value?.toInt() ?: 0
                    val base64Ext = next.groups[3]?.value ?: ""
                    val ext = json.decodeFromString<FaceExt>(
                        Base64.decode(base64Ext).toString(Charsets.UTF_8)
                    )
                    elements.add(Face(type = type, id = id, name = ext.text))
                }
                atMatch -> if (bot != null && mentions != null) {
                    val atId = next.groups[1] ?.value ?: ""
                    val mention = mentions.find { it.id == atId }
                    val at = when {
                        atId == "all" -> At(isAll = true)
                        mention != null ->
                            At(User(bot, mention.id, mention.username, isBot = mention.bot, isSelf = mention.isSelf))
                        else ->
                            At(User(bot, atId))
                    }
                    elements.add(at)
                }
            }
            index = next.range.last + 1
        }
        return elements
    }
}