package xyz.xszq.bot.message

import kotlin.test.Test
import kotlin.test.assertEquals

class MessageChainTest {
    @Test
    fun shouldParseFaceAndOmitInPlainText() {
        val raw = "Example ${Face(1, 2, "Laugh")} Text"

        val chain = MessageChain(raw, emptyList(), emptyList())

        assertEquals("Example [Laugh] Text", chain.content)
        assertEquals("Example  Text", chain.text)
        assertEquals(raw, chain.textToSend())
    }

    @Test
    fun shouldCombineTexts() {
        val chain = MessageChain(PlainText("Example")) + PlainText(" Text")

        assertEquals("Example Text", chain.content)
        assertEquals("Example Text", chain.textToSend())
    }
}
