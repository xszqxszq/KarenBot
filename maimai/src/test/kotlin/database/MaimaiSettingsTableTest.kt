package xyz.xszq.bot.database

import kotlinx.coroutines.test.runTest
import xyz.xszq.bot.controller.ControllerTest.Companion.newOpenId
import xyz.xszq.bot.music.PlayerSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MaimaiSettingsTableTest: MaimaiDatabaseTest() {
    @Test
    fun testUserSettings() = runTest {
        val testUserId = newOpenId()
        MaimaiSettingsTable[testUserId, "icon"] = "12"
        MaimaiSettingsTable[testUserId, "plate"] = ""

        assertEquals("12", MaimaiSettingsTable[testUserId, "icon"])
        assertNull(MaimaiSettingsTable[testUserId, "plate"])
        assertEquals(PlayerSettings(avatar = 12, plate = null), MaimaiSettingsTable.settings(testUserId))
    }
}
