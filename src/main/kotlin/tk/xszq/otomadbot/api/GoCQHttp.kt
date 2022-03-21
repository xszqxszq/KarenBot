@file:Suppress("unused", "PropertyName")

package tk.xszq.otomadbot.api

import com.soywiz.kds.iterators.fastForEach
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import tk.xszq.otomadbot.GuildHandler.guildBot
import kotlin.properties.Delegates


@Serializable
open class GOCQEvent {
    lateinit var post_type: String
    var time by Delegates.notNull<Long>()
    companion object {
        private val json = Json {
            isLenient = true
            ignoreUnknownKeys = true
        }
        fun parse(raw: JsonObject) = when (json.decodeFromJsonElement<GOCQEvent>(raw).post_type) {
            "message" -> {
                when (json.decodeFromJsonElement<GOCQMessageEvent>(raw).message_type) {
                    "guild" -> json.decodeFromJsonElement<GOCQGuildMessageEvent>(raw)
                    "group" -> json.decodeFromJsonElement<GOCQGroupMessageEvent>(raw)
                    "private" -> json.decodeFromJsonElement<GOCQPrivateMessageEvent>(raw)
                    else -> null
                }
            }
            "notice" -> null
            "meta_event" -> {
                if (json.decodeFromJsonElement<GOCQMetaEvent>(raw).meta_event_type == "heartbeat")
                    json.decodeFromJsonElement<GOCQHeartbeatEvent>(raw)
                else
                    null
            }
            else -> null
        }
    }
}

@Serializable
open class GOCQMessageEvent: GOCQEvent() {
    lateinit var message_type: String
    lateinit var message: String
    lateinit var sub_type: String
    lateinit var sender: GOCQSender
    var self_id by Delegates.notNull<Long>()
}

@Serializable
open class GOCQMetaEvent: GOCQEvent() {
    lateinit var meta_event_type: String
}

@Serializable
class GOCQHeartbeatEvent: GOCQMetaEvent() {
    var interval by Delegates.notNull<Long>()
    var self_id by Delegates.notNull<Long>()
    // lateinit var status: GOCQHeartbeatStatus // TODO: Implement this
}

@Serializable
class GOCQGuildMessageEvent: GOCQMessageEvent() {
    lateinit var channel_id: String
    lateinit var guild_id: String
    lateinit var message_id: String
    lateinit var user_id: String
    lateinit var self_tiny_id: String
    val channel by lazy {
        guildBot.guilds.find { it.id == guild_id }!!.channels.find { it.id == channel_id }!!
    }
}

@Serializable
open class GOCQNormalMessageEvent: GOCQMessageEvent() {
    lateinit var raw_message: String
    var font by Delegates.notNull<Int>()
    var message_id by Delegates.notNull<Long>()
}

@Serializable
class GOCQGroupMessageEvent: GOCQNormalMessageEvent() {
    var group_id by Delegates.notNull<Long>()
}

@Serializable
class GOCQPrivateMessageEvent: GOCQNormalMessageEvent() {
    var temp_source by Delegates.notNull<Int>()
}

@Serializable
data class GOCQSender(
    val age: Int = 0, val area: String = "", val card: String = "", val level: String = "", val nickname: String = "",
    val role: String = "", val sex: String = "", val title: String = "", val user_id: Long = 0
)

@Serializable
data class GOCQResponse<T>(
    val status: String,
    val retcode: Int,
    val data: T?
)

class Guild3rdPartyBot(
    val nickname: String,
    val id: String,
    val avatarUrl: String,
    val guilds: MutableList<Guild> = mutableListOf()
) {
    suspend fun getGuildList() {
        guilds.clear()
        coroutineScope {
            client.get<GOCQResponse<List<GuildInfo>>>("$api/get_guild_list").data!!.fastForEach {
                launch {
                    kotlin.runCatching {
                        guilds.add(client.get<GOCQResponse<Guild>>("$api/get_guild_meta_by_guest") {
                            parameter("guild_id", it.guild_id)
                        }.data!!.apply {
                            id = it.guild_id
                            displayId = it.guild_display_id
                            setOwner()
                            setChannels()
                        })
                    }.onFailure {
                        it.printStackTrace()
                    }
                }
            }
        }
    }
    companion object {
        private const val port = 19198
        const val api = "http://127.0.0.1:$port"
        val client = HttpClient {
            install(JsonFeature) {
                serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 60000L
                requestTimeoutMillis = 60000L
                socketTimeoutMillis = 60000L
            }
            expectSuccess = false
        }
        suspend fun get(): Guild3rdPartyBot {
            val info = client.get<GOCQResponse<Guild3rdPartyBotInfo>>("$api/get_guild_service_profile").data!!
            val result = Guild3rdPartyBot(info.nickname, info.tiny_id, info.avatar_url)
            result.getGuildList()
            return result
        }
    }
}

@Serializable
data class Guild3rdPartyBotInfo(
    val nickname: String, val tiny_id: String, val avatar_url: String
)
@Serializable
data class GuildInfo(
    val guild_id: String, val guild_name: String, val guild_display_id: Long
)

@Serializable
enum class GuildChannelType(val value: Int) {
    Text(1),
    VoiceStream(2),
    VideoStream(5),
    Topic(7)
}

