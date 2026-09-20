package xyz.xszq.bot.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RateLimiterTest {
    @Test
    fun shouldAllowUpToQuota() {
        val now = 0L
        val limiter = RateLimiter(3, now = { now })
        assertTrue(limiter.tryAcquire())
        assertTrue(limiter.tryAcquire())
        assertTrue(limiter.tryAcquire())
        assertFalse(limiter.tryAcquire())
    }

    @Test
    fun shouldReleaseQuotaAfterWindow() {
        var now = 0L
        val limiter = RateLimiter(2, now = { now })
        assertTrue(limiter.tryAcquire())
        assertTrue(limiter.tryAcquire())
        assertFalse(limiter.tryAcquire())
        now = 59_000L
        assertFalse(limiter.tryAcquire())
        now = 60_000L
        assertTrue(limiter.tryAcquire())
        assertTrue(limiter.tryAcquire())
        assertFalse(limiter.tryAcquire())
    }

    @Test
    fun shouldSlideWindowPerCall() {
        var now = 0L
        val limiter = RateLimiter(1, now = { now })
        assertTrue(limiter.tryAcquire())
        now = 30_000L
        assertFalse(limiter.tryAcquire())
        now = 60_000L
        assertTrue(limiter.tryAcquire())
        now = 90_000L
        assertFalse(limiter.tryAcquire())
        now = 120_000L
        assertTrue(limiter.tryAcquire())
    }
}