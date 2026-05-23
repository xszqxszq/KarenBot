package xyz.xszq.bot.message

open class RemoteMedia(
    val url: String,
    val filename: String = "",
    val contentType: String = "",
    val width: Int = 0,
    val height: Int = 0,
) : MessageElement {
    override val content = "[远程媒体]"
}
