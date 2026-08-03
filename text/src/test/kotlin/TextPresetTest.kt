package xyz.xszq.bot

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import xyz.xszq.bot.config.TextConfig
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class TextPresetTest {

    @Test
    fun presetMatch() = runTest {
        val sandbox = BotSandbox(this)
        setPreset(sandbox, "有人吗" to "有bot在哦")

        val msg = sandbox.user() says "有人吗"

        assertEquals("有bot在哦", sandbox.replyFor(msg) ?.text)
    }

    @Test
    fun noReplyForNonMatch() = runTest {
        val sandbox = BotSandbox(this)
        setPreset(sandbox, "有人吗" to "有bot在哦")

        val msg = sandbox.user() says "你好"

        assertEquals(0, sandbox.replies.size)
        assertEquals(null, sandbox.replyFor(msg))
    }

    @Test
    fun matchAndNonMatch() = runTest {
        val sandbox = BotSandbox(this)
        setPreset(sandbox, "有人吗" to "有bot在哦")

        val msg1 = sandbox.user() says "有人吗"
        assertEquals("有bot在哦", sandbox.replyFor(msg1) ?.text)

        val msg2 = sandbox.user() says "你好"
        assertEquals(1, sandbox.replies.size)
        assertEquals("有bot在哦", sandbox.replyFor(msg1) ?.text)
        assertEquals(null, sandbox.replyFor(msg2))
    }

    @Test
    fun matchTwice() = runTest {
        val sandbox = BotSandbox(this)
        setPreset(sandbox, "有人吗" to "有bot在哦")

        val msg1 = sandbox.user() says "有人吗"
        val msg2 = sandbox.user() says "有人吗"

        assertEquals(2, sandbox.replies.size)
        assertEquals("有bot在哦", sandbox.replyFor(msg1) ?.text)
        assertEquals("有bot在哦", sandbox.replyFor(msg2) ?.text)
    }

    private suspend fun setPreset(
        sandbox: BotSandbox,
        vararg presets: Pair<String, String>
    ) {
        Text().apply {
            plugin = "text"
            pluginLoader = sandbox.pluginLoader
            textConfig = TextConfig(
                system = "",
                presets = presets.toMap(),
            )
        }.setRoute()
        sandbox.advanceIdle()
    }
}
