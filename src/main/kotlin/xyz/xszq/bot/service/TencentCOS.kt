package xyz.xszq.bot.service

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import korlibs.io.file.VfsFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.xszq.bot.config.COSConfig
import xyz.xszq.bot.payload.UploadResult
import xyz.xszq.bot.util.errorLogger
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * 腾讯云 COS 客户端
 *
 * @property config COS 配置
 */
class TencentCOS(
    val config: COSConfig
) {
    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val OCTET_STREAM = "application/octet-stream"
    }

    private val host = "${config.appId}.cos.${config.region}.myqcloud.com"
    private val baseUrl = "https://$host"
    private val client = HttpClient(OkHttp)

    /**
     * 从对象存储中删除文件
     *
     * @param filename 要删除的文件名
     */
    suspend fun deleteFromCOS(filename: String) {
        when (config.lightMode) {
            true -> deleteLocalFile(filename)
            false -> deleteRemoteObject(filename)
        }
    }

    /**
     * 上传本地文件
     *
     * @param file 要上传的文件
     * @return 上传结果
     */
    suspend fun upload(file: File): UploadResult {
        val filename = UUID.randomUUID().toString() + "." + file.extension
        if (config.lightMode)
            return withContext(Dispatchers.IO) {
                file.copyTo(File(config.lightDir).resolve(filename))
                UploadResult("${config.lightUrl}/$filename", filename)
            }
        val binary = withContext(Dispatchers.IO) { file.readBytes() }
        uploadToCOS(filename, binary)
        return UploadResult("$baseUrl/$filename", filename)
    }

    /**
     * 上传二进制数据
     *
     * @param binary 要上传的二进制内容
     * @param suffix 文件扩展名
     * @return 上传结果
     */
    suspend fun uploadBinary(
        binary: ByteArray,
        suffix: String = ""
    ): UploadResult {
        val filename = UUID.randomUUID().toString() + suffix
        if (config.lightMode)
            return withContext(Dispatchers.IO) {
                File(config.lightDir).resolve(filename).writeBytes(binary)
                UploadResult("${config.lightUrl}/$filename", filename)
            }
        uploadToCOS(filename, binary)
        return UploadResult("$baseUrl/$filename", filename)
    }

    /**
     * 上传 VfsFile
     *
     * @param file 要上传的 VfsFile
     * @return 上传结果
     */
    suspend fun upload(file: VfsFile) = upload(File(file.absolutePath))

    private suspend fun deleteLocalFile(filename: String) =
        withContext(Dispatchers.IO) {
            File(config.lightDir).resolve(filename).delete()
        }

    private suspend fun deleteRemoteObject(filename: String) = runCatching {
        client.delete("$baseUrl${pathOf(filename)}") {
            header(AUTHORIZATION_HEADER, sign("delete", filename))
        }
    }.onFailure { e ->
        errorLogger.error(e) { "[COS] 删除 $filename 失败" }
    }

    private suspend fun uploadToCOS(
        filename: String,
        binary: ByteArray
    ) = runCatching {
        val response = client.put("$baseUrl${pathOf(filename)}") {
            header(AUTHORIZATION_HEADER, sign("put", filename))
            contentType(contentTypeOf(filename))
            setBody(binary)
        }
        ensureSuccess("上传对象", response)
    }.onFailure { e ->
        errorLogger.error(e) { "[COS] 上传 $filename 失败" }
    }

    private fun pathOf(filename: String) =
        "/" + Signer.encode(filename, keepSlash = true)

    private fun sign(
        method: String,
        filename: String
    ) = Signer.authorization(
        config = config,
        host = host,
        method = method,
        path = pathOf(filename)
    )

    private fun contentTypeOf(filename: String) = ContentType.parse(
        when (filename.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            else -> OCTET_STREAM
        }
    )

    private fun ensureSuccess(
        step: String,
        response: HttpResponse
    ) {
        val status = response.status.value
        if (status !in 200..299)
            throw IllegalStateException("$step 失败 HTTP $status")
    }

    /**
     * COS 请求签名
     */
    private object Signer {
        private const val EXPIRE_SECONDS = 3600L

        fun authorization(
            config: COSConfig,
            host: String,
            method: String,
            path: String
        ): String {
            val nowSeconds = System.currentTimeMillis() / 1000
            val keyTime = "$nowSeconds;${nowSeconds + EXPIRE_SECONDS}"
            val httpString = buildString {
                append(method.lowercase()).append('\n')
                append(path).append("\n\n")
                append("host=").append(encode(host)).append('\n')
            }
            val stringToSign = buildString {
                append("sha1").append('\n')
                append(keyTime).append('\n')
                append(sha1Hex(httpString)).append('\n')
            }
            val signKey = hmacSha1(config.secretKey, keyTime)
            val signature = hmacSha1(signKey, stringToSign)
            return buildString {
                append("q-sign-algorithm=sha1")
                append("&q-ak=").append(config.secretId)
                append("&q-sign-time=").append(keyTime)
                append("&q-key-time=").append(keyTime)
                append("&q-header-list=host")
                append("&q-url-param-list=")
                append("&q-signature=").append(signature)
            }
        }

        fun encode(
            value: String,
            keepSlash: Boolean = false
        ): String {
            val builder = StringBuilder()
            value.toByteArray(Charsets.UTF_8).forEach { byte ->
                val char = (byte.toInt() and 0xFF).toChar()
                when {
                    char in 'A'..'Z' -> builder.append(char)
                    char in 'a'..'z' -> builder.append(char)
                    char in '0'..'9' -> builder.append(char)
                    char in "-_.~" -> builder.append(char)
                    keepSlash && char == '/' -> builder.append(char)
                    else -> builder.append('%')
                        .append("%02X".format(byte.toInt() and 0xFF))
                }
            }
            return builder.toString()
        }

        private fun hmacSha1(
            key: String,
            data: String
        ): String {
            val mac = Mac.getInstance("HmacSHA1")
            val keySpec = SecretKeySpec(
                key.toByteArray(Charsets.UTF_8),
                "HmacSHA1"
            )
            mac.init(keySpec)
            return mac.doFinal(data.toByteArray(Charsets.UTF_8)).toHex()
        }

        private fun sha1Hex(data: String) =
            MessageDigest.getInstance("SHA-1")
                .digest(data.toByteArray(Charsets.UTF_8)).toHex()

        private fun ByteArray.toHex() =
            joinToString("") { "%02x".format(it) }
    }
}