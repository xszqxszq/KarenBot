package xyz.xszq.nereides.message

open class At(val target: String): Message {
    override fun contentToString(): String {
        return "[at:${target}]"
    }

}