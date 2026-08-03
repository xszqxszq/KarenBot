package xyz.xszq.bot.maimai

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import xyz.xszq.bot.*
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.payload.UploadResult
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

suspend fun setMaimai(scope: TestScope, database: org.jetbrains.exposed.sql.Database): BotSandbox {
    val cos = mockk<TencentCos>(relaxed = true)
    coEvery { cos.uploadBinary(any(), any()) } returns UploadResult("https://example.com/b50.jpg", "b50.jpg")

    val sandbox = BotSandbox(scope, cos, database)
    val maimai = Maimai().apply {
        plugin = "maimai"
        pluginLoader = sandbox.pluginLoader
        configPath = "../config/maimai.yml"
        dataPath = "../data/maimai"
    }
    maimai.load()
    maimai.image.manager.init()
    return sandbox
}

fun assertReplied(sandbox: BotSandbox, msg: MessageEvent, containsText: String) {
    val reply = sandbox.replyFor(msg)
    assertNotNull(reply) { "No reply for message" }
    assertTrue(reply.text.contains(containsText), "Expected '$containsText', got: ${reply.text}")
}

fun assertRepliedWithImage(sandbox: BotSandbox, msg: MessageEvent) {
    val reply = sandbox.replyFor(msg)
    assertNotNull(reply) { "No image reply" }
    assertTrue(
        reply.text.contains("![img]") || reply.text.contains("img #"),
        "Expected image markdown, got: ${reply.text}"
    )
}
