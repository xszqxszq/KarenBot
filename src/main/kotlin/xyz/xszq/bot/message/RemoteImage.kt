package xyz.xszq.bot.message

class RemoteImage(
    url: String,
    filename: String = "",
    contentType: String = "",
    width: Int = 0,
    height: Int = 0,
) : RemoteMedia(url, filename, contentType, width, height) {
    override val content = "[远程图片:$url]"
}
