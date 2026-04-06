import korlibs.image.bitmap.Bitmap32
import korlibs.image.font.nativeSystemFontProvider
import korlibs.image.format.PNG
import korlibs.image.format.readBitmap
import korlibs.image.format.showImageAndWait
import korlibs.image.format.writeBitmap
import korlibs.io.file.std.localVfs
import kotlinx.coroutines.runBlocking
import xyz.xszq.bot.BlueArchiveLogo
import xyz.xszq.bot.FiveThousandChoyen
import xyz.xszq.bot.sekai.SekaiCharacter
import xyz.xszq.bot.sekai.SekaiSticker
import xyz.xszq.bot.sekai.SekaiText
import kotlin.math.*

suspend fun listFonts() {
    nativeSystemFontProvider().listFontNames().forEach {
        println(it)
    }
}
suspend fun testSekai() {
    SekaiSticker().draw(SekaiCharacter(
        "62",
        "Emu 13",
        "emu",
        "emu/Emu_13.png",
        "#FF66BB",
        SekaiText(
            "Wonderhoy!",
            148,
            70,
            -2,
            38
        )
    ), "Wonderhoy!\nWonderhoy2!").showImageAndWait()
}
suspend fun testBA() {
    BlueArchiveLogo().draw("a", "b").showImageAndWait()
}
suspend fun test5k() {
    FiveThousandChoyen().draw("1", "b").showImageAndWait()
}
suspend fun testSpherize() {
    val inputBmp = localVfs("E:/Temp/input.png").readBitmap(PNG)
    val src = inputBmp.toBMP32IfRequired()

    val width = src.width
    val height = src.height
    val cx = width  / 2.0
    val cy = height / 2.0
    val maxRad = min(cx, cy)

    listOf(
        true  to "E:/Temp/bulge.png",
        false to "E:/Temp/sink.png"
    ).forEach { (bulge, filename) ->
        val out = Bitmap32(width, height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = (x - cx) / maxRad
                val dy = (y - cy) / maxRad
                val r  = hypot(dx, dy)

                if (r <= 1.0) {
                    val rMapped = if (bulge) {
                        asin(r) * 2.0 / PI
                    } else {
                        sin(r * PI / 2.0)
                    }
                    val scale = if (r != 0.0) rMapped / r else 1.0

                    val srcX = cx + dx * scale * maxRad
                    val srcY = cy + dy * scale * maxRad

                    val color = src.getRgbaSampled(srcX.toFloat(), srcY.toFloat())
                    out.setRgba(x, y, color)
                } else {
                    val color = src.getRgbaClamped(x, y)
                    out.setRgba(x, y, color)
                }
            }
        }

        localVfs(filename).writeBitmap(out, PNG)
    }
}

fun main() {
    runBlocking {
        testSpherize()
    }
}