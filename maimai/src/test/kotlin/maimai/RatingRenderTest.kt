package xyz.xszq.bot.maimai

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import xyz.xszq.bot.*
import xyz.xszq.bot.maimai.database.MaimaiDatabaseTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RatingRenderTest : MaimaiDatabaseTest() {

    @Test
    fun runAll() = runTest {
        val sandbox = setMaimai(this, database)

        testBind(sandbox)
        testB50(sandbox)
    }

    private suspend fun testBind(sandbox: BotSandbox) {
        sandbox.clear()
        val msg = sandbox.user() says "/bind 943551369"
        assertReplied(sandbox, msg, "绑定成功")
    }

    private suspend fun testB50(sandbox: BotSandbox) {
        sandbox.clear()
        val msg = sandbox.user() says "/b50 maxscore"
        Thread.sleep(3000)
        assertRepliedWithImage(sandbox, msg)
    }
}
