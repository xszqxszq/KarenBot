package xyz.xszq.shinobu.template

import org.jetbrains.skia.*
import org.jetbrains.skia.paragraph.*
import java.io.File

class BitmapFontManager(
    private val cacheDir: File,
    private val fontCollection: FontCollection
) {
    data class GlyphEntry(val ch: String, val x: Int, val w: Int)

    class AtlasCache(
        val image: Image,
        val glyphs: Map<Char, GlyphEntry>,
        val height: Int
    )

    private val charsets = mutableMapOf<String, String>()
    private val atlases = mutableMapOf<String, AtlasCache>()

    fun registerCharset(id: String, chars: String) {
        charsets[id] = chars.toSet().joinToString("")
    }

    fun getAtlas(
        charsetId: String,
        fontSize: Float,
        fontFamilies: List<String>?
    ): AtlasCache? {
        val charset = charsets[charsetId] ?: return null
        val key = buildKey(charsetId, fontSize, fontFamilies)

        atlases[key]?.let { return it }
        loadFromDisk(key)?.let { atlases[key] = it; return it }
        val atlas = buildAtlas(charset, fontSize, fontFamilies)
        atlases[key] = atlas
        saveToDisk(key, atlas)
        return atlas
    }

    private fun buildAtlas(
        charset: String,
        fontSize: Float,
        fontFamilies: List<String>?
    ): AtlasCache {
        val glyphEntries = mutableListOf<GlyphEntry>()
        val glyphImages = mutableListOf<Image>()
        var totalWidth = 0
        var maxHeight = 0

        for (ch in charset) {
            val img = renderGlyph(ch.toString(), fontSize, fontFamilies)
            glyphImages.add(img)
            val w = img.width
            val h = img.height
            glyphEntries.add(GlyphEntry(ch.toString(), totalWidth, w))
            totalWidth += w
            if (h > maxHeight) maxHeight = h
        }

        if (totalWidth == 0 || maxHeight == 0) {
            val empty = Surface.makeRasterN32Premul(1, 1).use { it.makeImageSnapshot() }
            return AtlasCache(empty, emptyMap(), 1)
        }

        val atlasImage = Surface.makeRasterN32Premul(totalWidth, maxHeight).use { surface ->
            var x = 0
            for (img in glyphImages) {
                surface.canvas.drawImage(img, x.toFloat(), 0f)
                x += img.width
            }
            surface.makeImageSnapshot()
        }

        glyphImages.forEach { it.close() }
        return AtlasCache(atlasImage, glyphEntries.associateBy { it.ch.single() }, maxHeight)
    }

    private fun renderGlyph(
        text: String,
        fontSize: Float,
        fontFamilies: List<String>?
    ): Image {
        val paragraph = buildParagraph(text, fontSize, fontFamilies)
        val w = paragraph.maxIntrinsicWidth.let { if (it < 1f) fontSize * 2f else it }.toInt().coerceAtLeast(1)
        val h = paragraph.height.let { if (it < 1f) fontSize * 1.5f else it }.toInt().coerceAtLeast(1)

        return Surface.makeRasterN32Premul(w, h).use { surface ->
            paragraph.paint(surface.canvas, 0f, 0f)
            surface.makeImageSnapshot()
        }
    }

    private fun buildParagraph(
        text: String,
        fontSize: Float,
        fontFamilies: List<String>?
    ): Paragraph {
        val textStyle = TextStyle().apply {
            this.fontSize = fontSize
            this.color = Color.WHITE
            if (fontFamilies != null) this.fontFamilies = fontFamilies.toTypedArray()
        }
        val paragraphStyle = ParagraphStyle().apply { this.textStyle = textStyle }
        return ParagraphBuilder(paragraphStyle, fontCollection).apply {
            pushStyle(textStyle)
            addText(text)
            popStyle()
        }.build().also { it.layout(Float.MAX_VALUE) }
    }

    private fun buildKey(charsetId: String, fontSize: Float, fontFamilies: List<String>?): String {
        val fams = fontFamilies?.joinToString(",") ?: "default"
        return "$charsetId|${fontSize.toInt()}|$fams"
    }

    private fun loadFromDisk(key: String): AtlasCache? {
        return runCatching {
            val png = File(cacheDir, "$key.png")
            val metaFile = File(cacheDir, "$key.json")
            if (!png.exists() || !metaFile.exists()) return null
            val lines = metaFile.readLines()
            val height = lines[0].toInt()
            val glyphs = mutableMapOf<Char, GlyphEntry>()
            for (i in 1 until lines.size) {
                val parts = lines[i].split(",")
                if (parts.size >= 3) {
                    val ch = parts[0].single()
                    val x = parts[1].toInt()
                    val w = parts[2].toInt()
                    glyphs[ch] = GlyphEntry(parts[0], x, w)
                }
            }
            val image = Image.makeFromEncoded(png.readBytes())
            AtlasCache(image, glyphs, height)
        }.getOrNull()
    }

    private fun saveToDisk(key: String, atlas: AtlasCache) {
        runCatching {
            cacheDir.mkdirs()
            val encoded = atlas.image.encodeToData(EncodedImageFormat.PNG) ?: return
            File(cacheDir, "$key.png").writeBytes(encoded.bytes)
            File(cacheDir, "$key.json").writeText(
                buildString {
                    appendLine(atlas.height)
                    atlas.glyphs.values.forEach { g ->
                        appendLine("${g.ch},${g.x},${g.w}")
                    }
                }
            )
        }
    }
}
