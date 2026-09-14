package xyz.xszq.bot.payload

import xyz.xszq.bot.util.json
import kotlin.test.Test
import kotlin.test.assertEquals

class AttachmentTest {
    @Test
    fun shouldDecodeLargeAttachmentSize() {
        val raw = """{"url":"https://example.com/a.zip","filename":"a.zip","size":4070910330,"content_type":"file"}"""
        val attachment = json.decodeFromString<Attachment>(raw)

        assertEquals(4070910330L, attachment.size)
    }
}