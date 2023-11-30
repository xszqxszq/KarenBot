@file:Suppress("unused")

package xyz.xszq.nereides.message

class MessageChain() {
    private var list: MutableList<Message> = mutableListOf()
    constructor(l: List<Message>) : this() {
        list = l.toMutableList()
    }
    constructor(message: Message) : this(mutableListOf(message))
    operator fun plus(message: Message): MessageChain {
        return MessageChain((list + listOf(message)))
    }

    operator fun plusAssign(message: Message) {
        list.add(message)
    }

    val text: String
        get() {
            if (list.all { it is RichMedia })
                return " "
            return list.filterIsInstance<PlainText>().joinToString(separator = " ").trim()
        }

    fun filter(block: Message.() -> Boolean) = MessageChain(list.filter(block))
    fun any(block: Message.() -> Boolean) = list.any(block)
    fun none(block: Message.() -> Boolean) = list.none(block)
    fun find(block: Message.() -> Boolean) = list.find(block)
    fun findLast(block: Message.() -> Boolean) = list.findLast(block)
    fun first(block: Message.() -> Boolean) = list.first(block)
    fun firstOrNull() = list.firstOrNull()
    fun firstOrNull(block: Message.() -> Boolean) = list.firstOrNull(block)
    fun reverse() = MessageChain(list.reversed())
    inline fun <reified R: Message> filterIsInstance() = filterIsInstanceTo<R>(mutableListOf())
    inline fun <reified R: Message> filterIsInstanceTo(destination: MutableList<R>): MutableList<R> {
        forEach { element ->
            if (element is R) destination.add(element)
        }
        return destination
    }
    fun forEach(action: (Message) -> Unit) = list.forEach(action)
    fun isEmpty() = list.isEmpty()

    fun clear() = list.clear()
    fun contentToString(): String = list.joinToString(separator = "") { it.contentToString() }
}