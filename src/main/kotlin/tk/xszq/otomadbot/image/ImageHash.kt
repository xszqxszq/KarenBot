@file:Suppress("unused")

package tk.xszq.otomadbot.image

import com.soywiz.kds.iterators.fastForEachWithIndex
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.resized
import com.soywiz.korim.color.RGBA
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.ScaleMode

open class ImageHash {
    companion object {
        fun similarity(a: Long, b: Long) = (64 - (a xor b).countOneBits()) / 64.0
    }
}

fun Bitmap32.toGrayScale() = clone().apply {
    data.fastForEachWithIndex { index, value ->
        val gray = .2126 * value.rd + .7152 * value.gd + .0722 * value.bd
        data[index] = RGBA.float(gray, gray, gray, 1.0)
    }
}

object DifferenceHash: ImageHash() {
    fun calc(image: Bitmap32): Long {
        var hash = 0L
        image.toGrayScale().resized(8, 8, ScaleMode.EXACT, Anchor.CENTER).toBMP32().run {
            var prev = last().r
            forEach { n, _, y ->
                hash = hash shl 1
                if ((y % 2 == 0 && data[n].r >= prev) || (y % 2 != 0 && prev >= data[n].r))
                    hash = hash or 1
                prev = data[n].r
            }
        }
        return hash
    }
    fun compare(a: Bitmap32, b: Bitmap32) = similarity(calc(a), calc(b))
}