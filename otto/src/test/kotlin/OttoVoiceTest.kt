package xyz.xszq.bot

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import xyz.xszq.bot.otto.OttoVoice
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OttoVoiceTest {

    @Test
    fun runAll() = runTest {
        val sandbox = setOtto(this)
        try {
            testTtsHelp(sandbox)
            testTtsGenerate(sandbox)
        } finally {
            sandbox.cleanup()
        }
    }

    private suspend fun testTtsHelp(sandbox: BotSandbox) {
        sandbox.clear()
        val reply = sandbox.awaitReply(sandbox.user() says "活字印刷")
        assertNotNull(reply) { "No reply for help" }
        assertTrue(reply.text.contains("使用方法"), "Expected help, got: ${reply.text}")
    }

    private suspend fun testTtsGenerate(sandbox: BotSandbox) {
        sandbox.clear()
        val msg = sandbox.user() says "活字印刷 大家好啊"
        assertNotNull(sandbox.awaitReply(msg)) { "No TTS reply" }
    }
}

suspend fun setOtto(scope: TestScope): BotSandbox {
    val sandbox = BotSandbox(scope)
    val otto = OttoVoice().apply {
        plugin = "otto"
        pluginLoader = sandbox.pluginLoader
    }
    otto.load()
    sandbox.cleanup = { otto.unload() }
    return sandbox
}