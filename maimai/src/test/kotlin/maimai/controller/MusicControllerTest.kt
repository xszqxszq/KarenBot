package xyz.xszq.bot.maimai.controller

import kotlinx.coroutines.test.runTest
import xyz.xszq.bot.maimai.controller.ControllerTest.Companion.newOpenId
import xyz.xszq.bot.maimai.database.MaimaiDatabaseTest
import xyz.xszq.bot.maimai.database.MaimaiMusicAliasesTable
import xyz.xszq.bot.maimai.database.MaimaiMusicAliasesVoteTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
            assertEquals(-2, MaimaiMusicAliasesTable[music, "测试别名"])
            assertTrue(MaimaiMusicAliasesVoteTable[music, "测试别名", user1])

            harness.sendGroupMessage("添加别名 10001 测试别名", userId = user2)
            assertEquals(-1, MaimaiMusicAliasesTable[music, "测试别名"])
            assertTrue(MaimaiMusicAliasesVoteTable[music, "测试别名", user2])

            harness.sendGroupMessage("添加别名 10001 测试别名", userId = user3)
            assertEquals(0, MaimaiMusicAliasesTable[music, "测试别名"])
            assertTrue(MaimaiMusicAliasesVoteTable[music, "测试别名", user3])

            assertEquals(MaimaiMusicAliasesTable[music].count(), 1)
        } finally {
            harness.close()
        }
    }
}
