package xyz.xszq.bot.message

/**
 * 远程图片消息
 */
class RemoteImage(
    url: String,
    filename: String = "",
    contentType: String = "",
    width: Int = 0,
    height: Int = 0,
) : RemoteMedia(url, filename, contentType, width, height) {
    override val content = "[远程图片:$url]"
}