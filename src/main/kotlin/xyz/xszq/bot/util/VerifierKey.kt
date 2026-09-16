package xyz.xszq.bot.util

import xyz.xszq.bot.crypto.Ed25519
import java.util.concurrent.ConcurrentHashMap

/**
 * Webhook 验签公钥缓存
 */
internal object VerifierKey {
    private val keys = ConcurrentHashMap<String, ByteArray>()

    /**
     * 取指定 Secret 派生的验签公钥
     *
     * @param secret 机器人客户端密钥
     * @return 32 字节 Ed25519 公钥
     */
    fun of(secret: String): ByteArray = keys.computeIfAbsent(secret) { value ->
        val publicKey = ByteArray(Ed25519.PUBLIC_KEY_SIZE)
        Ed25519.generatePublicKey(buildSeed(value), 0, publicKey, 0)
        publicKey
    }
}