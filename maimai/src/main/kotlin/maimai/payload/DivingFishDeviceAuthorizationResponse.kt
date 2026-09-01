package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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