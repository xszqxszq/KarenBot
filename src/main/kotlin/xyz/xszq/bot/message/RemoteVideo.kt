package xyz.xszq.bot.message

class RemoteVideo(
    url: String,
    filename: String = "",
    contentType: String = "",
    width: Int = 0,
    height: Int = 0,
) : RemoteMedia(url, filename, contentType, width, height) {
    override val content = "[远程视频:$url]"
}