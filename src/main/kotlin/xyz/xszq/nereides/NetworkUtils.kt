package xyz.xszq.nereides

import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.baseName
import com.soywiz.korio.util.UUID
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import xyz.xszq.config
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

object NetworkUtils {
    private val client = HttpClient(OkHttp) {
    }
    suspend fun upload(file: VfsFile): String {
        val bytes = file.readBytes()
        return try {
            config.uploadServer + client.submitFormWithBinaryData(
                url = "${config.uploadServer}/upload",
                formData = formData {
                    append("file", bytes, Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=${file.baseName}")
                    })
                }
            ) {
                headers.append("secret", config.uploadSecret)
            }.bodyAsText()
        } catch (e: Exception) {
            ""
        }
    }
    suspend fun uploadBinary(binary: ByteArray): String {
        return try {
            config.uploadServer + client.submitFormWithBinaryData(
                url = "${config.uploadServer}/upload",
                formData = formData {
                    append("file", binary, Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=${UUID.randomUUID()}")
                    })
                }
            ) {
                headers.append("secret", config.uploadSecret)
            }.bodyAsText()
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun downloadFile(
        url: String,
        dir: File,
        filename: String,
        headers: List<Pair<String, String>> = emptyList()
    ): File? = withContext(Dispatchers.IO) {
        val requestBuilder = Request.Builder()
            .url(url)
            .get()
        headers.forEach {
            requestBuilder.addHeader(it.first, it.second)
        }
        val clientBuilder = OkHttpClient.Builder()
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
    suspend fun downloadTempFile(url: String, headers: List<Pair<String, String>> = emptyList(), ext: String = "")
            = downloadFile(url, tempDir, UUID.randomUUID().toString() + if (ext.isNotEmpty()) ".$ext" else "",
        headers)
}