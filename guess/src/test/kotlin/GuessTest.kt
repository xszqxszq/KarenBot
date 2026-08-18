package xyz.xszq.bot

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import xyz.xszq.bot.guess.Guess
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GuessTest {

    @Test
    fun runAll() = runTest {
        val sandbox = setGuess(this)
        try {
            testQuizHelp(sandbox)
            testQuizInvalid(sandbox)
            testRandomMusic(sandbox)
            testQuiz(sandbox)
        } finally {
            sandbox.cleanup()
        }
    }

    private suspend fun testQuizHelp(sandbox: BotSandbox) {
        sandbox.clear()
        val msg = sandbox.user() says "原曲认知测验"
        assertNotNull(sandbox.awaitReply(msg)) { "No reply for help" }
    }

    private suspend fun testQuizInvalid(sandbox: BotSandbox) {
        sandbox.clear()
        val reply = sandbox.awaitReply(sandbox.user() says "原曲认知测验 无效")
        assertNotNull(reply) { "No reply" }
        assertTrue(reply.text.contains("该难度不存在"), "Expected error, got: ${reply.text}")
    }

    private suspend fun testRandomMusic(sandbox: BotSandbox) {
        sandbox.clear()
        val msg = sandbox.user() says "随机东方原曲"
        assertNotNull(sandbox.awaitReply(msg)) { "No reply for random music" }
    }

    private suspend fun testQuiz(sandbox: BotSandbox) {
        sandbox.clear()
        val msg = sandbox.user() says "原曲认知测验 normal 新作"
        assertNotNull(sandbox.awaitReply(msg)) { "No reply for quiz" }
    }
}

suspend fun setGuess(scope: TestScope): BotSandbox {
    val sandbox = BotSandbox(scope)
    val guess = Guess().apply {
        plugin = "guess"
        pluginLoader = sandbox.pluginLoader
    }
    guess.load()
    sandbox.cleanup = { guess.unload() }
    return sandbox
}