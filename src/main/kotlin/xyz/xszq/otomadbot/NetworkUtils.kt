package xyz.xszq.otomadbot

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korio.util.UUID
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.data.MarketFace
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import xyz.xszq.otomadbot.api.ApiSettings
import xyz.xszq.otomadbot.kotlin.tempDir
import xyz.xszq.otomadbot.mirai.queryUrl
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Proxy

@Suppress("MemberVisibilityCanBePrivate")
object NetworkUtils {
    val client = HttpClient()
    val clientProxy = HttpClient {
        engine {
            proxy =
                if (ApiSettings.data.proxy.type.lowercase() == "socks")
                    ProxyBuilder.socks(ApiSettings.data.proxy.addr, ApiSettings.data.proxy.port)
                else
                    ProxyBuilder.http("http://${ApiSettings.data.proxy.addr}:${ApiSettings.data.proxy.port}")
        }
    }
    suspend fun getDownloadFileSize(url: String): Long = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request: Request = Request.Builder().url(url).head().build()
        var response: Response? = null
        try {
            response = client.newCall(request).execute()
            val size: Long = response.body!!.contentLength()
            response.close()
            return@withContext size
        } catch (e: IOException) {
            response?.close()
            e.printStackTrace()
        }
        return@withContext 0L
    }
    suspend fun downloadFile(url: String, dir: File, filename: String, headers: List<Pair<String, String>> = emptyList(),
                     proxy: Boolean = false): File? = withContext(Dispatchers.IO) {
        val requestBuilder = Request.Builder()
            .url(url)
            .get()
        headers.forEach {
            requestBuilder.addHeader(it.first, it.second)
        }
        val clientBuilder = OkHttpClient.Builder()
        if (proxy)
            clientBuilder.proxy(
                Proxy(if (ApiSettings.data.proxy.type.lowercase() == "socks") Proxy.Type.SOCKS else Proxy.Type.HTTP,
                    InetSocketAddress(ApiSettings.data.proxy.addr, ApiSettings.data.proxy.port))
            )
        val call = clientBuilder.build().newCall(requestBuilder.build())
        try {
            val response = call.execute()
            if (response.code == 200 || response.code == 201) {
                var inputStream: InputStream? = null
                try {
                    inputStream = response.body!!.byteStream()
                    val buff = ByteArray(1024 * 4)
                    var downloaded = 0L
                    val result = File(dir, filename)
                    val output = FileOutputStream(result)
                    while (true) {
                        val readed = inputStream.read(buff)
                        if (readed == -1)
                            break
                        output.write(buff, 0, readed)
                        downloaded += readed.toLong()
                    }
                    output.flush()
                    output.close()
                    return@withContext result
                } catch (ignore: IOException) {
                    println("警告：下载文件过程中出现了 Exception")
                    ignore.printStackTrace()
                } finally {
                    inputStream?.close()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return@withContext null
    }
    suspend fun downloadTempFile(url: String, headers: List<Pair<String, String>> = emptyList(), ext: String = "",
                         proxy: Boolean = false)
            = downloadFile(url, tempDir, UUID.randomUUID().toString() + if (ext.isNotEmpty()) ".$ext" else "",
        headers, proxy)
    suspend fun downloadAsByteArray(url: String, headers: List<Pair<String, String>> = emptyList(),
                                    proxy: Boolean = false): ByteArray {
        return (if (proxy) clientProxy else client).get(url) {
            headers {
                headers.fastForEach {
                    append(it.first, it.second)
                }
            }
        }
    }
    suspend fun Image.getFile(): File? = downloadTempFile(queryUrl())
    suspend fun MarketFace.getFile(): File? = downloadTempFile(queryUrl())
}