package xyz.xszq.bot.message

class RemoteFile(
    url: String,
    filename: String = "",
    contentType: String = "",
) : RemoteMedia(url, filename, contentType) {
    override val content = "[远程文件:$url]"
}