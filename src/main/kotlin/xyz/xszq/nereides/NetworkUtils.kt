package xyz.xszq.nereides

import com.qcloud.cos.COSClient
import com.qcloud.cos.ClientConfig
import com.qcloud.cos.auth.BasicCOSCredentials
import com.qcloud.cos.http.HttpProtocol
import com.qcloud.cos.model.PutObjectRequest
import com.qcloud.cos.region.Region
import com.qcloud.cos.transfer.TransferManager
import korlibs.io.file.VfsFile
import korlibs.io.file.std.tempVfs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        cosClient.shutdown()
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
    suspend fun uploadBinary(binary: ByteArray, suffix: String = ""): UploadResult {
        return useTempFile(suffix=suffix) {
            it.writeBytes(binary)
            upload(it)
        }
    }
    fun upload(file: VfsFile) = upload(File(file.absolutePath))

    private suspend fun downloadFile(
        url: String,
        dir: VfsFile,
        filename: String,
        headers: List<Pair<String, String>> = emptyList()
    ): VfsFile? = withContext(Dispatchers.IO) {
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
                    val result = dir[filename]
                    val output = FileOutputStream(File(result.absolutePath))
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
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    inputStream?.close()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return@withContext null
    }
    private suspend fun downloadTempFile(
        url: String,
        headers: List<Pair<String, String>> = emptyList(),
        ext: String = ""
    ) = downloadFile(url, tempVfs, UUID.randomUUID().toString() + if (ext.isNotEmpty()) ".$ext" else "",
        headers)
    suspend fun <R> useNetFile(
        url: String,
        headers: List<Pair<String, String>> = emptyList(),
        ext: String = "",
        block: suspend (VfsFile) -> R
    ): R {
        return downloadTempFile(url, headers, ext) ?.let { file ->
            block(file).also {
                file.delete()
            }
        } ?: run {
            throw Exception("网络异常")
        }
    }
}