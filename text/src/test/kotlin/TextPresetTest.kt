package xyz.xszq.bot

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import xyz.xszq.bot.config.TextConfig
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.message.MessageChain
import xyz.xszq.bot.message.PlainText
import xyz.xszq.bot.subscribe.SubscribeManager
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class TextPresetTest {

    @Test
    fun presetMatchCallsSendC2CMessage() = runTest {
        val (subscribes, bot, sent) = setupTest(
            presets = mapOf("有人吗" to "有bot在哦")
        )
        subscribes.handle(createMessageEvent("有人吗", bot))
        advanceUntilIdle()
        assertEquals("有bot在哦", sent.last())
    }

    @Test
    fun presetNonMatchDoesNotSend() = runTest {
        val (subscribes, bot, sent) = setupTest(
            presets = mapOf("有人吗" to "有bot在哦")
        )
        subscribes.handle(createMessageEvent("有人吗", bot))
        advanceUntilIdle()
        assertEquals(1, sent.size)

        subscribes.handle(createMessageEvent("你好", bot))
        advanceUntilIdle()
        assertEquals(1, sent.size)
    }

    @Test
    fun emptyPresetsSendNothing() = runTest {
        val (subscribes, bot, _) = setupTest(presets = emptyMap())
        subscribes.handle(createMessageEvent("有人吗", bot))
        advanceUntilIdle()
    }

    private suspend fun TestScope.setupTest(
        presets: Map<String, String>
    ): Triple<SubscribeManager, Bot, MutableList<String>> {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val sentContents = mutableListOf<String>()

        val api = mockk<OpenAPI>(relaxed = true)
        coEvery {
            api.sendC2CMessage(any(), capture(sentContents), any(), any(), any(), any(), any(), any())
        } returns true

        val subscribes = SubscribeManager(dispatcher)
        val pluginLoader = mockk<PluginLoader>(relaxed = true)
        every { pluginLoader.subscribes } returns subscribes
        every { pluginLoader.llmClient } returns null

        val bot = Bot(api, mockk(relaxed = true), pluginLoader)
        every { pluginLoader.bot } returns bot

        Text().apply {
            plugin = "text-test"
            this.pluginLoader = pluginLoader
            textConfig = TextConfig(
                system = "",
                presets = presets,
                remoteKey = ""
            )
        }.setRoute()
        advanceUntilIdle()

        return Triple(subscribes, bot, sentContents)
    }

    private fun createMessageEvent(text: String, bot: Bot) = MessageEvent(
        bot = bot,
        eventId = "event-id",
        id = "msg-id",
        message = MessageChain(PlainText(text)),
        sender = User(bot, "user-id"),
    )
}
