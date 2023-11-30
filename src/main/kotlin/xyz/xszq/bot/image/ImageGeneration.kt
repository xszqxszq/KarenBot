package xyz.xszq.bot.image

import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.encode
import xyz.xszq.nereides.NetworkUtils
import xyz.xszq.nereides.readAsImage
import kotlin.math.*

object ImageGeneration {
    suspend fun flipImage(url: String): List<ByteArray> {
        val file = NetworkUtils.downloadTempFile(url)
        val img = file!!.readAsImage()
        val flipped = img.clone().flipX()

        val resultA = img.clone()
        val resultB = img.clone()

        flipped.copy(0, 0, resultA, 0, 0, img.width / 2, img.height)
        flipped.copy(img.width - img.width / 2, 0, resultB, img.width - img.width / 2, 0, img.width / 2, img.height)

        return listOf(resultA.encode(PNG), resultB.encode(PNG))
    }
    suspend fun spherize(url: String, a: Double = 1.0, b: Double = 3.0, c: Double = -9.0): ByteArray {
        val file = NetworkUtils.downloadTempFile(url)
        val img = file!!.readAsImage()
        val d = 1.0 - a - b - c
        val radius = min(img.width, img.height) / 2
        val result = Bitmap32(img.width, img.height) { x, y ->
            val midX = (img.width - 1) / 2.0
            val midY = (img.height - 1) / 2.0
            val dX = x - midX
            val dY = y - midY
            val dstR = sqrt((dX * dX + dY * dY) / radius / radius)
            val factor = abs(1.0 / (a * dstR * dstR * dstR + b * dstR * dstR + c * dstR + d))
            val srcX = (midX + dX * factor).toInt()
            val srcY = (midY + dY * factor).toInt()
            img[srcX, srcY]
        }
        return result.encode(PNG)
    }

    suspend fun pincushion(url: String, strength: Double = 7.0, zoom: Double = 1.5): ByteArray {
        val file = NetworkUtils.downloadTempFile(url)
        val img = file!!.readAsImage()
        val midW = img.width / 2.0
        val midH = img.height / 2.0
        val correctionRadius = sqrt(img.width.toDouble().pow(2) + img.height.toDouble().pow(2)) / strength
        return Bitmap32(img.width, img.height) { x, y ->
            val newX = x - midW
            val newY = y - midH
            val dis = sqrt(newX.pow(2) + newY.pow(2))
            val r = dis / correctionRadius
            val theta = if (r == 0.0) 1.0 else atan(r) / r
            val srcX = midW + theta * newX * zoom
            val srcY = midH + theta * newY * zoom
            img[srcX.toInt(), srcY.toInt()]
        }.encode(PNG)
    }
}