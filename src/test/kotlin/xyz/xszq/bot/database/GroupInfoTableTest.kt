package xyz.xszq.bot.database

import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.xszq.bot.MemberRole
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GroupInfoTableTest {
    private val database = Database.connect(
        url = "jdbc:h2:mem:group-infos;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver",
        user = "sa",
        password = ""
    )

    @BeforeTest
    fun setUp() {
        transaction(database) {
            SchemaUtils.drop(GroupInfoTable)
            SchemaUtils.create(GroupInfoTable)
        }
    }

    @Test
    fun shouldSaveAndLoadGroupInfo() = runTest {
        val info = groupInfo(id = "group-1")
        GroupInfoTable.save(database, info)

        val loaded = mutableListOf<GroupInfo>()
        GroupInfoTable.forEach(database) { loaded += it }

        assertEquals(listOf(info), loaded)
    }

    @Test
    fun shouldOverwriteExistingGroupInfo() = runTest {
        GroupInfoTable.save(database, groupInfo(id = "group-1"))
        GroupInfoTable.save(
            database,
            groupInfo(id = "group-1", name = "改过的群名")
        )

        val loaded = mutableListOf<GroupInfo>()
        GroupInfoTable.forEach(database) { loaded += it }

        assertEquals(1, loaded.size)
        assertEquals("改过的群名", loaded.single().name)
    }

    @Test
    fun shouldKeepGroupsSeparate() = runTest {
        GroupInfoTable.save(database, groupInfo(id = "group-1", name = "群一"))
        GroupInfoTable.save(database, groupInfo(id = "group-2", name = "群二"))

        val loaded = mutableMapOf<String, String>()
        GroupInfoTable.forEach(database) { loaded[it.id] = it.name }

        assertEquals(
            mapOf("group-1" to "群一", "group-2" to "群二"),
            loaded
        )
    }

    private fun groupInfo(
        id: String,
        name: String = "bot测试"
    ) = GroupInfo(
        id = id,
        name = name,
        description = "每周共读一本好书",
        category = "文化",
        tags = listOf("阅读", "文学"),
        memberCount = 256,
        botJoinedAt = "2024-05-09T20:36:28+08:00",
        allowPush = true,
        receiveMessageSetting = "all",
        botRole = MemberRole.Admin,
        muteMode = "schedule",
        fetchedAt = 1_700_000_000_000L
    )
}