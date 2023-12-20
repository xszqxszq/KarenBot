@file:Suppress("MemberVisibilityCanBePrivate", "SpellCheckingInspection")

package xyz.xszq.bot.image

import korlibs.image.bitmap.*
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.image.font.TtfFont
import korlibs.image.vector.Context2d
import korlibs.math.geom.Point
import korlibs.math.geom.vector.LineJoin
import kotlinx.coroutines.sync.withPermit

object FiveThousandChoyen {
    val topFont: TtfFont = globalFontRegistry.loadFontByName("Source Han Sans CN Bold")!!
    val bottomFont: TtfFont = globalFontRegistry.loadFontByName("Source Han Serif SC Bold")!!
    const val transparency: Boolean = false
    const val size: Double = 100.0
    const val topX = 70
    const val topY = 100
    const val bottomX = 250
    const val bottomY = 230

    suspend fun generate(top: String, bottom: String): Bitmap = MemeGenerator.semaphore.withPermit {
        kotlin.runCatching {
            val result = NativeImageOrBitmap32(1500, 270, true)
            var rightBorder = 1500.0F
            result.context2d {
                setTransform(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
                fillStyle = RGBA(255, 255, 255, if (transparency) 0 else 255)
                fillRect(0, 0, width, height)
                setTransform(1.0, 0.0, -0.45, 1.0, 0.0, 0.0)
                lineJoin = LineJoin.ROUND
                rightBorder = generateTop(top,this, topFont)!!
                rightBorder = kotlin.math.max(rightBorder,
                    generateBottom(bottom, this, bottomFont)!!
                )
                dispose()
            }
            result.sliceWithSize(0, 0, (rightBorder + 30).toInt(), result.height).extract()
        }.onFailure {
            it.printStackTrace()
        }
    }.getOrThrow()
    suspend fun generateTop(top: String, ctx: Context2d, nowFont: TtfFont, x: Int = topX, y: Int = topY) = ctx.run {
        font = nowFont
        fontSize = size.toFloat()
        // Black
        run {
            strokeStyle = Colors.BLACK
            lineWidth = 22.0F
            strokeTextSafe(top, Point(x + 4, y + 4))
        }
        // Silver
        run {
            strokeStyle = createLinearGradient(0, 24, 0, 122) {
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
            lineWidth = 20.0F
            strokeTextSafe(top, Point(x + 4, y + 4))
        }
        // Black
        run {
            strokeStyle = Colors.BLACK
            lineWidth = 16.0F
            strokeTextSafe(top, Point(x, y))
        }
        // Gold
        run {
            strokeStyle = createLinearGradient(0, 20, 0, 100) {
                add(0.0, RGBA(253, 241, 0))
                add(0.25, RGBA(245, 253, 187))
                add(0.4, Colors.WHITE)
                add(0.75, RGBA(253, 219, 9))
                add(0.9, RGBA(127, 53, 0))
                add(1.0, RGBA(243, 196, 11))
            }
            lineWidth = 10.0F
            strokeTextSafe(top, Point(x, y))
        }
        // Black
        run {
            strokeStyle = Colors.BLACK
            lineWidth = 6.0F
            strokeTextSafe(top, Point(x + 2, y - 3))
        }
        // White
        run {
            strokeStyle = Colors.WHITE
            lineWidth = 6.0F
            strokeTextSafe(top, Point(x, y - 3))
        }
        // Red
        run {
            strokeStyle = createLinearGradient(0, 20, 0, 100) {
                add(0.0, RGBA(255, 100, 0))
                add(0.5, RGBA(123, 0, 0))
                add(0.51, RGBA(240, 0, 0))
                add(1.0, RGBA(5, 0, 0))
            }
            lineWidth = 4.0F
            strokeTextSafe(top, Point(x, y - 3))
        }
        // Red
        run {
            fillStyle = createLinearGradient(0, 20, 0, 100) {
                add(0.0, RGBA(230, 0, 0))
                add(0.5, RGBA(123, 0, 0))
                add(0.51, RGBA(240, 0, 0))
                add(1.0, RGBA(5, 0, 0))
            }
            fillTextSafe(top, Point(x, y - 3))
        }
        font?.measureTextGlyphs(fontSize, top)?.glyphs?.maxByOrNull { it.pos.x } ?.let {
            x + it.pos.x + it.metrics.bounds.width
        }
    }
    suspend fun generateBottom(bottom: String, ctx: Context2d, nowFont: TtfFont, x: Int = bottomX, y: Int = bottomY) = ctx.run {
        font = nowFont
        fontSize = size.toFloat()
        // Black
        run {
            strokeStyle = Colors.BLACK
            lineWidth = 22.0F
            strokeTextSafe(bottom, Point(x + 5, y + 2))
        }
        // Silver
        run {
            strokeStyle = createLinearGradient(0, y -80, 0, y + 18) {
                add(0.0, RGBA(0,15,36))
                add(0.25, RGBA(250,250,250))
                add(0.5, RGBA(150,150,150))
                add(0.75, RGBA(55,58,59))
                add(0.85, RGBA(25,20,31))
                add(0.91, RGBA(240,240,240))
                add(0.95, RGBA(166,175,194))
                add(1.0, RGBA(50,50,50))
            }
            lineWidth = 19.0F
            strokeTextSafe(bottom, Point(x + 5, y + 2))
        }
        // Black
        run {
            strokeStyle = RGBA(16, 25, 58)
            lineWidth = 17.0F
            strokeTextSafe(bottom, Point(x, y))
        }
        // White
        run {
            strokeStyle = RGBA(221, 221, 221)
            lineWidth = 8.0F
            strokeTextSafe(bottom, Point(x, y))
        }
        // Navy blue
        run {
            strokeStyle = createLinearGradient(0, y -80, 0, y) {
                add(0.0, RGBA(16, 25, 58))
                add(0.03, RGBA(255, 255, 255))
                add(0.08, RGBA(16, 25, 58))
                add(0.2, RGBA(16, 25, 58))
                add(1.0, RGBA(16, 25, 58))
            }
            lineWidth = 7.0F
            strokeTextSafe(bottom, Point(x, y))
        }
        // Silver
        run {
            fillStyle = createLinearGradient(0, y - 80, 0, y ) {
                add(0.0, RGBA(245, 246, 248))
                add(0.15, RGBA(255, 255, 255))
                add(0.35, RGBA(195, 213, 220))
                add(0.5, RGBA(160, 190, 201))
                add(0.51, RGBA(160, 190, 201))
                add(0.52, RGBA(196, 215, 222))
                add(1.0, RGBA(255, 255, 255))
            }
            fillTextSafe(bottom, Point(x, y - 3))
        }
        font?.measureTextGlyphs(fontSize, bottom)?.glyphs?.maxByOrNull { it.pos.x } ?.let {
            x + it.pos.x + it.metrics.bounds.width
        }
    }
}
