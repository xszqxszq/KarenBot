package xyz.xszq.bot.util

import io.github.oshai.kotlinlogging.KLogger
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import korlibs.io.file.VfsFile
import korlibs.io.file.std.tempVfs
import java.util.*

private val downloadClient by lazy { createDownloadClient() }

/**
 * 创建临时文件
 *
 * @param prefix 文件名前缀
 * @param suffix 文件名后缀
 */
fun newTempFile(
    prefix: String = "",
    suffix: String = ""
) = tempVfs[prefix + UUID.randomUUID().toString() + suffix]

/**
 * 使用文件并自动删除
 *
 * @param block 使用文件的代码块
 */
suspend fun <T> VfsFile.use(block: suspend (VfsFile) -> T): T {
    return try {
        block.invoke(this)
    } finally {
        delete()
    }
}

/**
 * 创建临时文件并在使用后删除
 *
 * @param prefix 文件名前缀
 * @param suffix 文件名后缀
 * @param block 使用代码块
 */
suspend fun <R> useTempFile(
    prefix: String = "",
    suffix: String = "",
    block: suspend (VfsFile) -> R
): R = newTempFile(prefix, suffix).use(block)

fun createDownloadClient() = HttpClient(OkHttp)



/**
 * 从 URL 下载文件
 *
 * @param url 下载地址
 * @param filename 文件名
 * @param logger 日志器
 */
suspend fun downloadFile(url: String, filename: String, logger: KLogger): VfsFile? =
    downloadFile(url, filename, logger, downloadClient)

/**
 * 从 URL 下载文件
 *
 * @param url 下载地址
 * @param filename 文件名
 * @param logger 日志器
 * @param client 客户端
 */
suspend fun downloadFile(url: String, filename: String, logger: KLogger, client: HttpClient): VfsFile? =
    withContext(Dispatchers.IO) {
        val file = tempVfs[filename]
        runCatching {
            val response = client.get(url)
            file.write(response.bodyAsBytes())
            file
        }.onFailure { e ->
            logger.error { "下载文件失败: ${e.message}" }
        }.getOrNull()
    }