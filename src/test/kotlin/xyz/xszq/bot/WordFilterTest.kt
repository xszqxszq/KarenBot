package xyz.xszq.bot

import kotlin.test.Test
import kotlin.test.assertEquals
import xyz.xszq.bot.service.WordFilter

class WordFilterTest {
    @Test
    fun shouldFilterWords() {
        val filter = WordFilter(listOf("nsfw", "swear"))
        val result = filter.filter("Nsfw and swear")
        assertEquals("**** and *****", result)
    }

    @Test
    fun shouldKeepOtherWords() {
        val filter = WordFilter(listOf("Nsfw"))
        val sentence = "Valid and positive"

        assertEquals(sentence, filter.filter(sentence))
    }
}