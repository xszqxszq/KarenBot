package xyz.xszq.bot.image.parse

import org.jetbrains.skia.Color
import xyz.xszq.bot.image.style.*

object StyleParser {
    fun parse(styleString: String): Style {
        val style = Style()
        val declarations = styleString.split(";").map { it.trim() }.filter { it.isNotEmpty() }

        for (declaration in declarations) {
            val parts = declaration.split(":", limit = 2)
            if (parts.size != 2)
                continue
            val key = parts[0].trim().lowercase()
            val value = parts[1].trim()

            when (key) {
                "width" -> style.width = parsePx(value)
                "min-width" -> style.minWidth = parsePx(value)
                "max-width" -> style.maxWidth = parsePx(value)
                "height" -> style.height = parsePx(value)
                "min-height" -> style.minHeight = parsePx(value)
                "max-height" -> style.maxHeight = parsePx(value)

                "background-color" -> style.backgroundColor = parseColor(value)
                "background-image" -> {
                    val match = """url\(['"]?(.*?)['"]?\)""".toRegex().find(value)
                    if (match != null)
                        style.backgroundImage = match.groupValues[1]
                }
                "background-size" -> style.backgroundSize = parseBackgroundSize(value)
                "background-position" -> style.backgroundPosition = parseBackgroundPosition(value)
                "background-opacity" -> style.backgroundOpacity = value.toFloatOrNull() ?: 1.0f

                "margin" -> style.margin = parseSpacing(value)
                "padding" -> style.padding = parseSpacing(value)

                "flex-direction" -> style.flexDirection =
                    if (value == "column") FlexDirection.COLUMN
                    else FlexDirection.ROW
                "flex-wrap" -> style.flexWrap =
                    if (value == "wrap") FlexWrap.WRAP
                    else FlexWrap.NOWRAP
                "justify-content" -> style.justifyContent = parseJustify(value)
                "align-items" -> style.alignItems = parseAlignItems(value)

                "opacity" -> style.opacity = value.toFloatOrNull() ?: 1.0f
                "color" -> style.textColor = parseColor(value) ?: Color.BLACK
                "font-size" -> style.textSize = parsePx(value) ?: 14f
                "min-font-size" -> style.minTextSize = parsePx(value)
                "font-family" -> style.fontFamilies = value.split(",").map {
                    it.trim().replace("'", "").replace("\"", "")
                }
                "font-weight" -> style.fontWeight = parseFontWeight(value)
                "text-stroke" -> style.textStroke = parseStroke(value)
                "text-shadow" -> style.textShadow = parseShadow(value)
                "text-align" -> style.textAlign = when (value) {
                    "center" -> TextAlign.CENTER
                    "right" -> TextAlign.RIGHT
                    else -> TextAlign.LEFT
                }
                "white-space" -> style.whiteSpace =
                    if (value == "nowrap") WhiteSpace.NOWRAP
                    else WhiteSpace.NORMAL

                "object-fit" -> style.objectFit = when (value) {
                    "cover" -> ObjectFit.COVER
                    "contain" -> ObjectFit.CONTAIN
                    "none" -> ObjectFit.NONE
                    else -> ObjectFit.FILL
                }
                "mask-image" -> {
                    val match = """url\(['"]?(.*?)['"]?\)""".toRegex().find(value)
                    if (match != null)
                        style.maskImage = match.groupValues[1]
                }
            }
        }
        return style
    }

    private fun parsePx(value: String): Float? {
        return value.replace("px", "").trim().toFloatOrNull()
    }

    private fun parseColor(str: String): Int? {
        if (str.startsWith("rgb")) {
            return str.rgba()
        }

        if (str.startsWith("#")) {
            return str.rgbColor()
        }

        if (str == "transparent")
            return 0x00000000

        return null
    }

