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

        testB50(sandbox, "maxscore")

        testBind(sandbox)
        testB50(sandbox)
    }

    private suspend fun testBind(sandbox: BotSandbox) {
        sandbox.clear()
        val msg = sandbox.user() says "/bind 943551369"
        assertReplied(sandbox, msg, "绑定成功")
    }

    private suspend fun testB50(sandbox: BotSandbox, args: String ?= null) {
        sandbox.clear()
        val content = args ?.let {
            "/b50 $args"
        } ?: "/b50"
        val msg = sandbox.user() says content
        Thread.sleep(3000)
        assertRepliedWithImage(sandbox, msg)
    }
}
