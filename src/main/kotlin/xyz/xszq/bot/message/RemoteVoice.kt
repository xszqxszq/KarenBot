package xyz.xszq.bot.message

class RemoteVoice(
    url: String,
    filename: String = "",
    contentType: String = "",
    val wavUrl: String = "",
) : RemoteMedia(url, filename, contentType) {
    override val content = "[远程语音:$url]"
}
