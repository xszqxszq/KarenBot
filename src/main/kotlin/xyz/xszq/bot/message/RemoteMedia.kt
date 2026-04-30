package xyz.xszq.bot.message

open class RemoteMedia(
    val url: String,
    val filename: String = "",
    val contentType: String = "",
) : MessageElement {
    override val content = "[远程媒体]"
}
