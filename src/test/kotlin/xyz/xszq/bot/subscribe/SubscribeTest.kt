package xyz.xszq.bot.subscribe

import kotlinx.coroutines.test.runTest
import xyz.xszq.bot.interactionEvent
import xyz.xszq.bot.messageEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubscribeTest {
    @Test
    fun shouldMatchExactCommandAfterTrimmingPrefix() = runTest {
        var matched = false
        val subscribe = EqualsTo(parent = "/mai", forceParent = true, text = "help") { matched = true }

        subscribe.handler(messageEvent("/mai /help"))

        assertTrue(matched)
    }

    @Test
    fun shouldRespectForceParent() = runTest {
        var matched = false
        val subscribe = EqualsTo(parent = "/mai", forceParent = true, text = "help") { matched = true }

        subscribe.handler(messageEvent("/help"))

        assertFalse(matched)
    }

    @Test
    fun shouldExtractTrailingArgument() = runTest {
        var argument: String? = null
        val subscribe = StartsWith(prefix = "debug") { argument = it }

        subscribe.handler(messageEvent("/debug user-1"))

        assertEquals("user-1", argument)
    }

    @Test
    fun shouldMoveSuffixToArgument() = runTest {
        var argument: String? = null
        val subscribe = CommandEndsWith(suffix = "分") { argument = it }

        subscribe.handler(messageEvent("100分 extra"))

        assertEquals("100 extra", argument)
    }

    @Test
    fun shouldMatchButtonId() = runTest {
        var matched = false
        val subscribe = ButtonSubscribe("confirm") { matched = true }

        subscribe.handler(interactionEvent(button = "confirm"))

        assertTrue(matched)
    }
}
