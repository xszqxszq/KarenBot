package xyz.xszq.bot

import io.mockk.coEvery
import io.mockk.mockk
import korlibs.io.file.VfsFile
import xyz.xszq.bot.event.Event
import xyz.xszq.bot.payload.UploadResult
import xyz.xszq.bot.service.TencentCOS
import java.io.File

fun mockTencentCOS(): TencentCOS {
    val cos = mockk<TencentCOS>(relaxed = true)
    coEvery { cos.uploadBinary(any(), any()) } returns UploadResult("https://example.com/b50.jpg", "b50.jpg")
    coEvery { cos.upload(any<File>()) } returns UploadResult("https://example.com/upload.jpg", "upload.jpg")
    coEvery { cos.upload(any<VfsFile>()) } returns UploadResult("https://example.com/upload.jpg", "upload.jpg")
    return cos
}

fun assertReplied(sandbox: BotSandbox, msg: Event, containsText: String) {
    val reply = sandbox.awaitReply(msg)
    check(reply != null) { "No reply for message" }
    check(reply.text.contains(containsText)) { "Expected '$containsText', got: ${reply.text}" }
}

fun assertRepliedAny(sandbox: BotSandbox, msg: Event) {
    check(sandbox.awaitReply(msg) != null) { "No reply for message" }
}

fun assertRepliedWithImage(sandbox: BotSandbox, msg: Event) {
    val reply = sandbox.awaitReply(msg)
    check(reply != null) { "No image reply" }
    check(reply.text.contains("![img]") || reply.text.contains("img #")) {
        "Expected image markdown, got: ${reply.text}"
    }
}