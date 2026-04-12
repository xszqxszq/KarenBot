package xyz.xszq.bot.subscribe

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import xyz.xszq.bot.messageEvent
import kotlin.test.Test
import kotlin.test.assertEquals

class SubscribeManagerTest {
    @Test
    fun shouldTriggerPluginAndTempSubscribes() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val manager = SubscribeManager(dispatcher)
        val called = mutableListOf<String>()

        manager.subscribe("plugin-a", Always { called += "plugin:${text}" })
        manager.always("temp") { called += "temp:${text}" }

        manager.handle(messageEvent("hello"))

        assertEquals(listOf("temp:hello", "plugin:hello"), called)
    }

    @Test
    fun shouldRemoveHandlersAfterStopAndUnsubscribe() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val manager = SubscribeManager(dispatcher)
        var called = 0

        manager.subscribe("plugin-a", Always { called += 1 })
        manager.always("temp") { called += 1 }
        manager.stop("temp")
        manager.unsubscribe("plugin-a")

        manager.handle(messageEvent("hello"))

        assertEquals(0, called)
    }

    @Test
    fun shouldPreferStartsWithOverCommandEndsWithInSameDomain() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val manager = SubscribeManager(dispatcher)
        val called = mutableListOf<String>()

        manager.subscribe("plugin-a", StartsWith(prefix = "id") { called += "start:$it" }, "rhythm", "mai")
        manager.subscribe("plugin-a", CommandEndsWith(suffix = "50") { called += "end:$it" }, "rhythm", "mai")

        manager.handle(messageEvent("id11450"))

        assertEquals(listOf("start:11450"), called)
    }

    @Test
    fun shouldPreferLongerStartsWithInSameDomain() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val manager = SubscribeManager(dispatcher)
        val called = mutableListOf<String>()

        manager.subscribe("plugin-a", StartsWith(prefix = "设置") { called += "generic:$it" }, "rhythm", "mai")
        manager.subscribe("plugin-a", StartsWith(prefix = "设置头像") { called += "avatar:$it" }, "rhythm", "mai")

        manager.handle(messageEvent("设置头像 106103"))

        assertEquals(listOf("avatar:106103"), called)
    }

    @Test
    fun shouldPreferStartsWithOverSuffixForSpecificCommand() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val manager = SubscribeManager(dispatcher)
        val called = mutableListOf<String>()

        manager.subscribe("plugin-a", StartsWith(prefix = "设置b50") { called += "setting:$it" }, "rhythm", "mai")
        manager.subscribe("plugin-a", CommandEndsWith(suffix = "50") { called += "suffix:$it" }, "rhythm", "mai")

        manager.handle(messageEvent("设置b50"))

        assertEquals(listOf("setting:"), called)
    }

    @Test
    fun shouldUseDefaultHandlerForEqualMatches() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val manager = SubscribeManager(dispatcher)
        val called = mutableListOf<String>()
        val defaultHandler: suspend xyz.xszq.bot.event.MessageEvent.() -> String? = { "chu" }

        manager.subscribe(
            "plugin-mai",
            StartsWith(prefix = "b50") { called += "mai" },
            "rhythm",
            "mai",
            defaultHandler
        )
        manager.subscribe(
            "plugin-chu",
            StartsWith(prefix = "b50") { called += "chu" },
            "rhythm",
            "chu",
            defaultHandler
        )

        manager.handle(messageEvent("b50"))

        assertEquals(listOf("chu"), called)
    }
}