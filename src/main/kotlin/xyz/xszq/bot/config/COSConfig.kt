package xyz.xszq.bot.config

import kotlinx.serialization.Serializable

/**
 * 腾讯云对象存储配置
 *
 * `lightMode` == true 将会使用轻量级对象存储，而非对象存储
 *
 * @param appId 对象存储的 App ID
 * @param region 对象存储的区域
 * @param secretId 对象存储的密钥 ID
 * @param secretKey 对象存储的密钥
 * @param lightMode 是否使用轻量级对象存储
 * @param lightDir 轻量级对象存储的挂载路径
 * @param lightUrl 轻量级对象存储的公网 URL
 */
@Serializable
data class COSConfig(
    val appId: String = "",
    val region: String = "",
    val secretId: String = "",
    val secretKey: String = "",
    val lightMode: Boolean = false,
    val lightDir: String = "",
    val lightUrl: String = ""
)