    fun String.rgbColor(): Int? {
        var hex = substring(1)

        if (hex.length == 3 || hex.length == 4) {
            hex = hex.map { "$it$it" }.joinToString("")
        }

        return runCatching {
            when (hex.length) {
                6 -> {
                    val r = hex.substring(0, 2).toInt(16)
                    val g = hex.substring(2, 4).toInt(16)
                    val b = hex.substring(4, 6).toInt(16)
                    (255 shl 24) or (r shl 16) or (g shl 8) or b
                }
                8 -> {
                    val r = hex.substring(0, 2).toInt(16)
                    val g = hex.substring(2, 4).toInt(16)
                    val b = hex.substring(4, 6).toInt(16)
                    val a = hex.substring(6, 8).toInt(16)
                    (a shl 24) or (r shl 16) or (g shl 8) or b
                }
                else -> null
            }
        }.getOrNull()
    }
    fun String.rgba(): Int? {
        val regex = Regex("""rgba?\s*\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)(?:\s*,\s*([0-9.]+)\s*)?\)""")
        val match = regex.find(this) ?: return null

        val r = match.groupValues[1].toIntOrNull()?.coerceIn(0, 255) ?: 0
        val g = match.groupValues[2].toIntOrNull()?.coerceIn(0, 255) ?: 0
        val b = match.groupValues[3].toIntOrNull()?.coerceIn(0, 255) ?: 0

        val aFloat = match.groupValues.getOrNull(4)?.takeIf { it.isNotEmpty() }?.toFloatOrNull() ?: 1.0f
        val a = (aFloat * 255).toInt().coerceIn(0, 255)

        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
    fun rgba(
        r: Int,
        g: Int,
        b: Int,
        a: Float
    ): Int {
        val alpha = (a * 255).toInt().coerceIn(0, 255)
        return (alpha shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun parseBackgroundSize(value: String) = when (value) {
        "100% 100%" -> BackgroundSize.STRETCH_FILL
        "cover" -> BackgroundSize.COVER
        "contain" -> BackgroundSize.CONTAIN
        else -> BackgroundSize.AUTO
    }
    private fun parseBackgroundPosition(value: String): BackgroundPosition {
        val normalized = value.lowercase().trim().replace(Regex("\\s+"), " ")

        return when (normalized) {
            "top left", "left top" -> BackgroundPosition.TOP_LEFT
            "top", "top center", "center top" -> BackgroundPosition.TOP_CENTER
            "top right", "right top" -> BackgroundPosition.TOP_RIGHT

            "left", "center left", "left center" -> BackgroundPosition.CENTER_LEFT
            "center", "center center" -> BackgroundPosition.CENTER
            "right", "center right", "right center" -> BackgroundPosition.CENTER_RIGHT

            "bottom left", "left bottom" -> BackgroundPosition.BOTTOM_LEFT
            "bottom", "bottom center", "center bottom" -> BackgroundPosition.BOTTOM_CENTER
            "bottom right", "right bottom" -> BackgroundPosition.BOTTOM_RIGHT

            else -> BackgroundPosition.CENTER
        }
    }

    private fun parseSpacing(value: String): Spacing {
        val parts = value.split("\\s+".toRegex()).mapNotNull { parsePx(it) }
        return when (parts.size) {
            // all
            1 -> Spacing(parts[0], parts[0], parts[0], parts[0])
            // top-bottom, left-right
            2 -> Spacing(parts[0], parts[1], parts[0], parts[1])
            // top, right, bottom, left
            4 -> Spacing(parts[0], parts[1], parts[2], parts[3])
            else -> Spacing()
        }
    }

    private fun parseJustify(value: String) = when (value) {
        "flex-end" -> JustifyContent.FLEX_END
        "center" -> JustifyContent.CENTER
        "space-between" -> JustifyContent.SPACE_BETWEEN
        "space-around" -> JustifyContent.SPACE_AROUND
        else -> JustifyContent.FLEX_START
    }

    private fun parseAlignItems(value: String) = when (value) {
        "flex-end" -> AlignItems.FLEX_END
        "center" -> AlignItems.CENTER
        "stretch" -> AlignItems.STRETCH
        else -> AlignItems.FLEX_START
    }

    private fun parseFontWeight(value: String): Int {
        return when (value) {
            "normal" -> 400
            "bold" -> 700
            "bolder" -> 800
            "heavy", "black" -> 900
            else -> value.toIntOrNull() ?: 400
        }
    }

    private fun parseStroke(value: String): TextStroke? {
        val parts = value.trim().split("\\s+".toRegex())
        if (parts.size != 2)
            return null

        var size: Float? = null
        var color: Int? = null

        for (part in parts) {
            if (part.endsWith("px") || part.toFloatOrNull() != null) {
                size = part.replace("px", "").toFloatOrNull()
            } else {
                color = parseColor(part)
            }
        }

        if (size == null || color == null)
            return null
        return TextStroke(color, size)
    }

    private fun parseShadow(value: String): TextShadow? {
        var colorStr: String? = null
        var remainingStr = value

        val rgbRegex = Regex("""rgba?\s*\([^)]+\)""")
        val rgbMatch = rgbRegex.find(value)

        if (rgbMatch != null) {
            colorStr = rgbMatch.value
            remainingStr = value.replace(colorStr, "")
        } else {
            val parts = value.split("\\s+".toRegex())
            colorStr = parts.firstOrNull { it.startsWith("#") || it == "transparent" }
            if (colorStr != null) {
                remainingStr = value.replace(colorStr, "")
            }
        }

        if (colorStr == null)
            return null

        val color = parseColor(colorStr.trim()) ?: return null
        val numParts = remainingStr.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }

        if (numParts.size < 2)
            return null

        val dx = parsePx(numParts[0]) ?: return null
        val dy = parsePx(numParts[1]) ?: return null

        return TextShadow(color, dx, dy)
    }
}