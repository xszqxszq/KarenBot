package xyz.xszq.bot.util

import xyz.xszq.bot.crypto.Ed25519
import xyz.xszq.bot.payload.WebhookResponse
import xyz.xszq.bot.payload.WebhookValidation
import java.util.HexFormat
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
 * @param seed Ed25519 种子
 * @param message 待签名的消息
 */
fun signMessage(
    seed: ByteArray,
    message: ByteArray
): String {
    val signature = ByteArray(Ed25519.SIGNATURE_SIZE)
    Ed25519.sign(seed, 0, message, 0, message.size, signature, 0)
    return HexFormat.of().formatHex(signature)
}

/**
 * 校验腾讯服务器发来的 Webhook 签名
 *
 * @param publicKey 公钥
 * @param message 被签名的消息
 * @param signature 签名
 */
fun verifySignature(
    publicKey: ByteArray,
    message: ByteArray,
    signature: ByteArray
): Boolean = Ed25519.verify(publicKey, 0, message, 0, message.size, signature, 0)

/**
 * 处理 Webhook 地址验证请求
 *
 * @param secret 机器人客户端密钥
 * @param data 请求
 */
fun handleValidation(
    secret: String,
    data: WebhookValidation,
): WebhookResponse? = runCatching {
    val message = "${data.eventTs}${data.plainToken}"
    WebhookResponse(
        plainToken = data.plainToken,
        signature = signMessage(buildSeed(secret), message.toByteArray(UTF_8))
    )
}.onFailure { e ->
    e.printStackTrace()
}.getOrNull()

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
    val signature = runCatching {
        HexFormat.of().parseHex(signatureHeader)
    }.getOrNull() ?: return false
    if (signature.size != 64 || signature[63].toInt() and 224 != 0) {
        return false
    }

    val message = timestampHeader + body

    return verifySignature(
        VerifierKey.of(secret),
        message.toByteArray(UTF_8),
        signature
    )
}