package xyz.xszq.bot.maimai

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import xyz.xszq.bot.*
import xyz.xszq.bot.maimai.database.MaimaiDatabaseTest
import xyz.xszq.bot.payload.UploadResult
import kotlin.test.Test
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class RatingRenderTest : MaimaiDatabaseTest() {

    @Test
    fun testB50MaxScore() = runTest {
        val cos = mockk<TencentCos>(relaxed = true)
        coEvery { cos.uploadBinary(any(), any()) } returns UploadResult("https://example.com/b50.jpg", "b50.jpg")

        val sandbox = BotSandbox(this, cos, database)

        val maimai = Maimai().apply {
            plugin = "maimai"
            pluginLoader = sandbox.pluginLoader
            configPath = "../config/maimai.yml"
            dataPath = "../data/maimai"
        }
        try {
            maimai.load()
            Thread.sleep(3000)

            val msg = sandbox.user() says "/b50 maxscore"

            assertNotNull(sandbox.replyFor(msg)) {
                "Reply is null, replies.size=${sandbox.replies.size}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
