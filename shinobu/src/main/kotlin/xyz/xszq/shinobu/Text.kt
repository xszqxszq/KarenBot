package xyz.xszq.shinobu

import korlibs.image.font.SystemFontRegistry
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlValue

@Serializable
data class Text(
    @XmlValue(true)
    var text: String = "",
    var size: Int = 16,
    var font: String = "",
    var color: String = "#000000",
    val border: Border? = null,
    val shadow: Shadow? = null,
    override val id: String? = null,
    @XmlOtherAttributes
    override val margin: Spacing = Spacing(0),
    @XmlOtherAttributes
    override val padding: Spacing = Spacing(0),
): Element {
    @Transient
    override var parent: Container? = null
    @Transient
    var descent: Double = 0.0
    suspend fun calcWidth(): Int {
        val glyphs = SystemFontRegistry()[font].measureTextGlyphs(size, text)
        return glyphs.glyphs.lastOrNull()?.let {
            it.pos[0] + it.metrics.xadvance
        } ?.toInt() ?: 0
    }
    suspend fun isCharMissing(): Boolean = runCatching {
        val glyphs = SystemFontRegistry()[font].measureTextGlyphs(size, text.filter { it != ' ' && it != '　' })
        glyphs.glyphs.any { it.metrics.width == 0.0 }
    }.getOrNull() ?: true
}
