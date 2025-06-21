package xyz.xszq.bot.message

import korlibs.io.file.VfsFile
import kotlinx.serialization.json.Json
import xyz.xszq.bot.payload.FaceExt
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * The Chain of Message.
 */
class MessageChain() {
    private val list = mutableListOf<MessageElement>()

    /**
     * Create MessageChain with a List.
     * @param elements the list.
     */
    constructor(elements: List<MessageElement>) : this() {
        list.addAll(elements)
    }

    /**
     * Create MessageChain from server's response.
     * @param raw Raw Message String.
     * @param images downloaded images.
     */
    @OptIn(ExperimentalEncodingApi::class)
    constructor(raw: String, images: List<VfsFile>) : this() {
        /* Parse QQ Face */
        val faceRegex = Regex("""<faceType=(\d+),faceId="(\d+)",ext="([^"]+)">""")
        var index = 0

        faceRegex.findAll(raw).forEach { match ->
            val matchStart = match.range.first
            val matchEnd = match.range.last + 1
            /* Leading PlainText */
            if (index < matchStart) {
                val plainText = raw.substring(index, matchStart)
                list.add(PlainText(plainText))
            }
            val faceType = match.groups[1]?.value?.toInt() ?: 0
            val faceId = match.groups[2]?.value?.toInt() ?: 0
            val base64Ext = match.groups[3]?.value ?: ""
            val ext = Json.decodeFromString<FaceExt>(
                Base64.decode(base64Ext).toString(Charsets.UTF_8)
            )
            list.add(Face(
                type = faceType,
                id = faceId,
                name = ext.text
            ))
            index = matchEnd
        }
        /* Trailing PlainText */
        if (index < raw.length) {
            list.add(PlainText(raw.substring(index)))
        }

        /* Add Images */
        list.addAll(images.map { Image(it) })
    }

    /**
     * Get MessageChain with single element.
     * @param message the element.
     */
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
}