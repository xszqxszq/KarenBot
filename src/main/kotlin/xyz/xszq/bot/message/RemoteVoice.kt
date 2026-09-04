package xyz.xszq.bot.message

/**
 * 远程语音消息
 *
 * @param wavUrl WAV 格式的 URL
 */
class RemoteVoice(
    url: String,
    filename: String = "",
    contentType: String = "",
    val wavUrl: String = "",
) : RemoteMedia(url, filename, contentType) {
    override val content = "[远程语音:$url]"
}