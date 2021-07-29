package tk.xszq.otomadbot

import com.soywiz.korio.util.UUID
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import ru.gildor.coroutines.okhttp.await
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream


@Suppress("MemberVisibilityCanBePrivate")
object NetworkUtils {
    fun getDownloadFileSize(url: String): Long {
        val client = OkHttpClient()
        val request: Request = Request.Builder().url(url).head().build()
        var response: Response? = null
        try {
            response = client.newCall(request).execute()
            val size: Long = response.body!!.contentLength()
            response.close()
            return size
        } catch (e: IOException) {
            response?.close()
            e.printStackTrace()
        }
        return 0L
    }
    fun downloadFile(url: String, dir: File, filename: String, headers: List<Pair<String, String>> = emptyList()): File? {
        val requestBuilder = Request.Builder()
            .url(url)
            .get()
        headers.forEach {
            requestBuilder.addHeader(it.first, it.second)
        }
        val call = OkHttpClient().newCall(requestBuilder.build())
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
                    return result
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
        return null
    }
    fun downloadTempFile(url: String, headers: List<Pair<String, String>> = emptyList(), ext: String = "")
        = downloadFile(url, tempDir, UUID.randomUUID().toString() + if (ext.isNotEmpty()) ".$ext" else "", headers)
    suspend fun Image.getFile(): File? = downloadTempFile(queryUrl())
}