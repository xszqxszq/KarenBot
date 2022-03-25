@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package xyz.xszq.otomadbot.image

import com.soywiz.kds.iterators.fastForEachWithIndex
import com.soywiz.kmem.toByte
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.resized
import com.soywiz.korim.color.RGBA
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.ScaleMode
import java.math.BigInteger

interface ImageHash {
    val toWidth: Int
        get() = 8
    val toHeight: Int
        get() = 8
    fun calc(image: Bitmap32): BigInteger
    fun compare(a: Bitmap32, b: Bitmap32) = similarity(calc(a), calc(b))
    fun similarity(a: BigInteger, b: BigInteger): Double
}

fun Bitmap32.toGrayScale() = clone().apply {
    data.fastForEachWithIndex { index, value ->
        val gray = .2126 * value.rd + .7152 * value.gd + .0722 * value.bd
        data[index] = RGBA.float(gray, gray, gray, 1.0)
    }
}

// Reference: https://github.com/KilianB/JImageHash/blob/master/src/main/java/dev/brachtendorf/jimagehash/hashAlgorithms/DifferenceHash.java
class DifferenceHash(val precision: Precision): ImageHash {
    enum class Precision {
        Simple, Double, Triple
    }
    override fun calc(image: Bitmap32): BigInteger {
        var hash = byteArrayOf()
        image.toGrayScale().resized(toWidth, toHeight, ScaleMode.EXACT, Anchor.CENTER).toBMP32().run {
            (1 until width).forEach { x ->
                (0 until height).forEach { y ->
                    hash = hash.plus((this[x, y].r < this[x - 1, y].r).toByte())
                }
            }
            if (precision != Precision.Simple) {
                (0 until width).forEach { x ->
                    (1 until height).forEach { y ->
                        hash = hash.plus((this[x, y].r >= this[x, y - 1].r).toByte())
                    }
                }
            }
            if (precision == Precision.Triple) {
                (1 until width).forEach { x ->
                    (1 until height).forEach { y ->
                        hash = hash.plus((this[x, y].r >= this[x - 1, y - 1].r).toByte())
                    }
                }
            }
        }
        return BigInteger(hash)
    }

    override fun similarity(a: BigInteger, b: BigInteger) = when (precision) {
        Precision.Triple -> (192 - (a xor b).bitCount()) / 192.0
        Precision.Double -> (128 - (a xor b).bitCount()) / 128.0
        else -> (64 - (a xor b).bitCount()) / 64.0
    }
}
val differenceHashTriple = DifferenceHash(DifferenceHash.Precision.Triple)