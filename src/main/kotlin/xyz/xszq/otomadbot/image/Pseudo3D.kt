package xyz.xszq.otomadbot.image

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.NativeImageOrBitmap32
import com.soywiz.korma.geom.PointInt

// From https://stackoverflow.com/questions/24341033/texture-deforming-4-points
object Pseudo3D {
    fun computeImage(
        image: Bitmap,
        p0: PointInt, p1: PointInt, p2: PointInt, p3: PointInt
    ): Bitmap {
        val w = image.width
        val h = image.height
        val result = NativeImageOrBitmap32(w + 50, h + 50)
        val ip0 = PointInt(0, 0)
        val ip1 = PointInt(0, h)
        val ip2 = PointInt(w, h)
        val ip3 = PointInt(w, 0)
        val m = computeProjectionMatrix(arrayOf(p0, p1, p2, p3), arrayOf(ip0, ip1, ip2, ip3))
        val mInv = Matrix3D(m)
        mInv.invert()
        for (y in 0 until result.height) {
            for (x in 0 until result.width) {
                val p = PointInt(x, y)
                mInv.transform(p)
                val ix = p.x
                val iy = p.y
                if (ix in 0 until w && iy >= 0 && iy < h) {
                    result.setRgba(x, y, image.getRgba(ix, iy))
                }
            }
        }
        return result
    }

    // From https://math.stackexchange.com/questions/296794
    private fun computeProjectionMatrix(p0: Array<PointInt>, p1: Array<PointInt>): Matrix3D {
        val m0 = computeProjectionMatrix(p0)
        val m1 = computeProjectionMatrix(p1)
        m1.invert()
        m0.mul(m1)
        return m0
    }

    // From https://math.stackexchange.com/questions/296794
    private fun computeProjectionMatrix(p: Array<PointInt>): Matrix3D {
        val m = Matrix3D(
            p[0].x, p[1].x, p[2].x,
            p[0].y, p[1].y, p[2].y,
            1.0, 1.0, 1.0
        )
        val p3 = Point3D(p[3].x, p[3].y, 1.0)
        val mInv = Matrix3D(m)
        mInv.invert()
        mInv.transform(p3)
        m.m00 *= p3.x
        m.m01 *= p3.y
        m.m02 *= p3.z
        m.m10 *= p3.x
        m.m11 *= p3.y
        m.m12 *= p3.z
        m.m20 *= p3.x
        m.m21 *= p3.y
        m.m22 *= p3.z
        return m
    }
    private class Point3D(var x: Double, var y: Double, var z: Double) {
        constructor(x: Number, y: Number, z: Number) : this(x.toDouble(), y.toDouble(), z.toDouble())
    }
    private class Matrix3D {
        var m00: Double
        var m01: Double
        var m02: Double
        var m10: Double
        var m11: Double
        var m12: Double
        var m20: Double
        var m21: Double
        var m22: Double

        constructor(
            m00: Number, m01: Number, m02: Number,
            m10: Number, m11: Number, m12: Number,
            m20: Number, m21: Number, m22: Number
        ) {
            this.m00 = m00.toDouble()
            this.m01 = m01.toDouble()
            this.m02 = m02.toDouble()
            this.m10 = m10.toDouble()
            this.m11 = m11.toDouble()
            this.m12 = m12.toDouble()
            this.m20 = m20.toDouble()
            this.m21 = m21.toDouble()
            this.m22 = m22.toDouble()
        }

        constructor(m: Matrix3D) {
            m00 = m.m00
            m01 = m.m01
            m02 = m.m02
            m10 = m.m10
            m11 = m.m11
            m12 = m.m12
            m20 = m.m20
            m21 = m.m21
            m22 = m.m22
        }

        // From http://www.dr-lex.be/random/matrix_inv.html
        fun invert() {
            val invDet = 1.0 / determinant()
            val nm00 = m22 * m11 - m21 * m12
            val nm01 = -(m22 * m01 - m21 * m02)
            val nm02 = m12 * m01 - m11 * m02
            val nm10 = -(m22 * m10 - m20 * m12)
            val nm11 = m22 * m00 - m20 * m02
            val nm12 = -(m12 * m00 - m10 * m02)
            val nm20 = m21 * m10 - m20 * m11
            val nm21 = -(m21 * m00 - m20 * m01)
            val nm22 = m11 * m00 - m10 * m01
            m00 = nm00 * invDet
            m01 = nm01 * invDet
            m02 = nm02 * invDet
            m10 = nm10 * invDet
            m11 = nm11 * invDet
            m12 = nm12 * invDet
            m20 = nm20 * invDet
            m21 = nm21 * invDet
            m22 = nm22 * invDet
        }

        // From http://www.dr-lex.be/random/matrix_inv.html
        fun determinant(): Double {
            return m00 * (m11 * m22 - m12 * m21) + m01 * (m12 * m20 - m10 * m22) + m02 * (m10 * m21 - m11 * m20)
        }

        fun transform(p: Point3D) {
            val x = m00 * p.x + m01 * p.y + m02 * p.z
            val y = m10 * p.x + m11 * p.y + m12 * p.z
            val z = m20 * p.x + m21 * p.y + m22 * p.z
            p.x = x
            p.y = y
            p.z = z
        }

        fun transform(pp: PointInt) {
            val p = Point3D(pp.x, pp.y, 1.0)
            transform(p)
            pp.setTo((p.x / p.z).toInt(), (p.y / p.z).toInt())
        }

        fun mul(m: Matrix3D) {
            val nm00 = m00 * m.m00 + m01 * m.m10 + m02 * m.m20
            val nm01 = m00 * m.m01 + m01 * m.m11 + m02 * m.m21
            val nm02 = m00 * m.m02 + m01 * m.m12 + m02 * m.m22
            val nm10 = m10 * m.m00 + m11 * m.m10 + m12 * m.m20
            val nm11 = m10 * m.m01 + m11 * m.m11 + m12 * m.m21
            val nm12 = m10 * m.m02 + m11 * m.m12 + m12 * m.m22
            val nm20 = m20 * m.m00 + m21 * m.m10 + m22 * m.m20
            val nm21 = m20 * m.m01 + m21 * m.m11 + m22 * m.m21
            val nm22 = m20 * m.m02 + m21 * m.m12 + m22 * m.m22
            m00 = nm00
            m01 = nm01
            m02 = nm02
            m10 = nm10
            m11 = nm11
            m12 = nm12
            m20 = nm20
            m21 = nm21
            m22 = nm22
        }
    }
}
