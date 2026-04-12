package xyz.xszq.bot.maimai.database

import kotlinx.coroutines.test.runTest
import xyz.xszq.bot.maimai.music.GameVersion
import xyz.xszq.bot.maimai.music.MusicGenre
import xyz.xszq.bot.maimai.music.MusicInfo
import xyz.xszq.bot.maimai.music.MusicType
import kotlin.test.Test
import kotlin.test.assertEquals

class MusicAliasesTableTest: MaimaiDatabaseTest() {
    @Test
    fun testVote() = runTest {
        val music = musicInfo(10001)

        MusicAliasesTable.vote(music, "测试别名")
        assertEquals(-2, MusicAliasesTable[music, "测试别名"])

        MusicAliasesTable.vote(music, "测试别名")
        assertEquals(-1, MusicAliasesTable[music, "测试别名"])

        MusicAliasesTable.add(music, "测试别名")

        assertEquals(0, MusicAliasesTable[music, "测试别名"])
        assertEquals(listOf("测试别名" to 0), MusicAliasesTable[music])
        assertEquals(listOf(10001), MusicAliasesTable.exact("测试别名"))
    }

    private fun musicInfo(id: Int) = MusicInfo(
        id = id,
        name = "Test",
        type = MusicType.Deluxe,
        rights = "",
        artist = "Artist",
        genre = MusicGenre.Original,
        bpm = 120,
        version = GameVersion(1, "maimai", 1),
        isNew = true,
    )
}
