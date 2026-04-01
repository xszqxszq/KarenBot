package xyz.xszq.bot

import com.qcloud.cos.COSClient
import com.qcloud.cos.ClientConfig
import com.qcloud.cos.auth.BasicCOSCredentials
import com.qcloud.cos.http.HttpProtocol
import com.qcloud.cos.model.ObjectMetadata
import com.qcloud.cos.model.PutObjectRequest
import com.qcloud.cos.region.Region
import com.qcloud.cos.transfer.TransferManager
import korlibs.io.file.VfsFile
import xyz.xszq.bot.config.CosConfig
import xyz.xszq.bot.payload.UploadResult
import java.io.ByteArrayInputStream
import java.io.File
import java.util.*
import java.util.concurrent.Executors

class TencentCos(
    val config: CosConfig
) {
    val cosClient = COSClient(
        BasicCOSCredentials(config.secretId, config.secretKey),
        ClientConfig(Region(config.region)).also {
            it.httpProtocol = HttpProtocol.https
        })
    val transferManager = TransferManager(cosClient, Executors.newFixedThreadPool(32))
    fun deleteFromCos(filename: String) {
        when (config.lightMode) {
            true -> {
                File(config.lightDir).resolve(filename).delete()
            }
            false -> {
                val bucketName = config.appId
                cosClient.deleteObject(bucketName, filename)
            }
        }
    }
    fun upload(file: File): UploadResult {
        val filename = UUID.randomUUID().toString() + "." + file.extension
        when (config.lightMode) {
            true -> {
                file.copyTo(File(config.lightDir).resolve(filename))
                return UploadResult("${config.lightUrl}/${filename}", filename)
            }
            false -> {
                val bucketName = config.appId
                kotlin.runCatching {
                    val upload = transferManager.upload(PutObjectRequest(
                        bucketName, filename, file
                    ))
                    upload.waitForUploadResult()
                }.onFailure {
                    it.printStackTrace()
                }

                val expiration = Date(Date().time + 2 * 60 * 1000)
                return UploadResult(cosClient.generatePresignedUrl(bucketName, filename, expiration).toString(), filename)
            }
        }
    }
    fun uploadBinary(binary: ByteArray, suffix: String = ""): UploadResult {
        val extensionStr = if (suffix.isNotBlank()) ".$suffix" else ""
        val filename = UUID.randomUUID().toString() + extensionStr

        when (config.lightMode) {
            true -> {
                File(config.lightDir).resolve(filename).writeBytes(binary)
                return UploadResult("${config.lightUrl}/${filename}", filename)
            }
            false -> {
                val bucketName = config.appId
                kotlin.runCatching {
                    val inputStream = ByteArrayInputStream(binary)

                    val metadata = ObjectMetadata().apply {
                        contentLength = binary.size.toLong()
                    }

                    val upload = transferManager.upload(
                        PutObjectRequest(bucketName, filename, inputStream, metadata)
                    )
                    upload.waitForUploadResult()
                }.onFailure {
                    it.printStackTrace()
                }

                val expiration = Date(Date().time + 2 * 60 * 1000)
                return UploadResult(cosClient.generatePresignedUrl(bucketName, filename, expiration).toString(), filename)
            }
        }
    }
    fun upload(file: VfsFile) = upload(File(file.absolutePath))
}