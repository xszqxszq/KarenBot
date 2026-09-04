package xyz.xszq.bot.message

import io.ktor.client.request.*
import io.ktor.client.statement.*
import korlibs.io.file.VfsFile
import xyz.xszq.bot.util.useTempFile
import xyz.xszq.bot.util.createDownloadClient

open class RemoteMedia(
    val url: String,
    val filename: String = "",
    val contentType: String = "",
    val width: Int = 0,
    val height: Int = 0,
) : MessageElement {
    override val content = "[远程媒体:$url]"

    suspend fun <T> use(block: suspend (VfsFile) -> T): T {
        val client = createDownloadClient()
        return client.use { client ->
            val realUrl = when (this) {
                is RemoteVoice -> wavUrl
                else -> url
            }
            val response = client.get(realUrl)
            useTempFile(suffix = "." + filename.split(".").last()) { file ->
                file.write(response.bodyAsBytes())
                block.invoke(file)
            }
        }
    }
}