package xyz.xszq.bot.controller

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.xszq.bot.database.GuessGameStatus
import xyz.xszq.bot.database.GuessGameTable
import xyz.xszq.bot.database.MaimaiDatabaseTest
import xyz.xszq.bot.database.MaimaiSettingsTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GuessControllerTest: MaimaiDatabaseTest() {
    /**
     * 猜歌/舞萌开字母/不玩了
     */
    @Test
    fun testGuessStartAndEnd() = runTest {
        val harness = ControllerTest(this)
        try {
            harness.addMusic(10001, "Song1")
            harness.addMusic(10002, "Song2")
            harness.register(GuessController(harness.maimai))

            harness.sendGroupMessage("猜歌", userId = harness.testUserId, groupId = harness.testGroupId)
            assertEquals("classical", currentGuessType(harness.testGroupId))
            harness.sendGroupMessage("不玩了", userId = harness.testUserId, groupId = harness.testGroupId)
            delay(300L)
            assertNull(currentGuessTypeOrNull(harness.testGroupId))

            harness.sendGroupMessage("舞萌开字母", userId = harness.testUserId, groupId = harness.testGroupId)
            assertEquals("opening", currentGuessType(harness.testGroupId))
            harness.sendGroupMessage("不玩了", userId = harness.testUserId, groupId = harness.testGroupId)
            delay(300L)
            assertNull(currentGuessTypeOrNull(harness.testGroupId))

        } finally {
            harness.close()
        }
    }

    /**
     * 舞萌开字母是否会保存字符
     */
    @Test
    fun testOpeningSaveLetters() = runTest {
        val harness = ControllerTest(this)
        var status: GuessGameStatus
        try {
            harness.addMusic(10001, "Song1")
            harness.addMusic(10002, "Song2")
            harness.register(GuessController(harness.maimai))

            harness.sendGroupMessage("舞萌开字母", userId = harness.testUserId, groupId = harness.testGroupId)
            assertEquals("opening", currentGuessType(harness.testGroupId))

            harness.sendGroupMessage("开字母 s", userId = harness.testUserId, groupId = harness.testGroupId)

            status = currentGuessStatus(harness.testGroupId) as GuessGameStatus.Opening
            assertEquals(listOf('s'), status.opened)
            assertEquals(2, status.musics.size)

            harness.sendGroupMessage("开字母 b", userId = harness.testUserId, groupId = harness.testGroupId)
            harness.sendGroupMessage("开字母 g", userId = harness.testUserId, groupId = harness.testGroupId)
            harness.sendGroupMessage("开字母 a", userId = harness.testUserId, groupId = harness.testGroupId)

            status = currentGuessStatus(harness.testGroupId) as GuessGameStatus.Opening
            assertEquals(listOf('s', 'b', 'g', 'a'), status.opened)
        } finally {
            harness.close()
        }
    }

    /**
     * 舞萌开字母是否会保存已开歌曲
     */
    @Test
    fun testOpeningSaveMusics() = runTest {
        val harness = ControllerTest(this)
        try {
            val song1 = harness.addMusic(10001, "Song1")
            val song2 = harness.addMusic(10002, "Song2")
            harness.mockAliasSearch(song1, song2)
            harness.register(GuessController(harness.maimai))

            harness.sendGroupMessage("出你字母", userId = harness.testUserId, groupId = harness.testGroupId)
            assertEquals("opening", currentGuessType(harness.testGroupId))

            harness.sendGroupMessage("开歌 Song1", userId = harness.testUserId, groupId = harness.testGroupId)

            val status = currentGuessStatus(harness.testGroupId) as GuessGameStatus.Opening
            assertEquals(2, status.musics.size)
            assertNotNull(status.musics.firstOrNull { it.second })
        } finally {
            harness.close()
        }
    }

    /**
     * 猜歌设置 关闭/打开
     */
    @Test
    fun testAdminEnableAndDisable() = runTest {
        val harness = ControllerTest(this)
        try {
            harness.register(GuessController(harness.maimai))

            harness.tapButton("admin/guess", "0,${harness.testGroupId}", userId = harness.testUserId)
            assertEquals("false", MaimaiSettingsTable[harness.testGroupId, "guess"])

            harness.tapButton("admin/guess", "1,${harness.testGroupId}", userId = harness.testUserId)
            assertEquals("true", MaimaiSettingsTable[harness.testGroupId, "guess"])
        } finally {
            harness.close()
        }
    }

    private fun currentGuessType(contextId: String) = transaction {
        val row = GuessGameTable.selectAll().where { GuessGameTable.id eq contextId }.single()
        row[GuessGameTable.type]
    }

    private fun currentGuessTypeOrNull(contextId: String) = transaction {
        GuessGameTable.selectAll().where { GuessGameTable.id eq contextId }.singleOrNull()?.get(GuessGameTable.type)
    }

    private fun currentGuessStatus(contextId: String) = transaction {
        GuessGameTable.selectAll().where { GuessGameTable.id eq contextId }.single()[GuessGameTable.status]
    }

}
