package xyz.xszq.bot

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import xyz.xszq.bot.audio.Audio
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AudioTest {

    @Test
    fun runAll() = runTest {
        val sandbox = setAudio(this)
        try {
            testTTSHelp(sandbox)
            testTTSGenerate(sandbox)
            testQuizHelp(sandbox)
            testQuizInvalid(sandbox)
            testRandomMusic(sandbox)
            testQuiz(sandbox)
        } finally {
            sandbox.cleanup()
        }
    }

    private suspend fun testTTSHelp(sandbox: BotSandbox) {
        sandbox.clear()
        val reply = sandbox.awaitReply(sandbox.user() says "活字印刷")
        assertNotNull(reply) { "无帮助回复" }
        assertTrue(reply.text.contains("使用方法"), "应给出用法提示")
    }

    private suspend fun testTTSGenerate(sandbox: BotSandbox) {
        sandbox.clear()
        val msg = sandbox.user() says "活字印刷 大家好啊"
        assertNotNull(sandbox.awaitReply(msg)) { "无 TTS 回复" }
    }

    private suspend fun testQuizHelp(sandbox: BotSandbox) {
        sandbox.clear()
        val msg = sandbox.user() says "原曲认知测验"
        assertNotNull(sandbox.awaitReply(msg)) { "无帮助回复" }
    }

    private suspend fun testQuizInvalid(sandbox: BotSandbox) {
        sandbox.clear()
        val reply = sandbox.awaitReply(sandbox.user() says "原曲认知测验 无效")
        assertNotNull(reply) { "无回复" }
        assertTrue(reply.text.contains("该难度不存在"), "应提示难度错误")
    }

    private suspend fun testRandomMusic(sandbox: BotSandbox) {
        sandbox.clear()
        val msg = sandbox.user() says "随机东方原曲"
        assertNotNull(sandbox.awaitReply(msg)) { "无随机原曲回复" }
    }

    private suspend fun testQuiz(sandbox: BotSandbox) {
        sandbox.clear()
        val msg = sandbox.user() says "原曲认知测验 normal 新作"
        assertNotNull(sandbox.awaitReply(msg)) { "无猜题回复" }
    }
}

suspend fun setAudio(scope: TestScope): BotSandbox {
    val sandbox = BotSandbox(scope)
    val audio = Audio().apply {
        plugin = "audio"
        pluginLoader = sandbox.pluginLoader
    }
    audio.load()
    sandbox.cleanup = { audio.unload() }
    return sandbox
}