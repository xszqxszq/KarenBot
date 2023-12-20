package xyz.xszq.nereides

import com.qcloud.cos.COSClient
import com.qcloud.cos.ClientConfig
import com.qcloud.cos.auth.BasicCOSCredentials
import com.qcloud.cos.http.HttpProtocol
import com.qcloud.cos.model.PutObjectRequest
import com.qcloud.cos.region.Region
import com.qcloud.cos.transfer.TransferManager
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import korlibs.io.file.VfsFile
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import xyz.xszq.config
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.concurrent.Executors

object NetworkUtils {
    fun deleteFromCos(filename: String) {
        val cosClient = COSClient(
            BasicCOSCredentials(config.cosSecretId, config.cosSecretKey),
            ClientConfig(Region(config.cosRegion)).also {
                it.httpProtocol = HttpProtocol.https
            })
        val bucketName = config.cosAppId
        cosClient.deleteObject(bucketName, filename)
    }
    private fun upload(file: File): UploadResult {
        val cosClient = COSClient(
            BasicCOSCredentials(config.cosSecretId, config.cosSecretKey),
            ClientConfig(Region(config.cosRegion)).also {
            it.httpProtocol = HttpProtocol.https
        })
        val bucketName = config.cosAppId
        val transferManager = TransferManager(cosClient, Executors.newFixedThreadPool(32))
        val filename = UUID.randomUUID().toString() + "." + file.extension
        val result = kotlin.runCatching {
            val upload = transferManager.upload(PutObjectRequest(
                bucketName, filename, file
            ))
            upload.waitForUploadResult()
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()
        transferManager.shutdownNow()
        cosClient.shutdown()

        val expiration = Date(Date().time + 2 * 60 * 1000)
        return UploadResult(cosClient.generatePresignedUrl(bucketName, filename, expiration).toString(), filename)
    }
    fun uploadBinary(binary: ByteArray, suffix: String = ""): UploadResult {
        val file = newTempFile(suffix=suffix).also {
            it.writeBytes(binary)
        }
        return upload(file).also {
            file.delete()
        }
    }
    fun upload(file: VfsFile) = upload(File(file.absolutePath))

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