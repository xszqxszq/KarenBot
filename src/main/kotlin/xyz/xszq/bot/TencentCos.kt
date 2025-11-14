package xyz.xszq.bot

import com.qcloud.cos.COSClient
import com.qcloud.cos.ClientConfig
import com.qcloud.cos.auth.BasicCOSCredentials
import com.qcloud.cos.http.HttpProtocol
import com.qcloud.cos.model.PutObjectRequest
import com.qcloud.cos.region.Region
import com.qcloud.cos.transfer.TransferManager
import korlibs.io.file.VfsFile
import xyz.xszq.bot.config.CosConfig
import xyz.xszq.bot.payload.UploadResult
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
    suspend fun uploadBinary(binary: ByteArray, suffix: String = ""): UploadResult {
        return useTempFile(suffix=suffix) {
            it.writeBytes(binary)
            upload(it)
        }
    }
    fun upload(file: VfsFile) = upload(File(file.absolutePath))
}