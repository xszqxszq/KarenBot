package xyz.xszq.bot.maimai.controller

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import xyz.xszq.bot.maimai.database.Arcade
import xyz.xszq.bot.maimai.database.ArcadeGroup
import xyz.xszq.bot.maimai.database.ArcadeGroupBind
import xyz.xszq.bot.maimai.database.MaimaiDatabaseTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class QueueControllerTest: MaimaiDatabaseTest() {
    /**
     * 排卡管理 添加机厅/删除机厅/添加别名/删除别名
     */
    @Test
    fun testArcadeManagement() = runTest {
        val harness = ControllerTest(this)
        try {
            harness.register(QueueController(harness.maimai))

            harness.sendGroupMessage("排卡管理 添加机厅 测试")
            assertNotNull(ArcadeGroupBind.findArcade(harness.testGroupId, "测试"))

            harness.sendGroupMessage("排卡管理 添加别名 测试 test")
            assertEquals(listOf("测试", "test"), ArcadeGroupBind.aliases(harness.testGroupId, "测试"))

            harness.sendGroupMessage("排卡管理 删除别名 测试 test")
            assertEquals(listOf("测试"), ArcadeGroupBind.aliases(harness.testGroupId, "测试"))

            harness.sendGroupMessage("排卡管理 删除机厅 测试")
            val remaining = newSuspendedTransaction { Arcade.all().count() }
            assertEquals(0L, remaining)


            newSuspendedTransaction {
                ArcadeGroup.new {
                    name = "shared"
                }
            }
            harness.sendGroupMessage("排卡管理 添加分组 shared")
            assertEquals("shared", ArcadeGroupBind.find(harness.testGroupId)?.name)
        } finally {
            harness.close()
        }
    }

    /**
     * (机厅名)(人数/+人数/-人数)
     */
    @Test
    fun testArcadePeopleUpdate() = runTest {
        val harness = ControllerTest(this)
        try {
            harness.register(QueueController(harness.maimai))
            harness.sendGroupMessage("排卡管理 添加机厅 测试")
            harness.sendGroupMessage("排卡管理 添加别名 测试 test")

            harness.sendGroupMessage("test+3")
            assertEquals(3, ArcadeGroupBind.findArcade(harness.testGroupId, "test")?.value)
            harness.sendGroupMessage("test-2")
            assertEquals(1, ArcadeGroupBind.findArcade(harness.testGroupId, "test")?.value)
            harness.sendGroupMessage("test10")
            assertEquals(10, ArcadeGroupBind.findArcade(harness.testGroupId, "test")?.value)
        } finally {
            harness.close()
        }
    }

    /**
     * 隔日人数自动清空
     */
    @Test
    fun testAutoClear() = runTest {
        val harness = ControllerTest(this)
        try {
            harness.register(QueueController(harness.maimai))
            harness.sendGroupMessage("排卡管理 添加机厅 测试")

            newSuspendedTransaction {
                val group = ArcadeGroup.findById(ArcadeGroupBind.findById(harness.testGroupId)!!.group)!!
                val arcade = group.find("测试")!!
                arcade.value = 10
                arcade.modified = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            }
            harness.sendGroupMessage("/j")
            assertEquals(10, ArcadeGroupBind.findArcade(harness.testGroupId, "测试")?.value)

            newSuspendedTransaction {
                val group = ArcadeGroup.findById(ArcadeGroupBind.findById(harness.testGroupId)!!.group)!!
                val arcade = group.find("测试")!!
                arcade.value = 5
                arcade.modified = LocalDateTime(2024, 1, 1, 0, 0)
            }
            harness.sendGroupMessage("/j")
            assertEquals(0, ArcadeGroupBind.findArcade(harness.testGroupId, "测试")?.value)

            newSuspendedTransaction {
                val group = ArcadeGroup.findById(ArcadeGroupBind.findById(harness.testGroupId)!!.group)!!
                val arcade = group.find("测试")!!
                arcade.value = 10
                // 边界测试，假设上次更新是前一天23:59:59
                arcade.modified = LocalDateTime(
                    Clock.System.now()
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .date.minus(DatePeriod(days = 1)),
                    LocalTime(23, 59, 59)
                )
            }
            harness.sendGroupMessage("/j")
            assertEquals(0, ArcadeGroupBind.findArcade(harness.testGroupId, "测试")?.value)

        } finally {
            harness.close()
        }
    }
}
