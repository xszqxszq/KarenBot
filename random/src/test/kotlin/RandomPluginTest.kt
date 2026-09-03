package xyz.xszq.bot

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import xyz.xszq.bot.random.RandomPlugin
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RandomPluginTest {

    @Test
    fun randomNumberWithoutArgs() = runTest {
        val sandbox = BotSandbox(this)
        setRoute(sandbox)

        val msg = sandbox.user() says "随机数字"

        val text = sandbox.replyFor(msg) ?.text ?: error("无回复")
        assertTrue(text.toIntOrNull() != null, "应回复一个整数，实际: $text")
    }

    @Test
    fun randomNumberWithUpperBound() = runTest {
        val sandbox = BotSandbox(this)
        setRoute(sandbox)

        val msg = sandbox.user() says "随机数字 5"

        val text = sandbox.replyFor(msg) ?.text ?: error("无回复")
        val value = text.toIntOrNull() ?: error("应回复一个整数，实际: $text")
        assertTrue(value in 0..4, "上界 5 应生成 0..4 内的数，实际: $text")
    }

    @Test
    fun randomUUID() = runTest {
        val sandbox = BotSandbox(this)
        setRoute(sandbox)

        val msg = sandbox.user() says "随机uuid"

        val text = sandbox.replyFor(msg) ?.text ?: error("无回复")
        assertTrue(text.isNotBlank(), "应回复 UUID")
    }

    private suspend fun setRoute(sandbox: BotSandbox) {
        RandomPlugin().apply {
            plugin = "random"
            pluginLoader = sandbox.pluginLoader
        }.setRoute()
        sandbox.advanceIdle()
    }
}