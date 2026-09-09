package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 水鱼 OAuth 设备授权响应
 *
 * 用户在验证页面输入 `userCode` 完成授权，`deviceCode`
 * 用于轮询授权结果
 *
 * @param expiresIn 授权码有效期（秒）
 * @param interval 轮询间隔（秒）
 */
@Serializable
data class DivingFishDeviceAuthorizationResponse(
    @SerialName("device_code")
    val deviceCode: String,
    @SerialName("user_code")
    val userCode: String,
    @SerialName("verification_uri")
    val verificationUri: String,
    @SerialName("verification_uri_complete")
    val verificationUriComplete: String,
    @SerialName("expires_in")
    val expiresIn: Int,
    val interval: Int
)