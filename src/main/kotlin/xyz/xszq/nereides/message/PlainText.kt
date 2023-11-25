package xyz.xszq.nereides.message

class PlainText(val content: String): Message {
    override fun toString(): String = content
    override fun contentToString(): String = content
}