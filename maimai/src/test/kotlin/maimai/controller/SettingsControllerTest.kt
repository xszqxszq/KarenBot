package xyz.xszq.bot.maimai.controller

import kotlinx.coroutines.test.runTest
import xyz.xszq.bot.maimai.database.MaimaiDatabaseTest
import xyz.xszq.bot.maimai.database.MaimaiSettingsTable
import xyz.xszq.bot.maimai.database.QQBindTable
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsControllerTest: MaimaiDatabaseTest() {
    /**
     * bind/设置查分器
     */
    @Test
    fun testUserSetSettings() = runTest {
        val harness = ControllerTest(this)
        try {
            harness.register(SettingsController(harness.maimai))

            harness.sendGroupMessage("/bind 1234567890", userId = harness.testUserId)
            assertEquals(1234567890L, QQBindTable[harness.testUserId])

            harness.sendGroupMessage("设置查分器 水鱼", userId = harness.testUserId)
            assertEquals("diving-fish", MaimaiSettingsTable[harness.testUserId, "prober"])

            harness.sendGroupMessage("设置查分器 落雪", userId = harness.testUserId)
            assertEquals("lxns", MaimaiSettingsTable[harness.testUserId, "prober"])

            harness.sendGroupMessage("设置查分器 自动", userId = harness.testUserId)
            assertEquals(null, MaimaiSettingsTable[harness.testUserId, "prober"])

            harness.sendGroupMessage("设置水鱼", userId = harness.testUserId)
            assertEquals("diving-fish", MaimaiSettingsTable[harness.testUserId, "prober"])

            harness.sendGroupMessage("水鱼", userId = harness.testUserId)
            assertEquals("diving-fish", MaimaiSettingsTable[harness.testUserId, "prober"])

            harness.sendGroupMessage("设置落雪", userId = harness.testUserId)
            assertEquals("lxns", MaimaiSettingsTable[harness.testUserId, "prober"])

            harness.sendGroupMessage("落雪", userId = harness.testUserId)
            assertEquals("lxns", MaimaiSettingsTable[harness.testUserId, "prober"])
        } finally {
            harness.close()
        }
    }

    /**
     * 兼容模式 关闭/启用
     */
    @Test
    fun testCompatibilityChange() = runTest {
        val harness = ControllerTest(this)
        try {
            harness.register(SettingsController(harness.maimai))

            harness.sendGroupMessage("兼容模式", userId = harness.testUserId)
            assertEquals("1", MaimaiSettingsTable[harness.testUserId, "text-mode"])

            harness.sendGroupMessage("兼容模式 关闭", userId = harness.testUserId)
            assertEquals("0", MaimaiSettingsTable[harness.testUserId, "text-mode"])

            harness.sendGroupMessage("取消兼容模式", userId = harness.testUserId)
            assertEquals("0", MaimaiSettingsTable[harness.testUserId, "text-mode"])

            harness.sendGroupMessage("关闭兼容模式", userId = harness.testUserId)
            assertEquals("0", MaimaiSettingsTable[harness.testUserId, "text-mode"])

            harness.sendGroupMessage("禁用兼容模式", userId = harness.testUserId)
            assertEquals("0", MaimaiSettingsTable[harness.testUserId, "text-mode"])

            harness.sendGroupMessage("打开兼容模式", userId = harness.testUserId)
            assertEquals("1", MaimaiSettingsTable[harness.testUserId, "text-mode"])

            harness.sendGroupMessage("启用兼容模式", userId = harness.testUserId)
            assertEquals("1", MaimaiSettingsTable[harness.testUserId, "text-mode"])
        } finally {
            harness.close()
        }
    }

    /**
     * 设置收藏品
     */
    @Test
    fun testUserSetCollections() = runTest {
        val harness = ControllerTest(this)
        try {
            harness.addIcon(106103, "高瀬 梨緒")
            harness.addPlate(100501, "晓将")
            harness.register(SettingsController(harness.maimai))

            harness.sendGroupMessage("设置头像 106103", userId = harness.testUserId)
            assertEquals("106103", MaimaiSettingsTable[harness.testUserId, "icon"])

            harness.sendGroupMessage("设置牌子 晓将", userId = harness.testUserId)
            assertEquals("100501", MaimaiSettingsTable[harness.testUserId, "plate"])

            harness.sendGroupMessage("设置姓名框 晓将", userId = harness.testUserId)
            assertEquals("100501", MaimaiSettingsTable[harness.testUserId, "plate"])
        } finally {
            harness.close()
        }
    }
}
