package xyz.xszq.bot.database

import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroupCommandSettingsTest {
    private val database = Database.connect(
        url = "jdbc:h2:mem:command;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver",
        user = "sa",
        password = ""
    )

    @BeforeTest
    fun setUp() = runTest {
        newSuspendedTransaction(db = database) {
            SchemaUtils.create(GroupCommandSettings)
        }
    }

    @Test
    fun defaultEnabled() = runTest {
        assertTrue(GroupCommandSettings.isEnabled("g-default", "random.blonde.auto"))
    }

    @Test
    fun setEnabled() = runTest {
        GroupCommandSettings.setEnabled("g-switch", "random.blonde.auto", false)
        assertFalse(GroupCommandSettings.isEnabled("g-switch", "random.blonde.auto"))
        GroupCommandSettings.setEnabled("g-switch", "random.blonde.auto", true)
        assertTrue(GroupCommandSettings.isEnabled("g-switch", "random.blonde.auto"))
    }

    @Test
    fun setCustomKey() = runTest {
        GroupCommandSettings["g-key", "random.blonde.auto", "cooldown"] = "5000"
        assertEquals("5000", GroupCommandSettings["g-key", "random.blonde.auto", "cooldown"])
    }

    @Test
    fun isolatedByGroupAndCommand() = runTest {
        GroupCommandSettings.setEnabled("g1", "random.blonde.auto", false)
        assertTrue(GroupCommandSettings.isEnabled("g2", "random.blonde.auto"))
        assertTrue(GroupCommandSettings.isEnabled("g1", "maimai.guess"))
    }
}
