package xyz.xszq.nereides.payload.event

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.xszq.nereides.payload.user.GuildMember
import xyz.xszq.nereides.payload.user.GuildUser

@Serializable
data class GuildAtMessageCreate(
    val author: GuildUser,
    @SerialName("channel_id")
    val channelId: String,
    val content: String,
    @SerialName("guild_id")
    val guildId: String,
    val id: String,
    val member: GuildMember,
    val mentions: List<GuildUser>,
    val seq: Int,
    @SerialName("seq_in_channel")
    val seqInChannel: String,
    val timestamp: String
)
