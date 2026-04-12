package xyz.xszq.bot.maimai.database

import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import xyz.xszq.bot.maimai.controller.ControllerTest.Companion.newOpenId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ArcadeGroupBindTest: MaimaiDatabaseTest() {
    @Test
    fun testArcadeGroupBind() = runTest {
        val testGroupId = newOpenId()
        val created = ArcadeGroupBind.group(testGroupId)
        assertNotNull(created)
        assertEquals(created.id.value, ArcadeGroupBind.find(testGroupId)?.id?.value)

        val target = newSuspendedTransaction {
            ArcadeGroup.new {
                name = "shared"
            }
        }

        ArcadeGroupBind.bind(testGroupId, target)

        assertEquals("shared", ArcadeGroupBind.find(testGroupId)?.name)
    }
}
