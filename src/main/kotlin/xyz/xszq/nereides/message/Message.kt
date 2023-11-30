package xyz.xszq.nereides.message

interface Message {
    // 序列化时，对于任何消息，都应该调用此函数，而非 toString()
    fun contentToString(): String
}