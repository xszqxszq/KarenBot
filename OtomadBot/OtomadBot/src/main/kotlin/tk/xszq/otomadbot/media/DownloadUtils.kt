@file:Suppress("unused")
package tk.xszq.otomadbot.media

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import tk.xszq.otomadbot.newTempFile
import tk.xszq.otomadbot.tempDir
import java.io.File
import java.net.InetSocketAddress
import java.net.Proxy


suspend fun getDownloadFileSize(url: String): Long {
    return HttpClient(CIO).get<HttpResponse>(url).contentLength()!!
}

/**
 * Download file to tmp dir.
 * @param url URL of this image
 * @param filename Filename without suffix
 * @param suffix File suffix("." needed)
 * @param path Where to store the image
 */
@Suppress("EXPERIMENTAL_API_USAGE")
suspend fun downloadFile(
    url: String, filename: String, suffix: String = "",
    path: String = tempDir, requiredHeaders: List<Pair<String, String>>? = null,
    proxyEnabled: Boolean = false
): File {
    val client = HttpClient(CIO) {
        if (proxyEnabled)
            engine {
                proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(18889))
            }
    }
    val file = File("$path/$filename$suffix")
    val bytes = client.get<ByteReadChannel>(url) {
        headers {
            requiredHeaders?.forEach {
                append(it.first, it.second)
            }
        }
    }
    val byteBufferSize = 1024 * 100
    val byteBuffer = ByteArray(byteBufferSize)
    var read = 0
    file.writeChannel().use {
        do {
            val currentRead = bytes.readAvailable(byteBuffer, 0, byteBufferSize)
            if (currentRead > 0) {
                read += currentRead
                writeFully(byteBuffer, 0, currentRead)
            }
        } while (currentRead >= 0)
    }
    return file
}

suspend fun Image.getFile(): File {
    val client = HttpClient(CIO)
    val file = newTempFile("mirai_", "")
    file.writeBytes(client.get<HttpResponse>(queryUrl()).readBytes())
    return file
}
