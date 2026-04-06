package xyz.xszq.bot.subscribe

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import xyz.xszq.bot.messageEvent

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
}
