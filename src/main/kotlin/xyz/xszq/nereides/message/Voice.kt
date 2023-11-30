package xyz.xszq.nereides.message

interface Voice: RichMedia {
    val id: String
    override fun contentToString(): String {
        return "[voice:$id]"
    }
}