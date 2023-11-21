@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xszq.nereides.payload.websocket

object Intents {
    // 频道事件
    const val GUILDS = 1 shl 0
    const val GUILD_MEMBERS = 1 shl 1
    const val GUILD_MESSAGES = 1 shl 9 // 仅私域机器人
    const val GUILD_MESSAGE_REACTIONS = 1 shl 10
    const val DIRECT_MESSAGE = 1 shl 12
    const val INTERACTION = 1 shl 26
    const val MESSAGE_AUDIT = 1 shl 27
    const val FORUMS_EVENT = 1 shl 28
    const val AUDIO_ACTION = 1 shl 29
    const val PUBLIC_GUILD_MESSAGES = 1 shl 30
    // 群/私聊事件
    const val C2C = 1 shl 25

    val GUILD_ALL: Int
        get() {
            return GUILDS or
                    GUILD_MEMBERS or
                    GUILD_MESSAGE_REACTIONS or
                    DIRECT_MESSAGE or
                    INTERACTION or
                    MESSAGE_AUDIT or
                    FORUMS_EVENT or
                    AUDIO_ACTION or
                    PUBLIC_GUILD_MESSAGES

        }
    val PUBLIC_ALL: Int
        get() {
            return GUILD_ALL or C2C
        }
    val PRIVATE_ALL: Int
        get() {
            return GUILD_ALL or GUILD_MESSAGES
        }
}