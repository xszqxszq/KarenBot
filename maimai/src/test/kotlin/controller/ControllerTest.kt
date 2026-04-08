package xyz.xszq.bot.controller

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import xyz.xszq.bot.Bot
import xyz.xszq.bot.Group
import xyz.xszq.bot.Maimai
import xyz.xszq.bot.OpenAPI
import xyz.xszq.bot.PluginLoader
import xyz.xszq.bot.TencentCos
import xyz.xszq.bot.User
import xyz.xszq.bot.component.AliasesSearch
import xyz.xszq.bot.component.MaimaiData
import xyz.xszq.bot.component.MaimaiQuery
import xyz.xszq.bot.config.DatabaseConfig
import xyz.xszq.bot.config.MaimaiConfig
import xyz.xszq.bot.event.GroupMessageEvent
import xyz.xszq.bot.event.InteractionEvent
import xyz.xszq.bot.message.MessageChain
import xyz.xszq.bot.message.PlainText
import xyz.xszq.bot.payload.LocalIconInfo
import xyz.xszq.bot.payload.LocalPlateInfo
import xyz.xszq.bot.music.ChartInfo
import xyz.xszq.bot.music.GameVersion
import xyz.xszq.bot.music.MusicDifficulty
import xyz.xszq.bot.music.MusicGenre
import xyz.xszq.bot.music.MusicInfo
import xyz.xszq.bot.music.MusicType
import xyz.xszq.bot.music.Notes
import xyz.xszq.bot.subscribe.SubscribeManager
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ControllerTest(
    private val scope: TestScope
) {
    private val dispatcher = StandardTestDispatcher(scope.testScheduler)
    private val subscribes = SubscribeManager(dispatcher)
    private val api = mockk<OpenAPI>(relaxed = true)
    private val cos = mockk<TencentCos>(relaxed = true)
    private val pluginLoader = mockk<PluginLoader>(relaxed = true)
    private var eventIndex = 0

    val bot = Bot(api, cos, pluginLoader)
    val maimai = Maimai().apply {
        plugin = "maimai-test"
        this.pluginLoader = this@ControllerTest.pluginLoader
        config = MaimaiConfig(
            database = DatabaseConfig(
                url = "jdbc:h2:mem:maimai;MODE=MySQL;DB_CLOSE_DELAY=-1",
                driver = "org.h2.Driver",
                username = "",
                password = "",
            ),
            tokens = mapOf("assets-jacket" to "https://example.com"),
            tips = emptyList(),
        )
        maimaiData = MaimaiData().also {
            it.newestVersion = GameVersion(1, "maimai", 1)
        }
        aliases = mockk<AliasesSearch>(relaxed = true)
        query = mockk<MaimaiQuery>(relaxed = true)
        api = mockk<ApiController>(relaxed = true)
        backends = emptyList()
    }

    val testUserId = newOpenId()
    val testGroupId = newOpenId()

    init {
        every { pluginLoader.subscribes } returns subscribes
        every { pluginLoader.bot } returns bot
    }

    suspend fun register(vararg controllers: Controller) {
        controllers.forEach { it.setRoute() }
        scope.advanceUntilIdle()
    }

    suspend fun sendGroupMessage(
        text: String,
        userId: String = testUserId,
        groupId: String = testGroupId,
        eventId: String = "",
    ): GroupMessageEvent {
        val event = GroupMessageEvent(
            bot = bot,
            eventId = eventId,
            id = nextId("message"),
            message = MessageChain(PlainText(text)),
            sender = User(bot, userId),
            group = Group(bot, groupId),
        )
        subscribes.handle(event)
        scope.advanceUntilIdle()
        return event
    }

    suspend fun tapButton(
        button: String,
        data: String,
        userId: String = testUserId,
        eventId: String = "interaction-event",
    ): InteractionEvent {
        val event = InteractionEvent(
            bot = bot,
            eventId = eventId,
            id = nextId("interaction"),
            data = data,
            button = button,
            sender = User(bot, userId),
        )
        subscribes.handle(event)
        scope.advanceUntilIdle()
        return event
    }

    fun addIcon(id: Int, name: String, filename: String = "$id.png") {
        maimai.maimaiData.icons[id] = LocalIconInfo(id, filename, name, "普通", "")
    }

    fun addPlate(id: Int, name: String, filename: String = "$id.png") {
        maimai.maimaiData.plates[id] = LocalPlateInfo(id, filename, name, "通常", "", emptyList(), emptyList())
    }

    fun addMusic(id: Int, name: String = "Song$id"): MusicInfo {
        val music = MusicInfo(
            id = id,
            name = name,
            type = MusicType.Standard,
            rights = "",
            artist = "Artist",
            genre = MusicGenre.Original,
            bpm = 180,
            version = maimai.maimaiData.newestVersion,
            isNew = true,
        )
        music.charts = listOf(
            ChartInfo(music, MusicDifficulty.Basic, "1", 1.0, Notes(), "A"),
            ChartInfo(music, MusicDifficulty.Advanced, "2", 2.0, Notes(), "B"),
            ChartInfo(music, MusicDifficulty.Expert, "10", 10.0, Notes(), "C"),
            ChartInfo(music, MusicDifficulty.Master, "12", 12.0, Notes(), "D"),
        )
        maimai.maimaiData.musics[id] = music
        return music
    }

    fun mockAliasSearch(vararg musics: MusicInfo) {
        coEvery { maimai.aliases.search(any()) } returns musics.toList()
    }

    fun close() {
        maimai.scope.cancel()
    }

    private fun nextId(prefix: String): String {
        eventIndex += 1
        return "$prefix-$eventIndex"
    }

    companion object {
        fun newOpenId() = UUID.randomUUID().toString()
            .replace("-", "").uppercase()
    }
}
