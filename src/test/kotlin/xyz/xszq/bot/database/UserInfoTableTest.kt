package xyz.xszq.bot.database

import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserInfoTableTest {
    private val database = Database.connect(
        url = "jdbc:h2:mem:user-infos;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver",
        user = "sa",
        password = ""
    )

    @BeforeTest
    fun setUp() {
        transaction(database) {
            SchemaUtils.drop(UserInfoTable)
            SchemaUtils.create(UserInfoTable)
        }
    }

    @Test
    fun shouldSaveAndLoadUser() = runTest {
        UserInfoTable.save(database, UserInfo("user-1", "小光", false))

        val loaded = users()

        assertEquals(1, loaded.size)
        assertEquals("小光", loaded["user-1"]?.username)
        assertEquals(false, loaded["user-1"]?.isBot)
    }

    @Test
    fun shouldOverwriteNickname() = runTest {
        UserInfoTable.save(
            database,
            UserInfo("user-1", "旧昵称", false)
        )
        UserInfoTable.save(
            database,
            UserInfo("user-1", "新昵称", false)
        )

        val loaded = users()

        assertEquals(1, loaded.size)
        assertEquals("新昵称", loaded.getValue("user-1").username)
    }

    @Test
    fun shouldKeepUsersSeparate() = runTest {
        UserInfoTable.save(database, UserInfo("user-1", "小光", false))
        UserInfoTable.save(database, UserInfo("user-2", "小不点", true))

        val loaded = users()

        assertEquals(2, loaded.size)
        assertTrue(loaded.getValue("user-2").isBot)
    }

    private suspend fun users(): Map<String, UserInfo> {
        val result = mutableMapOf<String, UserInfo>()
        UserInfoTable.forEach(database) { result[it.id] = it }
        return result
    }
}