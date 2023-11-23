@file:Suppress("MemberVisibilityCanBePrivate", "SpellCheckingInspection")

package xyz.xszq.bot

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.NativeImageOrBitmap32
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.bitmap.sliceWithSize
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.font.TtfFont
import com.soywiz.korim.font.measureTextGlyphs
import com.soywiz.korim.vector.Context2d
import com.soywiz.korio.file.std.localCurrentDirVfs
import com.soywiz.korma.geom.vector.LineJoin
import xyz.xszq.bot.maimai.MultiPlatformNativeSystemFontProvider
import java.lang.Double.max

object FiveThousandChoyen {
    val topFont: String = "Source Han Sans CN Bold"
    val bottomFont: String = "Source Han Serif SC Bold"
    val transparency: Boolean = false
    val size: Double = 100.0
    val topX = 70
    val topY = 100
    val bottomX = 250
    val bottomY = 230
    val fonts = MultiPlatformNativeSystemFontProvider(localCurrentDirVfs["font"].absolutePath)
    fun generate(top: String, bottom: String): Bitmap {
        val result = NativeImageOrBitmap32(1500, 270, true)
        var rightBorder = 1500.0
        result.context2d {
            setTransform(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
            fillStyle = RGBA(255, 255, 255, if (transparency) 0 else 255)
            fillRect(0, 0, width, height)
            setTransform(1.0, 0.0, -0.45, 1.0, 0.0, 0.0)
            lineJoin = LineJoin.ROUND
            generateTop(top,this, fonts.loadFontByName(topFont)!!) ?.let {
                rightBorder = it
            }
            generateBottom(bottom, this, fonts.loadFontByName(bottomFont)!!) ?.let {
                rightBorder = max(rightBorder, it)
            }
            dispose()
        }
        return result.sliceWithSize(0, 0, (rightBorder + 30).toInt(), result.height).extract()
    }
    fun generateTop(top: String, ctx: Context2d, nowFont: TtfFont, x: Int = topX, y: Int = topY) = ctx.run {
        font = nowFont
        fontSize = size
        // Black
        run {
            strokeStyle = Colors.BLACK
            lineWidth = 22.0
            strokeText(top, x + 4, y + 4)
        }
        // Silver
        run {
            strokeStyle = createLinearGradient(0, -82, 0, 18) {
                add(0.0, RGBA(0, 15, 36))
                add(0.10, Colors.WHITE)
                add(0.18,  RGBA(55,58,59))
                add(0.25,  RGBA(55,58,59))
                add(0.5,  RGBA(200,200,200))
                add(0.75,  RGBA(55,58,59))
                add(0.85,  RGBA(25,20,31))
                add(0.91,  RGBA(240,240,240))
                add(0.95,  RGBA(166,175,194))
                add(1.0,  RGBA(50,50,50))
            }
            lineWidth = 20.0
            strokeText(top, x + 4, y + 4)
        }
        // Black
        run {
            strokeStyle = Colors.BLACK
            lineWidth = 16.0
            strokeText(top, x, y)
        }
        // Gold
        run {
            strokeStyle = createLinearGradient(0, -80, 0, 0) {
                add(0.0, RGBA(253, 241, 0))
                add(0.25, RGBA(245, 253, 187))
                add(0.4, Colors.WHITE)
                add(0.75, RGBA(253, 219, 9))
                add(0.9, RGBA(127, 53, 0))
                add(1.0, RGBA(243, 196, 11))
            }
            lineWidth = 10.0
            strokeText(top, x, y)
        }
        // Black
        run {
            strokeStyle = Colors.BLACK
            lineWidth = 6.0
            strokeText(top, x + 2, y - 3)
        }
        // White
        run {
            strokeStyle = Colors.WHITE
            lineWidth = 6.0
            strokeText(top, x, y - 3)
        }
        // Red
        run {
            strokeStyle = createLinearGradient(0, -80, 0, 0) {
                add(0.0, RGBA(255, 100, 0))
                add(0.5, RGBA(123, 0, 0))
                add(0.51, RGBA(240, 0, 0))
                add(1.0, RGBA(5, 0, 0))
            }
            lineWidth = 4.0
            strokeText(top, x, y - 3)
        }
        // Red
        run {
            fillStyle = createLinearGradient(0, -80, 0, 0) {
                add(0.0, RGBA(230, 0, 0))
                add(0.5, RGBA(123, 0, 0))
                add(0.51, RGBA(240, 0, 0))
                add(1.0, RGBA(5, 0, 0))
            }
            fillText(top, x, y - 3)
        }
        font?.measureTextGlyphs(fontSize, top)?.glyphs?.maxByOrNull { it.x } ?.let {
            x + it.x + it.metrics.bounds.width
        }
    }
    fun generateBottom(bottom: String, ctx: Context2d, nowFont: TtfFont, x: Int = bottomX, y: Int = bottomY) = ctx.run {
        font = nowFont
        fontSize = size
        // Black
        run {
            strokeStyle = Colors.BLACK
            lineWidth = 22.0
            strokeText(bottom, x + 5, y + 2)
        }
        // Silver
        run {
            strokeStyle = createLinearGradient(0, -80, 0, 18) {
                add(0.0, RGBA(0,15,36))
                add(0.25, RGBA(250,250,250))
                add(0.5, RGBA(150,150,150))
                add(0.75, RGBA(55,58,59))
                add(0.85, RGBA(25,20,31))
                add(0.91, RGBA(240,240,240))
                add(0.95, RGBA(166,175,194))
                add(1.0, RGBA(50,50,50))
            }
            lineWidth = 19.0
            strokeText(bottom, x + 5, y + 2)
        }
        // Black
        run {
            strokeStyle = RGBA(16, 25, 58)
            lineWidth = 17.0
            strokeText(bottom, x, y)
        }
        // White
        run {
            strokeStyle = RGBA(221, 221, 221)
            lineWidth = 8.0
            strokeText(bottom, x, y)
        }
        // Navy blue
        run {
            strokeStyle = createLinearGradient(0, -80, 0, 0) {
                add(0.0, RGBA(16, 25, 58))
                add(0.03, RGBA(255, 255, 255))
                add(0.08, RGBA(16, 25, 58))
                add(0.2, RGBA(16, 25, 58))
                add(1.0, RGBA(16, 25, 58))
            }
            lineWidth = 7.0
            strokeText(bottom, x, y)
        }
        // Silver
        run {
            fillStyle = createLinearGradient(0, - 80, 0, 0) {
                add(0.0, RGBA(245, 246, 248))
                add(0.15, RGBA(255, 255, 255))
                add(0.35, RGBA(195, 213, 220))
                add(0.5, RGBA(160, 190, 201))
                add(0.51, RGBA(160, 190, 201))
                add(0.52, RGBA(196, 215, 222))
                add(1.0, RGBA(255, 255, 255))
            }
            fillText(bottom, x, y - 3)
        }
        font?.measureTextGlyphs(fontSize, bottom)?.glyphs?.maxByOrNull { it.x } ?.let {
            x + it.x + it.metrics.bounds.width
        }
    }
}
