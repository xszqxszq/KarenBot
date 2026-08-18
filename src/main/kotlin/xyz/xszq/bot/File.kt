package xyz.xszq.bot

import io.github.oshai.kotlinlogging.KLogger
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import korlibs.io.file.VfsFile
import korlibs.io.file.std.tempVfs
import java.util.*

/**
 * Create a new Temp file.
 * @param prefix Filename prefix
 * @param suffix Filename suffix
 */
fun newTempFile(
    prefix: String = "",
    suffix: String = ""
) = tempVfs[prefix + UUID.randomUUID().toString() + suffix]

/**
 * Use a VfsFile and delete it.
 * @param block The code to use the VfsFile.
 */
suspend fun <T> VfsFile.use(block: suspend (VfsFile) -> T): T {
    return try {
        block.invoke(this)
    } finally {
        delete()
    }
}

/**
 * Create a temp file and delete it after using.
 * @param prefix Filename prefix
 * @param suffix Filename suffix
 * @param block The code to use the temp file.
 */
suspend fun <R> useTempFile(
    prefix: String = "",
    suffix: String = "",
    block: suspend (VfsFile) -> R
): R = newTempFile(prefix, suffix).use(block)

fun createDownloadClient() = HttpClient(OkHttp)



/**
 * Download File from URL.
 * @param url The URL.
 * @param filename Filename.
 * @param logger Logger to log error.
 */
suspend fun downloadFile(url: String, filename: String, logger: KLogger): VfsFile? {
    val client = createDownloadClient()
    return client.use { client ->
        downloadFile(url, filename, logger, client)
    }
}

suspend fun downloadFile(url: String, filename: String, logger: KLogger, client: HttpClient): VfsFile? {
    val file = tempVfs[filename]
    try {
        val response = client.get(url)
        file.write(response.bodyAsBytes())
        return file
    } catch (e: Exception) {
        logger.error { "Error downloading file: ${e.message}" }
    }
    return null
}