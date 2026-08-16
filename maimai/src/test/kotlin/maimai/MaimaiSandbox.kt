package xyz.xszq.bot.maimai

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import korlibs.io.file.VfsFile
import kotlinx.coroutines.test.TestScope
import xyz.xszq.bot.*
import xyz.xszq.bot.event.Event
import xyz.xszq.bot.payload.UploadResult
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

suspend fun setMaimai(scope: TestScope, database: org.jetbrains.exposed.sql.Database): BotSandbox {
    val cos = mockk<TencentCos>(relaxed = true)
    coEvery { cos.uploadBinary(any(), any()) } returns UploadResult("https://example.com/b50.jpg", "b50.jpg")
    every { cos.upload(any<File>()) } returns UploadResult("https://example.com/upload.jpg", "upload.jpg")
    every { cos.upload(any<VfsFile>()) } returns UploadResult("https://example.com/upload.jpg", "upload.jpg")

    val sandbox = BotSandbox(scope, cos, database)
    val maimai = Maimai().apply {
        plugin = "maimai"
        pluginLoader = sandbox.pluginLoader
        configPath = "../config/maimai.yml"
        dataPath = "../data/maimai"
    }
    maimai.load()
    maimai.image.manager.init()
    sandbox.cleanup = { maimai.unload() }
    return sandbox
}

fun assertReplied(sandbox: BotSandbox, msg: Event, containsText: String) {
    val reply = sandbox.awaitReply(msg)
    assertNotNull(reply) { "No reply for message" }
    assertTrue(reply.text.contains(containsText), "Expected '$containsText', got: ${reply.text}")
}

fun assertRepliedAny(sandbox: BotSandbox, msg: Event) {
    assertNotNull(sandbox.awaitReply(msg)) { "No reply for message" }
}

fun assertRepliedWithImage(sandbox: BotSandbox, msg: Event) {
    val reply = sandbox.awaitReply(msg)
    assertNotNull(reply) { "No image reply" }
    assertTrue(
        reply.text.contains("![img]") || reply.text.contains("img #"),
        "Expected image markdown, got: ${reply.text}"
    )
}
