package xyz.xszq.shinobu.dom

import org.jetbrains.skia.FontSlant
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.FontWidth
import org.jetbrains.skia.Typeface
import org.jetbrains.skia.paragraph.FontCollection

object FontResolver {
    data class TextRun(
        val text: String,
        val typeface: Typeface? = null,
        val fontFamily: String? = null,
    )

    fun resolve(
        text: String,
        fontFamilies: List<String>,
        fontWeight: Int,
        fontCollection: FontCollection
    ): List<TextRun> {
        if (text.isEmpty()) return emptyList()
        if (fontFamilies.isEmpty()) return listOf(TextRun(text))

        val desiredStyle = FontStyle(fontWeight, FontWidth.NORMAL, FontSlant.UPRIGHT)

        val familyTypefaces = fontFamilies.map { family ->
            fontCollection.findTypefaces(arrayOf(family), desiredStyle).firstOrNull()
        }

        data class FontKey(val familyIdx: Int, val typefaceId: Int)
        val noFontKey = FontKey(-1, -1)

        val keys = text.map { char ->
            val cp = char.code
            var key = noFontKey

            for ((idx, tf) in familyTypefaces.withIndex()) {
                if (tf != null && tf.getUTF32Glyph(cp) != 0.toShort()) {
                    key = FontKey(idx, -1)
                    break
                }
            }

            if (key == noFontKey) {
                val fallbackTf = fontCollection.defaultFallback(cp, desiredStyle, "zh-Hans")
                if (fallbackTf != null && fallbackTf.getUTF32Glyph(cp) != 0.toShort()) {
                    val weightedTf = fontCollection.findTypefaces(
                        arrayOf(fallbackTf.familyName), desiredStyle
                    ).firstOrNull()
                    val tf = weightedTf ?: fallbackTf
                    key = FontKey(-1, tf.uniqueId)
                } else {
                    key = FontKey(0, -1)
                }
            }

            key
        }

        val runs = mutableListOf<TextRun>()
        var i = 0
        while (i < text.length) {
            val key = keys[i]
            var j = i + 1
            while (j < text.length && keys[j] == key) {
                j++
            }

            val chunk = text.substring(i, j)
            if (key.familyIdx >= 0) {
                runs.add(TextRun(text = chunk, fontFamily = fontFamilies[key.familyIdx]))
            } else {
                val cp = text[i].code
                val fallbackTf = fontCollection.defaultFallback(cp, desiredStyle, "zh-Hans")
                if (fallbackTf != null && fallbackTf.getUTF32Glyph(cp) != 0.toShort()) {
                    val weightedTf = fontCollection.findTypefaces(
                        arrayOf(fallbackTf.familyName), desiredStyle
                    ).firstOrNull()
                    runs.add(TextRun(text = chunk, typeface = weightedTf ?: fallbackTf))
                } else {
                    runs.add(TextRun(text = chunk, fontFamily = fontFamilies.first()))
                }
            }

            i = j
        }

        return runs
    }
}
