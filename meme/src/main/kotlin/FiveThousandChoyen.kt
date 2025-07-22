package xyz.xszq.bot

import korlibs.image.bitmap.Bitmap
import korlibs.image.bitmap.NativeImage
import korlibs.image.bitmap.context2d
import korlibs.image.bitmap.extract
import korlibs.image.bitmap.sliceWithSize
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.image.font.FontRegistry
import korlibs.image.font.SystemFontRegistry
import korlibs.image.font.getTextBoundsWithGlyphs
import korlibs.image.vector.Context2d
import korlibs.math.geom.Point
import korlibs.math.geom.vector.LineCap
import korlibs.math.geom.vector.LineJoin
import korlibs.math.toIntCeil
import kotlinx.coroutines.runBlocking
import kotlin.math.max

class FiveThousandChoyen {
    val registry: FontRegistry = runBlocking {
        SystemFontRegistry()
    }
    fun draw(top: String, bottom: String ?= null): Bitmap {
        var rightBorder = 1500.0
        val result = NativeImage(1500, 270).context2d {
            setTransform(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
            fillStyle = Colors.WHITE
            fillRect(0, 0, width, height)

            lineJoin = LineJoin.ROUND
            lineCap = LineCap.ROUND

            rightBorder = drawTop(top)
            bottom ?.let {
                rightBorder = max(rightBorder, drawBottom(bottom))
            }
        }
        return result.sliceWithSize(0, 0, rightBorder.toIntCeil(), result.height).extract()
    }
    fun Context2d.drawTop(
        top: String,
        x: Int = TOP_X,
        y: Int = TOP_Y
    ): Double {
        font = registry[TOP_FONT]
        fontSize = SIZE
        setTransform(1.0, 0.0, -0.45, 1.0, 0.0, 0.0)

        // Black
        strokeStyle = Colors.BLACK
        lineWidth = 22.0
        strokeText(top, Point(x + 4, y + 4))
        // Silver
        strokeStyle = createLinearGradient(0, 24, 0, 122) {
            add(0.0, RGBA(0, 15, 36))
            add(0.10, RGBA(255, 255, 255))
            add(0.18,  RGBA(55, 58, 59))
            add(0.25,  RGBA(55, 58, 59))
            add(0.5,  RGBA(200, 200, 200))
            add(0.75,  RGBA(55, 58, 59))
            add(0.85,  RGBA(25, 20, 31))
            add(0.91,  RGBA(240, 240, 240))
            add(0.95,  RGBA(166, 175, 194))
            add(1.0,  RGBA(50, 50, 50))
        }
        lineWidth = 20.0
        strokeText(top, Point(x + 4, y + 4))
        
        // Black
        strokeStyle = Colors.BLACK
        lineWidth = 16.0
        strokeText(top, Point(x, y))

        // Gold
        strokeStyle = createLinearGradient(0, 20, 0, 100) {
            add(0.0, RGBA(253, 241, 0))
            add(0.25, RGBA(245, 253, 187))
            add(0.4, RGBA(255, 255, 255))
            add(0.75, RGBA(253, 219, 9))
            add(0.9, RGBA(127, 53, 0))
            add(1.0, RGBA(243, 196, 11))
        }
        lineWidth = 10.0
        strokeText(top, Point(x, y))

        // Black
        strokeStyle = Colors.BLACK
        lineWidth = 6.0
        strokeText(top, Point(x + 2, y - 3))

        // White
        strokeStyle = Colors.WHITE
        lineWidth = 6.0
        strokeText(top, Point(x, y - 3))

        // Red
        strokeStyle = createLinearGradient(0, 20, 0, 100) {
            add(0.0, RGBA(255, 100, 0))
            add(0.5, RGBA(123, 0, 0))
            add(0.51, RGBA(240, 0, 0))
            add(1.0, RGBA(5, 0, 0))
        }
        lineWidth = 4.0
        strokeText(top, Point(x, y - 3))

        // Red
        fillStyle = createLinearGradient(0, 20, 0, 100) {
            add(0.0, RGBA(230, 0, 0))
            add(0.5, RGBA(123, 0, 0))
            add(0.51, RGBA(240, 0, 0))
            add(1.0, RGBA(5, 0, 0))
        }
        fillText(top, Point(x, y - 3))

        return font!!.getTextBoundsWithGlyphs(fontSize, top).metrics.width + TOP_X
    }

    fun Context2d.drawBottom(
        bottom: String,
        x: Int = BOTTOM_X,
        y: Int = BOTTOM_Y
    ): Double {
        font = registry[BOTTOM_FONT]
        fontSize = SIZE
        setTransform(1.0, 0.0, -0.45, 1.0, 0.0, 0.0)

        // Black
        strokeStyle = Colors.BLACK
        lineWidth = 22.0
        strokeText(bottom, Point(x + 5, y + 2))

        // Silver
        strokeStyle = createLinearGradient(0, y - 80, 0, y + 18) {
            add(0.0, RGBA(0, 15, 36))
            add(0.25, RGBA(250, 250, 250))
            add(0.5, RGBA(150, 150, 150))
            add(0.75, RGBA(55, 58, 59))
            add(0.85, RGBA(25, 20, 31))
            add(0.91, RGBA(240, 240, 240))
            add(0.95, RGBA(166, 175, 194))
            add(1.0, RGBA(50, 50, 50))
        }
        lineWidth = 19.0
        strokeText(bottom, Point(x + 5, y + 2))

        // Black
        strokeStyle = RGBA(16, 25, 58)
        lineWidth = 17.0
        strokeText(bottom, Point(x, y))

        // White
        strokeStyle = RGBA(221, 221, 221)
        lineWidth = 8.0
        strokeText(bottom, Point(x, y))

        // Navy blue
        strokeStyle = createLinearGradient(0, y - 80, 0, y) {
            add(0.0, RGBA(16, 25, 58))
            add(0.03, RGBA(255, 255, 255))
            add(0.08, RGBA(16, 25, 58))
            add(0.2, RGBA(16, 25, 58))
            add(1.0, RGBA(16, 25, 58))
        }
        lineWidth = 7.0
        strokeText(bottom, Point(x, y))

        // Silver
        fillStyle = createLinearGradient(0, y - 80, 0, y) {
            add(0.0, RGBA(245, 246, 248))
            add(0.15, RGBA(255, 255, 255))
            add(0.35, RGBA(195, 213, 220))
            add(0.5, RGBA(160, 190, 201))
            add(0.51, RGBA(160, 190, 201))
            add(0.52, RGBA(196, 215, 222))
            add(1.0, RGBA(255, 255, 255))
        }
        fillText(bottom, Point(x, y - 3))
        return font!!.getTextBoundsWithGlyphs(SIZE, bottom).metrics.width + x - 50
    }
    companion object {
        const val TOP_FONT = "Source Han Sans CN Bold"
        const val BOTTOM_FONT = "Source Han Serif SC Bold"
        const val SIZE = 100.0
        const val TOP_X = 70
        const val TOP_Y = 100
        const val BOTTOM_X = 250
        const val BOTTOM_Y = 230
    }
}