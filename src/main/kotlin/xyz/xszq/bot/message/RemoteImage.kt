package xyz.xszq.bot.message

class RemoteImage(
    url: String,
    filename: String = "",
    contentType: String = "",
) : RemoteMedia(url, filename, contentType) {
    override val content = "[远程图片]"
}