@Serializable
data class Guild(
    private val guild_id: String,
    private val guild_name: String,
    private val guild_profile: String,
    private val create_time: Long,
    private val max_member_count: Long,
    private val max_robot_count: Long,
    private val max_admin_count: Long,
    private val member_count: Long,
    private val owner_id: String,
    var id: String = guild_id,
    val name: String = guild_name,
    val profile: String = guild_profile,
    val createTime: Long = create_time,
    val maxMember: Long = max_member_count,
    val maxRobot: Long = max_robot_count,
    val maxAdmin: Long = max_admin_count,
    var channels: List<GuildChannel> = emptyList(),
    var owner: GuildMember ?= null,
    var displayId: Long = 0,
    var members: Long = member_count
) {
    suspend fun getMember(userId: String)= Guild3rdPartyBot.client
        .get<GOCQResponse<GuildMember>>("${Guild3rdPartyBot.api}/get_guild_member_profile") {
            parameter("guild_id", id)
            parameter("user_id", userId)
        }.data
    suspend fun setOwner() {
        kotlin.runCatching {
            owner = getMember(owner_id)
        }.onFailure {
            it.printStackTrace()
        }
    }
    suspend fun setChannels() {
        kotlin.runCatching {
            channels = Guild3rdPartyBot.client
                .get<GOCQResponse<List<GuildChannel>>>("${Guild3rdPartyBot.api}/get_guild_channel_list") {
                    parameter("guild_id", id)
                    parameter("no_cache", false)
                }.data!!.apply {
                    coroutineScope {
                        fastForEach {
                            launch {
                                it.setCreator()
                            }
                        }
                    }
                }
        }.onFailure {
            it.printStackTrace()
        }
    }
}

@Serializable
data class GuildChannel(
    private val owner_guild_id: String,
    private val channel_id: String,
    private val channel_type: Int,
    private val channel_name: String,
    private val create_time: Long,
    private val creator_tiny_id: String,
    private val talk_permission: Int,
    private val visible_type: Int,
    private val current_slow_mode: Int,
    private val slow_modes: List<GuildSlowModeInfo>,
    val id: String = channel_id,
    val type: Int = channel_type,
    val name: String = channel_name,
    val guildId: String = owner_guild_id,
    val createTime: Long = create_time,
    val talkPermission: Int = talk_permission,
    val visible: Int = visible_type,
    val slowModeNow: Int = current_slow_mode,
    val slowModes: List<GuildSlowModeInfo> = emptyList(),
    var creator: GuildMember ?= null,
) {
    suspend fun getMember(userId: String) = Guild3rdPartyBot.client
        .get<GOCQResponse<GuildMember>>("${Guild3rdPartyBot.api}/get_guild_member_profile") {
            parameter("guild_id", guildId)
            parameter("user_id", userId)
        }.data
    suspend fun setCreator() {
        kotlin.runCatching {
            creator = getMember(creator_tiny_id)
        }.onFailure {
            it.printStackTrace()
        }
    }
    suspend fun sendMessage(message: MessageChain) = Guild3rdPartyBot.client
        .get<GOCQResponse<GuildMessageFeedback>>("${Guild3rdPartyBot.api}/send_guild_channel_msg") {
            parameter("guild_id", guildId)
            parameter("channel_id", id)
            parameter("message", message.serializeToCQ())
        }.data
}
suspend fun GOCQGuildMessageEvent.quoteReply(message: MessageChain) = channel.sendMessage(buildMessageChain {
    // add(GuildReply(message_id))
    add(GuildAt(sender.user_id))
    addAll(message)
})
suspend fun GOCQGuildMessageEvent.quoteReply(message: Message) = quoteReply(messageChainOf(message))
suspend fun GOCQGuildMessageEvent.quoteReply(text: String) = quoteReply(text.toPlainText())

@Suppress("EXPERIMENTAL_API_USAGE")
suspend fun MessageChain.serializeToCQ(): String {
    val result = StringBuilder()
    fastForEach {
        when (it) {
            is PlainText -> result.append(it.content)
            is Image -> result.append("[CQ:image,file=${it.md5}.image,url=${it.queryUrl()}]")
            else -> result.append(it.contentToString())
        }
    }
    return result.toString()
}

@Serializable
data class GuildMessageFeedback(
    val message_id: String
)

@Serializable
data class GuildSlowModeInfo(
    private val slow_mode_key: Int,
    private val slow_mode_text: String,
    private val slow_mode_circle: Int,
    private val speak_frequency: Int,
    val slowModeKey: Int = slow_mode_key,
    val slowModeText: String = slow_mode_text,
    val slowModeCircle: Int = slow_mode_circle,
    val speakFrequency: Int = speak_frequency,
)

@Serializable
data class GuildMember(
    private val tiny_id: String,
    private val join_time: Long,
    private val avatar_url: String,
    val id: String = tiny_id,
    val nickname: String,
    val avatarUrl: String = avatar_url,
    val joinTime: Long = join_time,
    val roles: List<GuildPermGroup> = emptyList()
)

@Serializable
data class GuildPermGroup(
    private val role_id: String,
    private val role_name: String,
    val id: String = role_id,
    val name: String = role_name
)

data class GuildAt(val qq: Long): MessageContent {
    override fun contentToString(): String = "[CQ:at,qq=$qq]"
}
// Cannot use, IDK why
data class GuildReply(val id: String): MessageContent {
    override fun contentToString(): String = "[CQ:reply,id=$id]"
}