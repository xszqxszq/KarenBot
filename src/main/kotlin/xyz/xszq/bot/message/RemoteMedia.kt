package xyz.xszq.bot.message

import io.ktor.client.request.*
import io.ktor.client.statement.*
import korlibs.io.file.VfsFile
import xyz.xszq.bot.util.createDownloadClient
import xyz.xszq.bot.util.useTempFile

/**
 * 远程媒体消息
 *
 * @param url 媒体地址
 * @param filename 文件名
 * @param contentType 媒体类型
 * @param width 宽度
 * @param height 高度
 */
open class RemoteMedia(
    val url: String,
    val filename: String = "",
    val contentType: String = "",
    val width: Int = 0,
    val height: Int = 0,
) : MessageElement {
    override val content = "[远程媒体:$url]"

    /**
     * 下载远程媒体到临时文件并使用，使用后自动删除
     *
     * @param block 代码块
     * @return 代码块返回结果
     */
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