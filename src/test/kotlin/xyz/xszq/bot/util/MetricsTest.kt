package xyz.xszq.bot.util

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MetricsTest {
    @Test
    fun shouldKeepTimerResult() = runTest {
        val result = Metrics.time("karenbot.test.result") { 42 }

        assertEquals(42, result)
        assertTrue("outcome=\"success\"" in Metrics.scrape())
    }

    @Test
    fun shouldRethrowOriginalTimerException() = runTest {
        val error = IllegalStateException("failed")
        val actual = assertFailsWith<IllegalStateException> {
            Metrics.time("karenbot.test.error") { throw error }
        }

        assertEquals(error, actual)
        assertTrue("outcome=\"error\"" in Metrics.scrape())
    }

    @Test
    fun shouldAllowTimerAndCounterWithSameName() = runTest {
        val result = Metrics.time("karenbot.test.mixed") { 42 }

        Metrics.count("karenbot.test.mixed", "outcome" to "success")
        assertEquals(42, result)
        val scrape = Metrics.scrape()
        assertTrue("karenbot_test_mixed_seconds_count" in scrape)
        assertTrue(scrape.lineSequence().any { it.startsWith("karenbot_test_mixed_total") })
    }

    @Test
    fun shouldNotDuplicateTotalSuffix() = runTest {
        Metrics.time("karenbot.test.prefixed") { 42 }
        Metrics.count("karenbot.test.prefixed.total", "outcome" to "success")

        val scrape = Metrics.scrape()
        assertTrue("karenbot_test_prefixed_seconds_count" in scrape)
        assertTrue(scrape.lineSequence().any { it.startsWith("karenbot_test_prefixed_total") })
        assertTrue("_total_total" !in scrape)
    }
}