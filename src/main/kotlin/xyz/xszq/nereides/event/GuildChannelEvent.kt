package xyz.xszq.nereides.event

interface GuildChannelEvent: GuildEvent {
    val channelId: String
}