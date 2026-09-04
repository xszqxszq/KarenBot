package xyz.xszq.bot.util

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.bouncycastle.util.encoders.Hex
import xyz.xszq.bot.payload.WebhookResponse
import xyz.xszq.bot.payload.WebhookValidation
import kotlin.text.Charsets.UTF_8

/**
 * 构造 Ed25519 种子
 *
 * @param secret 机器人客户端密钥
 */
fun buildSeed(secret: String): ByteArray {
    val ed25519SeedSize = 32
    var seed = secret
    while (seed.length < ed25519SeedSize) {
        seed += seed
    }
    return seed.substring(0, ed25519SeedSize).toByteArray(UTF_8)
}

/**
 * 签名消息以响应验证
 *
 * @param privateKeyParams 私钥
 * @param message 待签名的消息
 */
fun signMessage(privateKeyParams: Ed25519PrivateKeyParameters, message: ByteArray): String {
    val signer = Ed25519Signer()
    signer.init(true, privateKeyParams)
    signer.update(message, 0, message.size)
    return Hex.toHexString(signer.generateSignature())
}

/**
 * 校验腾讯服务器发来的 Webhook 签名
 *
 * @param publicKeyParams 公钥
 * @param message 被签名的消息
 * @param signature 签名
 */
fun verifySignature(publicKeyParams: Ed25519PublicKeyParameters, message: ByteArray, signature: ByteArray): Boolean {
    val signer = Ed25519Signer()
    signer.init(false, publicKeyParams)
    signer.update(message, 0, message.size)
    return signer.verifySignature(signature)
}

/**
 * 处理 Webhook 地址验证请求
 *
 * @param secret 机器人客户端密钥
 * @param data 请求
 */
fun handleValidation(
    secret: String,
    data: WebhookValidation,
): WebhookResponse? {
    try {
        val seed = buildSeed(secret)
        val privateKeyParams = Ed25519PrivateKeyParameters(seed, 0)

        val message = "${data.eventTs}${data.plainToken}"
        val signature = signMessage(privateKeyParams, message.toByteArray(UTF_8))

        return WebhookResponse(
            plainToken = data.plainToken,
            signature = signature
        )
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

/**
 * 校验 Webhook 请求头签名
 *
 * @param secret 机器人客户端密钥
 * @param signatureHeader X-Signature-Ed25519 请求头
 * @param timestampHeader X-Signature-Timestamp 请求头
 * @param body HTTP 请求
 */
fun verifyBody(
    secret: String,
    signatureHeader: String,
    timestampHeader: String,
    body: String
): Boolean {
    try {
        val signature = Hex.decode(signatureHeader)
        if (signature.size != 64 || signature[63].toInt() and 224 != 0) {
            return false
        }

        val seed = buildSeed(secret)

        val privateKeyParams = Ed25519PrivateKeyParameters(seed, 0)
        val publicKeyParams = privateKeyParams.generatePublicKey()

        val message = timestampHeader + body

        return verifySignature(publicKeyParams, message.toByteArray(UTF_8), signature)
    } catch (e: Exception) {
        e.printStackTrace()
        return false
    }
}