package xyz.xszq.bot

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.bouncycastle.util.encoders.Hex
import xyz.xszq.bot.payload.WebhookResponse
import xyz.xszq.bot.payload.WebhookValidation
import kotlin.text.Charsets.UTF_8

/**
 * Build Ed25519 seed.
 * @param secret Bot's client secret.
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
 * Sign Message to respond.
 * @param privateKeyParams private key.
 * @param message The message to sign.
 */
fun signMessage(privateKeyParams: Ed25519PrivateKeyParameters, message: ByteArray): String {
    val signer = Ed25519Signer()
    signer.init(true, privateKeyParams)
    signer.update(message, 0, message.size)
    return Hex.toHexString(signer.generateSignature())
}

/**
 * Verify Webhook signature from Tencent server.
 * @param publicKeyParams public key.
 * @param message The message signed.
 * @param signature Signature.
 */
fun verifySignature(publicKeyParams: Ed25519PublicKeyParameters, message: ByteArray, signature: ByteArray): Boolean {
    val signer = Ed25519Signer()
    signer.init(false, publicKeyParams)
    signer.update(message, 0, message.size)
    return signer.verifySignature(signature)
}

/**
 * Handle Webhook validation request.
 * @param data Request body.
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
 * Verify Webhook request's headers.
 * @param signatureHeader X-Signature-Ed25519
 * @param timestampHeader X-Signature-Timestamp
 * @param body HTTP Body
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