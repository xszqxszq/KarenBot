package xyz.xszq.bot

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.xszq.bot.database.*
import xyz.xszq.bot.payload.BotStateResponse
import xyz.xszq.bot.payload.GroupInfoResponse
import xyz.xszq.bot.service.OpenAPI
import xyz.xszq.bot.service.TencentCOS
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class InfoCacheTest {
    private val database = Database.connect(
        url = "jdbc:h2:mem:info-cache;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver",
        user = "sa",
        password = ""
    )

    @BeforeTest
    fun setUp() {
        transaction(database) {
            SchemaUtils.drop(GroupInfoTable, GroupMemberTable, UserInfoTable)
            SchemaUtils.create(GroupInfoTable, GroupMemberTable, UserInfoTable)
        }
    }

    @Test
    fun shouldRecordMemberAndUserOnVisit() = runTest {
        val now = 1_700_000_000_000L
        val api = mockk<OpenAPI>(relaxed = true)
        val bot = Bot(api, mockk<TencentCOS>(relaxed = true))
        val cache = InfoCache(api, bot, database, backgroundScope) { now }

        bot.group("group-1").fetchedAt = now

        val group = cache.visit(
            group = "group-1",
            member = "member-1",
            username = "小光",
            isBot = false,
            role = MemberRole.Member
        )

        assertSame(group, bot.group("group-1"))
        assertEquals(MemberRole.Member, group.members["member-1"])
        assertEquals("小光", bot.users.getValue("member-1").username)
        coVerify(exactly = 0) { api.getGroupInfo(any()) }
    }

    @Test
    fun shouldRefreshGroupInfoWhenStale() = runTest {
        val now = 1_700_000_000_000L
        val api = mockk<OpenAPI>(relaxed = true)
        coEvery { api.getGroupInfo("group-1") } returns GroupInfoResponse(
            group = "group-1",
            name = "bot测试",
            description = "每周共读一本好书",
            category = "文化",
            tags = listOf("阅读"),
            memberCount = 21
        )
        coEvery { api.getBotState("group-1") } returns BotStateResponse(
            member = "bot-1",
            joinedAt = "2024-05-09T20:36:28+08:00",
            allowPush = true,
            recvMsgSetting = "all",
            role = "admin"
        )
        val bot = Bot(api, mockk<TencentCOS>(relaxed = true))
        val cache = InfoCache(api, bot, database, backgroundScope) { now }

        val group = cache.visit(
            group = "group-1",
            member = "member-1",
            username = "小光",
            isBot = false,
            role = MemberRole.Member
        )
        assertEquals("", group.name)
        runCurrent()

        assertEquals("bot测试", group.name)
        assertEquals("每周共读一本好书", group.description)
        assertEquals("文化", group.category)
        assertEquals(listOf("阅读"), group.tags)
        assertEquals(21, group.memberCount)
        assertEquals("2024-05-09T20:36:28+08:00", group.botJoinedAt)
        assertEquals(true, group.allowPush)
        assertEquals("all", group.receiveMessageSetting)
        assertEquals(MemberRole.Admin, group.botRole)
        assertEquals(now, group.fetchedAt)
    }

    @Test
    fun shouldKeepInfoWhenFetchFails() = runTest {
        val now = 1_700_000_000_000L
        val api = mockk<OpenAPI>(relaxed = true)
        coEvery { api.getGroupInfo("group-1") } returns null
        val bot = Bot(api, mockk<TencentCOS>(relaxed = true))
        val cache = InfoCache(api, bot, database, backgroundScope) { now }

        val group = cache.visit(
            group = "group-1",
            member = "member-1",
            username = "小光",
            isBot = false,
            role = MemberRole.Member
        )
        runCurrent()

        assertEquals(0L, group.fetchedAt)
        assertTrue(group.nextRefreshAt > now)
        assertEquals(MemberRole.Member, group.members["member-1"])
    }

    @Test
    fun shouldRemoveMember() = runTest {
        val now = 1_700_000_000_000L
        val api = mockk<OpenAPI>(relaxed = true)
        val bot = Bot(api, mockk<TencentCOS>(relaxed = true))
        val cache = InfoCache(api, bot, database, backgroundScope) { now }
        bot.group("group-1").fetchedAt = now

        val group = cache.visit(
            group = "group-1",
            member = "member-1",
            username = "小光",
            isBot = false,
            role = MemberRole.Member
        )
        cache.remove("group-1", "member-1")

        assertNull(group.members["member-1"])
        assertEquals("小光", bot.users.getValue("member-1").username)
    }

    @Test
    fun shouldLoadFromDatabase() = runTest {
        val now = 1_700_000_000_000L
        GroupInfoTable.save(database, GroupInfo(
            id = "group-1",
            name = "bot测试",
            description = "",
            category = "文化",
            tags = listOf("阅读"),
            memberCount = 21,
            botJoinedAt = "2024-05-09T20:36:28+08:00",
            allowPush = true,
            receiveMessageSetting = "all",
            botRole = MemberRole.Admin,
            muteMode = "none",
            fetchedAt = now
        ))
        UserInfoTable.save(database, UserInfo("member-1", "小光", false))
        GroupMemberTable.add(database, "group-1", "member-1")
        val api = mockk<OpenAPI>(relaxed = true)
        val bot = Bot(api, mockk<TencentCOS>(relaxed = true))
        val cache = InfoCache(api, bot, database, backgroundScope) { now }

        cache.load()

        val group = bot.group("group-1")
        assertEquals("bot测试", group.name)
        assertEquals(listOf("阅读"), group.tags)
        assertEquals(now, group.fetchedAt)
        assertEquals(MemberRole.Member, group.members["member-1"])
        assertEquals("小光", bot.users.getValue("member-1").username)
        val member = group.memberList().single()
        assertEquals("member-1", member.id)
        assertEquals("小光", member.username)
    }

    @Test
    fun shouldNotQueryMuteState() = runTest {
        val now = 1_700_000_000_000L
        val api = mockk<OpenAPI>(relaxed = true)
        coEvery { api.getGroupInfo("group-1") } returns GroupInfoResponse(
            name = "bot测试"
        )
        coEvery { api.getBotState("group-1") } returns BotStateResponse(
            role = "admin"
        )
        val bot = Bot(api, mockk<TencentCOS>(relaxed = true))
        val cache = InfoCache(api, bot, database, backgroundScope) { now }

        cache.visit(
            group = "group-1",
            member = "member-1",
            username = "小光",
            isBot = false,
            role = MemberRole.Member
        )
        runCurrent()

        coVerify(exactly = 0) { api.getRestrictChatSetting(any()) }
    }

    @Test
    fun shouldRunWhenInfoReadyAfterRefresh() = runTest {
        val now = 1_700_000_000_000L
        val api = mockk<OpenAPI>(relaxed = true)
        coEvery { api.getGroupInfo("group-1") } returns GroupInfoResponse(
            name = "bot测试"
        )
        val bot = Bot(api, mockk<TencentCOS>(relaxed = true))
        val cache = InfoCache(api, bot, database, backgroundScope) { now }

        var logged: String? = null
        val group = cache.visit(
            group = "group-1",
            member = "member-1",
            username = "小光",
            isBot = false,
            role = MemberRole.Member
        )
        group.whenInfoReady { logged = it.logPrefix }
        assertNull(logged)
        runCurrent()

        assertEquals("bot测试[group-1]", logged)
    }

    @Test
    fun shouldRunWhenInfoReadyWhenFetchFails() = runTest {
        val now = 1_700_000_000_000L
        val api = mockk<OpenAPI>(relaxed = true)
        coEvery { api.getGroupInfo("group-1") } returns null
        val bot = Bot(api, mockk<TencentCOS>(relaxed = true))
        val cache = InfoCache(api, bot, database, backgroundScope) { now }

        var logged: String? = null
        val group = cache.visit(
            group = "group-1",
            member = "member-1",
            username = "小光",
            isBot = false,
            role = MemberRole.Member
        )
        group.whenInfoReady { logged = it.logPrefix }
        assertNull(logged)
        runCurrent()

        assertEquals("[group-1]", logged)
    }

    @Test
    fun shouldRunWhenInfoReadyImmediatelyWhenCached() = runTest {
        val now = 1_700_000_000_000L
        val api = mockk<OpenAPI>(relaxed = true)
        val bot = Bot(api, mockk<TencentCOS>(relaxed = true))
        val cache = InfoCache(api, bot, database, backgroundScope) { now }
        bot.group("group-1").apply {
            name = "bot测试"
            fetchedAt = now
        }

        val group = cache.visit(
            group = "group-1",
            member = "member-1",
            username = "小光",
            isBot = false,
            role = MemberRole.Member
        )
        var logged: String? = null
        group.whenInfoReady { logged = it.logPrefix }

        assertEquals("bot测试[group-1]", logged)
    }
}