package xyz.xszq.bot.controller

import kotlinx.coroutines.test.runTest
import xyz.xszq.bot.controller.ControllerTest.Companion.newOpenId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import xyz.xszq.bot.database.MaimaiDatabaseTest
import xyz.xszq.bot.database.MusicAliasesTable
import xyz.xszq.bot.database.MusicAliasesVoteTable

class MusicControllerTest: MaimaiDatabaseTest() {
    /**
     * 添加别名
     */
    @Test
    fun testAliasVote() = runTest {
        val harness = ControllerTest(this)
        try {
            val music = harness.addMusic(10001, "Song1")
            harness.mockAliasSearch(music)
            harness.register(MusicController(harness.maimai))

            val user1 = newOpenId()
            val user2 = newOpenId()
            val user3 = newOpenId()

            harness.sendGroupMessage("添加别名 10001 测试别名", userId = user1)
            assertEquals(-2, MusicAliasesTable[music, "测试别名"])
            assertTrue(MusicAliasesVoteTable[music, "测试别名", user1])

            harness.sendGroupMessage("添加别名 10001 测试别名", userId = user2)
            assertEquals(-1, MusicAliasesTable[music, "测试别名"])
            assertTrue(MusicAliasesVoteTable[music, "测试别名", user2])

            harness.sendGroupMessage("添加别名 10001 测试别名", userId = user3)
            assertEquals(0, MusicAliasesTable[music, "测试别名"])
            assertTrue(MusicAliasesVoteTable[music, "测试别名", user3])

            assertEquals(MusicAliasesTable[music].count(), 1)
        } finally {
            harness.close()
        }
    }
}
