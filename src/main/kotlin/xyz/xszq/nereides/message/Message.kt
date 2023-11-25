package xyz.xszq.nereides.message

interface Message {
    override fun toString(): String
    fun contentToString(): String
}