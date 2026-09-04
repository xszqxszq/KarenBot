package xyz.xszq.bot.service

import com.qcloud.cos.COSClient
import com.qcloud.cos.ClientConfig
import com.qcloud.cos.auth.BasicCOSCredentials
import com.qcloud.cos.http.HttpProtocol
import com.qcloud.cos.model.ObjectMetadata
import com.qcloud.cos.model.PutObjectRequest
import com.qcloud.cos.region.Region
import com.qcloud.cos.transfer.TransferManager
import korlibs.io.file.VfsFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.xszq.bot.config.COSConfig
import xyz.xszq.bot.payload.UploadResult
import java.io.ByteArrayInputStream
import java.io.File
import java.util.*
import java.util.concurrent.Executors

/**
 * 腾讯云 COS 客户端
 *
 * @property config COS 配置
 */
class TencentCOS(
    val config: COSConfig
) {
    private companion object {
        const val PUT_OBJECT_MAX_SIZE = 20L * 1024 * 1024
    }
    val cosClient = COSClient(
        BasicCOSCredentials(config.secretId, config.secretKey),
        ClientConfig(Region(config.region)).also {
            it.httpProtocol = HttpProtocol.https
        })
    private val transferManager = TransferManager(cosClient, Executors.newFixedThreadPool(4))

    suspend fun deleteFromCOS(filename: String) {
        when (config.lightMode) {
            true -> withContext(Dispatchers.IO) {
                File(config.lightDir).resolve(filename).delete()
            }
            false -> withContext(Dispatchers.IO) {
                cosClient.deleteObject(config.appId, filename)
            }
        }
    }

    suspend fun upload(file: File): UploadResult {
        val filename = UUID.randomUUID().toString() + "." + file.extension
        if (config.lightMode)
            return withContext(Dispatchers.IO) {
                file.copyTo(File(config.lightDir).resolve(filename))
                UploadResult("${config.lightUrl}/${filename}", filename)
            }
        return uploadToCOS(filename, file.length(), PutObjectRequest(config.appId, filename, file))
    }

    suspend fun uploadBinary(binary: ByteArray, suffix: String = ""): UploadResult {
        val extensionStr = if (suffix.isNotBlank()) ".$suffix" else ""
        val filename = UUID.randomUUID().toString() + extensionStr
        if (config.lightMode)
            return withContext(Dispatchers.IO) {
                File(config.lightDir).resolve(filename).writeBytes(binary)
                UploadResult("${config.lightUrl}/${filename}", filename)
            }
        val request = PutObjectRequest(config.appId, filename,
            ByteArrayInputStream(binary),
            ObjectMetadata().apply { contentLength = binary.size.toLong() })
        return uploadToCOS(filename, binary.size.toLong(), request)
    }

    private suspend fun uploadToCOS(
        filename: String,
        size: Long,
        request: PutObjectRequest
    ): UploadResult {
        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                if (size < PUT_OBJECT_MAX_SIZE)
                    cosClient.putObject(request)
                else
                    transferManager.upload(request).waitForUploadResult()
            }.onFailure {
                it.printStackTrace()
            }
        }
        return UploadResult("https://${config.appId}.cos.${config.region}.myqcloud.com/$filename", filename)
    }

    suspend fun upload(file: VfsFile) = upload(File(file.absolutePath))
}