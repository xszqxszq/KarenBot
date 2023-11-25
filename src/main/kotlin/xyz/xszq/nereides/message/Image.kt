package xyz.xszq.nereides.message

class Image(val id: String): Message {
    override fun toString(): String {
        return "[image:$id]"
    }

    override fun contentToString(): String {
        return "[image:$id]"
    }
}