package xyz.xszq.bot.crypto

import java.security.MessageDigest

/**
 * Ed25519 签名与验签
 *
 * 移植自 BouncyCastle
 */
object Ed25519 {
    const val PUBLIC_KEY_SIZE = 32
    const val SECRET_KEY_SIZE = 32
    const val SIGNATURE_SIZE = 64

    private const val COORD_INTS = 8
    private const val POINT_BYTES = COORD_INTS * 4
    private const val SCALAR_INTS = 8
    private const val SCALAR_BYTES = SCALAR_INTS * 4

    private const val DIGEST_SIZE = 64

    // -x^2+y^2 = 1+d*x^2*y^2

    private val pLimbs = intArrayOf(
        0xFFFFFFED.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(),
        0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(),
        0xFFFFFFFF.toInt(), 0x7FFFFFFF
    )

    private val order8Y1 = intArrayOf(
        0x706A17C7, 0x4FD84D3D, 0x760B3CBA, 0x0F67100D, 0xFA53202A.toInt(),
        0xC6CC392C.toInt(), 0x77FDC74E, 0x7A03AC92
    )
    private val order8Y2 = intArrayOf(
        0x8F95E826.toInt(), 0xB027B2C2.toInt(), 0x89F4C345.toInt(),
        0xF098EFF2.toInt(), 0x05ACDFD5, 0x3933C6D3,
        0x880238B1.toInt(), 0x05FC536D
    )

    private val bX = intArrayOf(
        0x0325D51A, 0x018B5823, 0x007B2C95, 0x0304A92D, 0x00D2598E,
        0x01D6DC5C, 0x01388C7F, 0x013FEC0A, 0x029E6B72, 0x0042D26D
    )
    private val bY = intArrayOf(
        0x02666658, 0x01999999, 0x00666666, 0x03333333, 0x00CCCCCC,
        0x02666666, 0x01999999, 0x00666666, 0x03333333, 0x00CCCCCC
    )

    private val b128X = intArrayOf(
        0x00B7E824, 0x0011EB98, 0x003E5FC8, 0x024E1739, 0x0131CD0B,
        0x014E29A0, 0x034E6138, 0x0132C952, 0x03F9E22F, 0x00984F5F
    )
    private val b128Y = intArrayOf(
        0x03F5A66B, 0x02AF4452, 0x0049E5BB, 0x00F28D26, 0x0121A17C,
        0x02C29C3A, 0x0047AD89, 0x0087D95F, 0x0332936E, 0x00BE5933
    )

    private val cD = intArrayOf(
        0x035978A3, 0x02D37284, 0x018AB75E, 0x026A0A0E, 0x0000E014,
        0x0379E898, 0x01D01E5D, 0x01E738CC, 0x03715B7F, 0x00A406D9
    )
    private val cD2 = intArrayOf(
        0x02B2F159, 0x01A6E509, 0x01156EBD, 0x00D4141D, 0x0001C029,
        0x02F3D130, 0x03A03CBB, 0x01CE7198, 0x02E2B6FF, 0x00480DB3
    )
    private val cD4 = intArrayOf(
        0x0165E2B2, 0x034DCA13, 0x002ADD7A, 0x01A8283B, 0x00038052,
        0x01E7A260, 0x03407977, 0x019CE331, 0x01C56DFF, 0x00901B67
    )

    private const val WNAF_WIDTH_128 = 4
    private const val WNAF_WIDTH_BASE = 6

    private const val PRECOMP_BLOCKS = 8
    private const val PRECOMP_TEETH = 4
    private const val PRECOMP_SPACING = 8
    private const val PRECOMP_POINTS = 8
    private const val PRECOMP_MASK = PRECOMP_POINTS - 1

    private lateinit var precompBaseWnaf: Array<PointPreComp>
    private lateinit var precompBase128Wnaf: Array<PointPreComp>
    private lateinit var precompBaseComb: IntArray
    private var precomputed = false

    private class PointAccum {
        val x = Field.create()
        val y = Field.create()
        val z = Field.create()
        val u = Field.create()
        val v = Field.create()
    }

    private class PointAffine {
        val x = Field.create()
        val y = Field.create()
    }

    private class PointExtended {
        val x = Field.create()
        val y = Field.create()
        val z = Field.create()
        val t = Field.create()
    }

    private class PointPreComp {
        val ymxH = Field.create()      // (y - x)/2
        val ypxH = Field.create()      // (y + x)/2
        val xyd = Field.create()        // x.y.d
    }

    private class PointPreCompZ {
        val ymxH = Field.create()      // (y - x)/2
        val ypxH = Field.create()      // (y + x)/2
        val xyd = Field.create()        // x.y.d
        val z = Field.create()
    }

    private class PointTemp {
        val r0 = Field.create()
        val r1 = Field.create()
    }

    /**
     * 由种子派生 Ed25519 公钥
     *
     * @param seed 私钥种子
     * @param seedOff 起始偏移
     * @param publicKey 输出公钥
     * @param publicKeyOff 写入偏移
     */
    @JvmStatic
    fun generatePublicKey(
        seed: ByteArray,
        seedOff: Int,
        publicKey: ByteArray,
        publicKeyOff: Int
    ) {
        Arrays.validateSegment(seed, seedOff, SECRET_KEY_SIZE)
        Arrays.validateSegment(publicKey, publicKeyOff, PUBLIC_KEY_SIZE)

        val h = ByteArray(DIGEST_SIZE)
        expandPrivateKey(createDigest(), seed, seedOff, h)

        val s = ByteArray(SCALAR_BYTES)
        pruneScalar(h, s)

        scalarMultBaseEncoded(s, publicKey, publicKeyOff)
    }

    /**
     * Ed25519 明文签名
     *
     * @param seed 私钥种子
     * @param seedOff 起始偏移
     * @param message 消息
     * @param messageOff 消息起始偏移
     * @param messageLen 消息长度
     * @param signature 输出签名
     * @param signatureOff 位置偏移
     */
    @JvmStatic
    fun sign(
        seed: ByteArray,
        seedOff: Int,
        message: ByteArray,
        messageOff: Int,
        messageLen: Int,
        signature: ByteArray,
        signatureOff: Int
    ) {
        Arrays.validateSegment(seed, seedOff, SECRET_KEY_SIZE)
        Arrays.validateSegment(message, messageOff, messageLen)
        Arrays.validateSegment(signature, signatureOff, SIGNATURE_SIZE)

        implSign(
            sk = seed,
            skOff = seedOff,
            m = message,
            mOff = messageOff,
            mLen = messageLen,
            sig = signature,
            sigOff = signatureOff
        )
    }

    /**
     * Ed25519 明文验签
     *
     * @param publicKey 公钥
     * @param publicKeyOff 起始偏移
     * @param message 消息
     * @param messageOff 消息起始偏移
     * @param messageLen 消息长度
     * @param signature 64 字节签名
     * @param signatureOff 签名起始偏移
     * @return 签名是否有效
     */
    @JvmStatic
    fun verify(
        publicKey: ByteArray,
        publicKeyOff: Int,
        message: ByteArray,
        messageOff: Int,
        messageLen: Int,
        signature: ByteArray,
        signatureOff: Int
    ): Boolean {
        Arrays.validateSegment(publicKey, publicKeyOff, PUBLIC_KEY_SIZE)
        Arrays.validateSegment(message, messageOff, messageLen)
        Arrays.validateSegment(signature, signatureOff, SIGNATURE_SIZE)

        return implVerify(
            sig = signature,
            sigOff = signatureOff,
            pk = publicKey,
            pkOff = publicKeyOff,
            m = message,
            mOff = messageOff,
            mLen = messageLen
        )
    }

    private fun calculateS(
        r: ByteArray,
        k: ByteArray,
        s: ByteArray
    ): ByteArray {
        val t = IntArray(SCALAR_INTS * 2)
        Scalar25519.decode(r, t)
        val u = IntArray(SCALAR_INTS)
        Scalar25519.decode(k, u)
        val v = IntArray(SCALAR_INTS)
        Scalar25519.decode(s, v)

        Nat256.mulAddTo(u, v, t)

        val result = ByteArray(SCALAR_BYTES * 2)
        Codec.encode32(t, 0, t.size, result, 0)
        return Scalar25519.reduce512(result)
    }

    private fun checkPoint(p: PointAffine): Int {
        val t = Field.create()
        val u = Field.create()
        val v = Field.create()

        Field.sqr(p.x, u)
        Field.sqr(p.y, v)
        Field.mul(u, v, t)
        Field.sub(u, v, u)
        Field.mul(t, cD, t)
        Field.addOne(t)
        Field.add(t, u, t)
        Field.normalize(t)
        Field.normalize(v)

        return Field.isZero(t) and Field.isZero(v).inv()
    }

    private fun checkPointFullVar(p: ByteArray): Boolean {
        val y7 = Codec.decode32(p, 28) and 0x7FFFFFFF

        var t0 = y7
        var t1 = y7 xor pLimbs[7]
        var t2 = y7 xor order8Y1[7]
        var t3 = y7 xor order8Y2[7]

        for (i in COORD_INTS - 2 downTo 1) {
            val yi = Codec.decode32(p, i * 4)

            t0 = t0 or yi
            t1 = t1 or (yi xor pLimbs[i])
            t2 = t2 or (yi xor order8Y1[i])
            t3 = t3 or (yi xor order8Y2[i])
        }

        val y0 = Codec.decode32(p, 0)

        if (t0 == 0 && Integers.compareUnsigned(y0, 1) <= 0) {
            return false
        }

        if (t1 == 0 && Integers.compareUnsigned(y0, pLimbs[0] - 1) >= 0) {
            return false
        }

        t2 = t2 or (y0 xor order8Y1[0])
        t3 = t3 or (y0 xor order8Y2[0])

        return (t2 != 0) and (t3 != 0)
    }

    private fun checkPointVar(p: ByteArray): Boolean {
        if ((Codec.decode32(p, 28) and 0x7FFFFFFF) < pLimbs[7]) {
            return true
        }

        val t = IntArray(COORD_INTS)
        Codec.decode32(p, 0, t, 0, COORD_INTS)
        t[COORD_INTS - 1] = t[COORD_INTS - 1] and 0x7FFFFFFF
        return !Nat256.gte(t, pLimbs)
    }

    private fun copy(
        buf: ByteArray,
        off: Int,
        len: Int
    ): ByteArray {
        val result = ByteArray(len)
        System.arraycopy(buf, off, result, 0, len)
        return result
    }

    private fun createDigest(): MessageDigest =
        MessageDigest.getInstance("SHA-512")

    private fun decodePointVar(
        p: ByteArray,
        r: PointAffine
    ): Boolean {
        val x0 = (p[POINT_BYTES - 1].toInt() and 0x80) ushr 7

        Field.decode255(p, r.y)

        val u = Field.create()
        val v = Field.create()

        Field.sqr(r.y, u)
        Field.mul(cD, u, v)
        Field.subOne(u)
        Field.addOne(v)

        if (!Field.sqrtRatioVar(u, v, r.x)) {
            return false
        }

        Field.normalize(r.x)
        if (x0 == 1 && Field.isZeroVar(r.x)) {
            return false
        }

        if (x0 == (r.x[0] and 1)) {
            Field.negate(r.x, r.x)
            Field.normalize(r.x)
        }

        return true
    }

    private fun encodePoint(
        p: PointAffine,
        r: ByteArray,
        rOff: Int
    ) {
        Field.encode(p.y, r, rOff)
        val i = rOff + POINT_BYTES - 1
        r[i] = (r[i].toInt() or ((p.x[0] and 1) shl 7)).toByte()
    }

    private fun encodeResult(
        p: PointAccum,
        r: ByteArray,
        rOff: Int
    ): Int {
        val q = PointAffine()
        normalizeToAffine(p, q)

        val result = checkPoint(q)

        encodePoint(q, r, rOff)

        return result
    }

    private fun expandPrivateKey(
        d: MessageDigest,
        sk: ByteArray,
        skOff: Int,
        h: ByteArray
    ) {
        d.update(sk, skOff, SECRET_KEY_SIZE)
        d.digest(h, 0, DIGEST_SIZE)
    }
    private fun groupCombBits(n: IntArray) {
        for (i in n.indices) {
            n[i] = Interleave.shuffle2(n[i])
        }
    }

    private fun implSign(
        d: MessageDigest,
        h: ByteArray,
        s: ByteArray,
        pk: ByteArray,
        m: ByteArray,
        mOff: Int,
        mLen: Int,
        sig: ByteArray,
        sigOff: Int
    ) {
        d.update(h, SCALAR_BYTES, SCALAR_BYTES)
        d.update(m, mOff, mLen)
        d.digest(h, 0, DIGEST_SIZE)

        val r = Scalar25519.reduce512(h)
        val rBytes = ByteArray(POINT_BYTES)
        scalarMultBaseEncoded(r, rBytes, 0)

        d.update(rBytes, 0, POINT_BYTES)
        d.update(pk, 0, POINT_BYTES)
        d.update(m, mOff, mLen)
        d.digest(h, 0, DIGEST_SIZE)

        val k = Scalar25519.reduce512(h)
        val sBytes = calculateS(r, k, s)

        System.arraycopy(rBytes, 0, sig, sigOff, POINT_BYTES)
        System.arraycopy(sBytes, 0, sig, sigOff + POINT_BYTES, SCALAR_BYTES)
    }

    private fun implSign(
        sk: ByteArray,
        skOff: Int,
        m: ByteArray,
        mOff: Int,
        mLen: Int,
        sig: ByteArray,
        sigOff: Int
    ) {
        val d = createDigest()

        val h = ByteArray(DIGEST_SIZE)
        expandPrivateKey(d, sk, skOff, h)

        val s = ByteArray(SCALAR_BYTES)
        pruneScalar(h, s)

        val pk = ByteArray(POINT_BYTES)
        scalarMultBaseEncoded(s, pk, 0)

        implSign(d, h, s, pk, m, mOff, mLen, sig, sigOff)
    }

    private fun implVerify(
        sig: ByteArray,
        sigOff: Int,
        pk: ByteArray,
        pkOff: Int,
        m: ByteArray,
        mOff: Int,
        mLen: Int
    ): Boolean {
        val r = copy(sig, sigOff, POINT_BYTES)
        val s = copy(sig, sigOff + POINT_BYTES, SCALAR_BYTES)
        val a = copy(pk, pkOff, PUBLIC_KEY_SIZE)

        if (!checkPointVar(r)) {
            return false
        }

        val nS = IntArray(SCALAR_INTS)
        if (!Scalar25519.checkVar(s, nS)) {
            return false
        }

        if (!checkPointFullVar(a)) {
            return false
        }

        val pR = PointAffine()
        if (!decodePointVar(r, pR)) {
            return false
        }

        val pA = PointAffine()
        if (!decodePointVar(a, pA)) {
            return false
        }

        val d = createDigest()
        val h = ByteArray(DIGEST_SIZE)

        d.update(r, 0, POINT_BYTES)
        d.update(a, 0, POINT_BYTES)
        d.update(m, mOff, mLen)
        d.digest(h, 0, DIGEST_SIZE)

        val k = Scalar25519.reduce512(h)

        val nA = IntArray(SCALAR_INTS)
        Scalar25519.decode(k, nA)

        val v0 = IntArray(4)
        val v1 = IntArray(4)

        if (!Scalar25519.reduceBasisVar(nA, v0, v1)) {
            throw IllegalStateException()
        }

        Scalar25519.multiply128Var(nS, v1, nS)

        val pZ = PointAccum()
        scalarMultStraus128Var(nS, v0, pA, v1, pR, pZ)
        return normalizeToNeutralElementVar(pZ)
    }

    private fun invertDoubleZs(points: Array<PointExtended>) {
        val count = points.size
        val cs = Field.createTable(count)

        val u = Field.create()
        Field.copy(points[0].z, 0, u, 0)
        Field.copy(u, 0, cs, 0)

        var i = 0
        while (++i < count) {
            Field.mul(u, points[i].z, u)
            Field.copy(u, 0, cs, i * Field.SIZE)
        }

        Field.add(u, u, u)
        Field.invVar(u, u)
        --i

        val t = Field.create()

        while (i > 0) {
            val j = i--
            Field.copy(cs, i * Field.SIZE, t, 0)
            Field.mul(t, u, t)
            Field.mul(u, points[j].z, u)
            Field.copy(t, 0, points[j].z, 0)
        }

        Field.copy(u, 0, points[0].z, 0)
    }

    private fun normalizeToAffine(
        p: PointAccum,
        r: PointAffine
    ) {
        Field.inv(p.z, r.y)
        Field.mul(r.y, p.x, r.x)
        Field.mul(r.y, p.y, r.y)
        Field.normalize(r.x)
        Field.normalize(r.y)
    }

    private fun normalizeToNeutralElementVar(p: PointAccum): Boolean {
        Field.normalize(p.x)
        Field.normalize(p.y)
        Field.normalize(p.z)

        return Field.isZeroVar(p.x) &&
            !Field.isZeroVar(p.y) &&
            Field.areEqualVar(p.y, p.z)
    }

    private fun pointAdd(
        p: PointExtended,
        q: PointExtended,
        r: PointExtended,
        t: PointTemp
    ) {

        val a = r.x
        val b = r.y
        val c = t.r0
        val d = t.r1

        Field.apm(p.y, p.x, b, a)
        Field.apm(q.y, q.x, d, c)
        Field.mul(a, c, a)
        Field.mul(b, d, b)
        Field.mul(p.t, q.t, c)
        Field.mul(c, cD2, c)
        Field.add(p.z, p.z, d)
        Field.mul(d, q.z, d)
        Field.apm(b, a, b, a)
        Field.apm(d, c, d, c)
        Field.mul(a, b, r.t)
        Field.mul(c, d, r.z)
        Field.mul(a, c, r.x)
        Field.mul(b, d, r.y)
    }

    private fun pointAdd(
        p: PointPreComp,
        r: PointAccum,
        t: PointTemp
    ) {
        val a = r.x
        val b = r.y
        val c = t.r0
        val e = r.u
        val h = r.v

        Field.apm(r.y, r.x, b, a)
        Field.mul(a, p.ymxH, a)
        Field.mul(b, p.ypxH, b)
        Field.mul(r.u, r.v, c)
        Field.mul(c, p.xyd, c)
        Field.apm(b, a, h, e)
        Field.apm(r.z, c, b, a)
        Field.mul(a, b, r.z)
        Field.mul(a, e, r.x)
        Field.mul(b, h, r.y)
    }

    private fun pointAddVar(
        negate: Boolean,
        p: PointPreComp,
        r: PointAccum,
        t: PointTemp
    ) {
        val a = r.x
        val b = r.y
        val c = t.r0
        val e = r.u
        val h = r.v

        Field.apm(r.y, r.x, b, a)
        if (negate) {
            Field.mul(b, p.ymxH, b)
            Field.mul(a, p.ypxH, a)
        } else {
            Field.mul(a, p.ymxH, a)
            Field.mul(b, p.ypxH, b)
        }
        Field.mul(r.u, r.v, c)
        Field.mul(c, p.xyd, c)
        Field.apm(b, a, h, e)
        if (negate) {
            Field.apm(r.z, c, a, b)
        } else {
            Field.apm(r.z, c, b, a)
        }
        Field.mul(a, b, r.z)
        Field.mul(a, e, r.x)
        Field.mul(b, h, r.y)
    }

    private fun pointAddVar(
        negate: Boolean,
        p: PointPreCompZ,
        r: PointAccum,
        t: PointTemp
    ) {
        val a = r.x
        val b = r.y
        val c = t.r0
        val d = r.z
        val e = r.u
        val h = r.v

        Field.apm(r.y, r.x, b, a)
        if (negate) {
            Field.mul(b, p.ymxH, b)
            Field.mul(a, p.ypxH, a)
        } else {
            Field.mul(a, p.ymxH, a)
            Field.mul(b, p.ypxH, b)
        }
        Field.mul(r.u, r.v, c)
        Field.mul(c, p.xyd, c)
        Field.mul(r.z, p.z, d)
        Field.apm(b, a, h, e)
        if (negate) {
            Field.apm(d, c, a, b)
        } else {
            Field.apm(d, c, b, a)
        }
        Field.mul(a, b, r.z)
        Field.mul(a, e, r.x)
        Field.mul(b, h, r.y)
    }

    private fun pointCopy(
        p: PointAccum,
        r: PointExtended
    ) {
        Field.copy(p.x, 0, r.x, 0)
        Field.copy(p.y, 0, r.y, 0)
        Field.copy(p.z, 0, r.z, 0)
        Field.mul(p.u, p.v, r.t)
    }

    private fun pointCopy(
        p: PointAffine,
        r: PointExtended
    ) {
        Field.copy(p.x, 0, r.x, 0)
        Field.copy(p.y, 0, r.y, 0)
        Field.one(r.z)
        Field.mul(p.x, p.y, r.t)
    }

    private fun pointCopy(
        p: PointExtended,
        r: PointPreCompZ
    ) {
        Field.apm(p.y, p.x, r.ypxH, r.ymxH)
        Field.mul(p.t, cD2, r.xyd)
        Field.add(p.z, p.z, r.z)
    }

    private fun pointDouble(r: PointAccum) {
        val a = r.x
        val b = r.y
        val c = r.z
        val e = r.u
        val h = r.v

        Field.add(r.x, r.y, e)
        Field.sqr(r.x, a)
        Field.sqr(r.y, b)
        Field.sqr(r.z, c)
        Field.add(c, c, c)
        Field.apm(a, b, h, b)
        Field.sqr(e, e)
        Field.sub(h, e, e)
        Field.add(c, b, a)
        Field.carry(a)
        Field.mul(a, b, r.z)
        Field.mul(a, e, r.x)
        Field.mul(b, h, r.y)
    }

    private fun pointLookup(
        block: Int,
        index: Int,
        p: PointPreComp
    ) {
        var off = block * PRECOMP_POINTS * 3 * Field.SIZE

        for (i in 0 until PRECOMP_POINTS) {
            val cond = ((i xor index) - 1) shr 31
            Field.cmov(cond, precompBaseComb, off, p.ymxH, 0)
            off += Field.SIZE
            Field.cmov(cond, precompBaseComb, off, p.ypxH, 0)
            off += Field.SIZE
            Field.cmov(cond, precompBaseComb, off, p.xyd, 0)
            off += Field.SIZE
        }
    }
    private fun pointPrecompute(
        p: PointAffine,
        points: Array<PointExtended>,
        pointsOff: Int,
        t: PointTemp
    ) {
        val q = PointExtended()
        pointCopy(p, q)
        points[pointsOff] = q

        val d = PointExtended()
        pointAdd(q, q, d, t)

        for (i in 1 until 16) {
            val r = PointExtended()
            pointAdd(points[pointsOff + i - 1], d, r, t)
            points[pointsOff + i] = r
        }
    }
    private fun pointPrecomputeZ(
        p: PointAffine,
        points: Array<PointPreCompZ>,
        t: PointTemp
    ) {
        val q = PointExtended()
        pointCopy(p, q)

        val d = PointExtended()
        pointAdd(q, q, d, t)

        var i = 0
        while (true) {
            val r = points[i]
            pointCopy(q, r)

            if (++i == 4) {
                break
            }

            pointAdd(q, d, q, t)
        }
    }

    private fun pointSetNeutral(p: PointAccum) {
        Field.zero(p.x)
        Field.one(p.y)
        Field.one(p.z)
        Field.zero(p.u)
        Field.one(p.v)
    }

    private fun precompute() {
        if (precomputed) {
            return
        }

        val wnafPoints = 1 shl (WNAF_WIDTH_BASE - 2)
        val combPoints = PRECOMP_BLOCKS * PRECOMP_POINTS
        val totalPoints = wnafPoints * 2 + combPoints

        val points = Array(totalPoints) { PointExtended() }
        val t = PointTemp()

        val b = PointAffine()
        Field.copy(bX, 0, b.x, 0)
        Field.copy(bY, 0, b.y, 0)

        pointPrecompute(b, points, 0, t)

        val b128 = PointAffine()
        Field.copy(b128X, 0, b128.x, 0)
        Field.copy(b128Y, 0, b128.y, 0)

        pointPrecompute(b128, points, wnafPoints, t)

        val p = PointAccum()
        Field.copy(bX, 0, p.x, 0)
        Field.copy(bY, 0, p.y, 0)
        Field.one(p.z)
        Field.copy(p.x, 0, p.u, 0)
        Field.copy(p.y, 0, p.v, 0)

        var pointsIndex = wnafPoints * 2
        val toothPowers = Array(PRECOMP_TEETH) { PointExtended() }

        val u = PointExtended()
        for (block in 0 until PRECOMP_BLOCKS) {
            val sum = PointExtended()

            for (tooth in 0 until PRECOMP_TEETH) {
                if (tooth == 0) {
                    pointCopy(p, sum)
                } else {
                    pointCopy(p, u)
                    pointAdd(sum, u, sum, t)
                }

                pointDouble(p)
                pointCopy(p, toothPowers[tooth])

                if (block + tooth != PRECOMP_BLOCKS + PRECOMP_TEETH - 2) {
                    repeat(PRECOMP_SPACING - 1) {
                        pointDouble(p)
                    }
                }
            }

            Field.negate(sum.x, sum.x)
            Field.negate(sum.t, sum.t)

            points[pointsIndex++] = sum

            for (tooth in 0 until PRECOMP_TEETH - 1) {
                val size = 1 shl tooth
                var j = 0
                while (j < size) {
                    points[pointsIndex] = PointExtended()
                    pointAdd(
                        p = points[pointsIndex - size],
                        q = toothPowers[tooth],
                        r = points[pointsIndex],
                        t = t
                    )
                    ++j
                    ++pointsIndex
                }
            }
        }

        invertDoubleZs(points)

        precompBaseWnaf = Array(wnafPoints) { PointPreComp() }
        for (i in 0 until wnafPoints) {
            val q = points[i]
            val r = precompBaseWnaf[i]

            Field.mul(q.x, q.z, q.x)
            Field.mul(q.y, q.z, q.y)

            Field.apm(q.y, q.x, r.ypxH, r.ymxH)

            Field.mul(q.x, q.y, r.xyd)
            Field.mul(r.xyd, cD4, r.xyd)

            Field.normalize(r.ymxH)
            Field.normalize(r.ypxH)
            Field.normalize(r.xyd)
        }

        precompBase128Wnaf = Array(wnafPoints) { PointPreComp() }
        for (i in 0 until wnafPoints) {
            val q = points[wnafPoints + i]
            val r = precompBase128Wnaf[i]

            Field.mul(q.x, q.z, q.x)
            Field.mul(q.y, q.z, q.y)

            Field.apm(q.y, q.x, r.ypxH, r.ymxH)

            Field.mul(q.x, q.y, r.xyd)
            Field.mul(r.xyd, cD4, r.xyd)

            Field.normalize(r.ymxH)
            Field.normalize(r.ypxH)
            Field.normalize(r.xyd)
        }

        precompBaseComb = Field.createTable(combPoints * 3)
        val s = PointPreComp()
        var off = 0
        for (i in wnafPoints * 2 until totalPoints) {
            val q = points[i]

            Field.mul(q.x, q.z, q.x)
            Field.mul(q.y, q.z, q.y)

            Field.apm(q.y, q.x, s.ypxH, s.ymxH)

            Field.mul(q.x, q.y, s.xyd)
            Field.mul(s.xyd, cD4, s.xyd)

            Field.normalize(s.ymxH)
            Field.normalize(s.ypxH)
            Field.normalize(s.xyd)

            Field.copy(s.ymxH, 0, precompBaseComb, off)
            off += Field.SIZE
            Field.copy(s.ypxH, 0, precompBaseComb, off)
            off += Field.SIZE
            Field.copy(s.xyd, 0, precompBaseComb, off)
            off += Field.SIZE
        }

        precomputed = true
    }

    private fun pruneScalar(
        s: ByteArray
    ) {
        s[0] = (s[0].toInt() and 0xF8).toByte()
        s[SCALAR_BYTES - 1] =
            (s[SCALAR_BYTES - 1].toInt() and 0x7F).toByte()
        s[SCALAR_BYTES - 1] =
            (s[SCALAR_BYTES - 1].toInt() or 0x40).toByte()
    }

    private fun pruneScalar(
        n: ByteArray,
        s: ByteArray
    ) {
        System.arraycopy(n, 0, s, 0, SCALAR_BYTES)
        pruneScalar(s)
    }
    private fun scalarMultBase(
        k: ByteArray,
        r: PointAccum
    ) {
        precompute()

        val n = IntArray(SCALAR_INTS)
        Scalar25519.decode(k, n)
        Scalar25519.toSignedDigits(n)
        groupCombBits(n)

        val p = PointPreComp()
        val t = PointTemp()

        pointSetNeutral(r)
        var resultSign = 0

        var cOff = (PRECOMP_SPACING - 1) * PRECOMP_TEETH
        while (true) {
            for (block in 0 until PRECOMP_BLOCKS) {
                val w = n[block] ushr cOff
                val sign = (w ushr (PRECOMP_TEETH - 1)) and 1
                val abs = (w xor -sign) and PRECOMP_MASK

                pointLookup(block, abs, p)

                Field.cNegate(resultSign xor sign, r.x)
                Field.cNegate(resultSign xor sign, r.u)
                resultSign = sign

                pointAdd(p, r, t)
            }

            cOff -= PRECOMP_TEETH
            if (cOff < 0) {
                break
            }

            pointDouble(r)
        }

        Field.cNegate(resultSign, r.x)
        Field.cNegate(resultSign, r.u)
    }

    private fun scalarMultBaseEncoded(
        k: ByteArray,
        r: ByteArray,
        rOff: Int
    ) {
        val p = PointAccum()
        scalarMultBase(k, p)
        if (0 == encodeResult(p, r, rOff)) {
            throw IllegalStateException()
        }
    }

    private fun scalarMultStraus128Var(
        nb: IntArray,
        np: IntArray,
        p: PointAffine,
        nq: IntArray,
        q: PointAffine,
        r: PointAccum
    ) {
        precompute()

        val wsB = ByteArray(256)
        val wsP = ByteArray(128)
        val wsQ = ByteArray(128)

        Wnaf.getSignedVar(nb, WNAF_WIDTH_BASE, wsB)
        Wnaf.getSignedVar(np, WNAF_WIDTH_128, wsP)
        Wnaf.getSignedVar(nq, WNAF_WIDTH_128, wsQ)

        val count = 1 shl (WNAF_WIDTH_128 - 2)
        val tp = Array(count) { PointPreCompZ() }
        val tq = Array(count) { PointPreCompZ() }
        val t = PointTemp()
        pointPrecomputeZ(p, tp, t)
        pointPrecomputeZ(q, tq, t)

        pointSetNeutral(r)

        var bit = 128
        while (--bit >= 0) {
            if ((wsB[bit].toInt() or wsB[128 + bit].toInt()
                or wsP[bit].toInt() or wsQ[bit].toInt()) != 0) {
                break
            }
        }

        while (bit >= 0) {
            val wb = wsB[bit].toInt()
            if (wb != 0) {
                val index = (wb shr 1) xor (wb shr 31)
                pointAddVar(wb < 0, precompBaseWnaf[index], r, t)
            }

            val wb128 = wsB[128 + bit].toInt()
            if (wb128 != 0) {
                val index = (wb128 shr 1) xor (wb128 shr 31)
                pointAddVar(wb128 < 0, precompBase128Wnaf[index], r, t)
            }

            val wp = wsP[bit].toInt()
            if (wp != 0) {
                val index = (wp shr 1) xor (wp shr 31)
                pointAddVar(wp < 0, tp[index], r, t)
            }

            val wq = wsQ[bit].toInt()
            if (wq != 0) {
                val index = (wq shr 1) xor (wq shr 31)
                pointAddVar(wq < 0, tq[index], r, t)
            }

            pointDouble(r)
            --bit
        }

        pointDouble(r)
        pointDouble(r)
    }

    init {
        precompute()
    }

    private object Field {
        const val SIZE = 10

        private const val M24 = 0x00FFFFFF
        private const val M25 = 0x01FFFFFF
        private const val M26 = 0x03FFFFFF

        private val p32 = intArrayOf(
            0xFFFFFFED.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(),
            0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(),
            0xFFFFFFFF.toInt(), 0x7FFFFFFF
        )

        private val rootNegOne = intArrayOf(
            -0x01F15F50, -0x0079362D, 0x00478C4F, 0x0035697F, 0x005E8630,
            0x01FBD7A7, -0x00BFD9B1, -0x000F4D4B, 0x00027E0F, 0x00570649
        )

        fun add(
            x: IntArray,
            y: IntArray,
            z: IntArray
        ) {
            for (i in 0 until SIZE) {
                z[i] = x[i] + y[i]
            }
        }

        fun addOne(z: IntArray) {
            z[0] += 1
        }

        fun apm(
            x: IntArray,
            y: IntArray,
            zp: IntArray,
            zm: IntArray
        ) {
            for (i in 0 until SIZE) {
                val xi = x[i]
                val yi = y[i]
                zp[i] = xi + yi
                zm[i] = xi - yi
            }
        }

        fun areEqual(
            x: IntArray,
            y: IntArray
        ): Int {
            var d = 0
            for (i in 0 until SIZE) {
                d = d or (x[i] xor y[i])
            }
            return ((d - 1) and d.inv()) shr 31
        }

        fun areEqualVar(
            x: IntArray,
            y: IntArray
        ): Boolean = 0 != areEqual(x, y)

        fun carry(z: IntArray) {
            var z0 = z[0]
            var z1 = z[1]
            var z2 = z[2]
            var z3 = z[3]
            var z4 = z[4]
            var z5 = z[5]
            var z6 = z[6]
            var z7 = z[7]
            var z8 = z[8]
            var z9 = z[9]

            z2 += z1 shr 26
            z1 = z1 and M26
            z4 += z3 shr 26
            z3 = z3 and M26
            z7 += z6 shr 26
            z6 = z6 and M26
            z9 += z8 shr 26
            z8 = z8 and M26

            z3 += z2 shr 25
            z2 = z2 and M25
            z5 += z4 shr 25
            z4 = z4 and M25
            z8 += z7 shr 25
            z7 = z7 and M25
            z0 += (z9 shr 25) * 38
            z9 = z9 and M25

            z1 += z0 shr 26
            z0 = z0 and M26
            z6 += z5 shr 26
            z5 = z5 and M26

            z2 += z1 shr 26
            z1 = z1 and M26
            z4 += z3 shr 26
            z3 = z3 and M26
            z7 += z6 shr 26
            z6 = z6 and M26
            z9 += z8 shr 26
            z8 = z8 and M26

            z[0] = z0
            z[1] = z1
            z[2] = z2
            z[3] = z3
            z[4] = z4
            z[5] = z5
            z[6] = z6
            z[7] = z7
            z[8] = z8
            z[9] = z9
        }

        fun cmov(
            cond: Int,
            x: IntArray,
            xOff: Int,
            z: IntArray,
            zOff: Int
        ) {
            for (i in 0 until SIZE) {
                val zi = z[zOff + i]
                val diff = zi xor x[xOff + i]
                z[zOff + i] = zi xor (diff and cond)
            }
        }

        fun cNegate(
            negate: Int,
            z: IntArray
        ) {
            val mask = -negate
            for (i in 0 until SIZE) {
                z[i] = (z[i] xor mask) - mask
            }
        }

        fun copy(
            x: IntArray,
            xOff: Int,
            z: IntArray,
            zOff: Int
        ) {
            for (i in 0 until SIZE) {
                z[zOff + i] = x[xOff + i]
            }
        }

        fun create(): IntArray = IntArray(SIZE)

        fun createTable(n: Int): IntArray = IntArray(SIZE * n)

        private fun decode128(
            bs: ByteArray,
            off: Int,
            z: IntArray,
            zOff: Int
        ) {
            val t0 = decode32(bs, off + 0)
            val t1 = decode32(bs, off + 4)
            val t2 = decode32(bs, off + 8)
            val t3 = decode32(bs, off + 12)

            z[zOff + 0] = t0 and M26
            z[zOff + 1] = ((t1 shl 6) or (t0 ushr 26)) and M26
            z[zOff + 2] = ((t2 shl 12) or (t1 ushr 20)) and M25
            z[zOff + 3] = ((t3 shl 19) or (t2 ushr 13)) and M26
            z[zOff + 4] = t3 ushr 7
        }

        fun decode255(
            x: ByteArray,
            z: IntArray
        ) {
            decode255(x, 0, z, 0)
        }

        fun decode255(
            x: ByteArray,
            xOff: Int,
            z: IntArray,
            zOff: Int
        ) {
            decode128(x, xOff, z, zOff)
            decode128(x, xOff + 16, z, zOff + 5)
            z[zOff + 9] = z[zOff + 9] and M24
        }

        private fun decode128(
            packed: IntArray,
            off: Int,
            z: IntArray,
            zOff: Int
        ) {
            val t0 = packed[off + 0]
            val t1 = packed[off + 1]
            val t2 = packed[off + 2]
            val t3 = packed[off + 3]

            z[zOff + 0] = t0 and M26
            z[zOff + 1] = ((t1 shl 6) or (t0 ushr 26)) and M26
            z[zOff + 2] = ((t2 shl 12) or (t1 ushr 20)) and M25
            z[zOff + 3] = ((t3 shl 19) or (t2 ushr 13)) and M26
            z[zOff + 4] = t3 ushr 7
        }

        fun decode255(
            x: IntArray,
            z: IntArray
        ) {
            decode255(x, 0, z, 0)
        }

        fun decode255(
            x: IntArray,
            xOff: Int,
            z: IntArray,
            zOff: Int
        ) {
            decode128(x, xOff, z, zOff)
            decode128(x, xOff + 4, z, zOff + 5)
            z[zOff + 9] = z[zOff + 9] and M24
        }

        private fun decode32(
            bs: ByteArray,
            off: Int
        ): Int {
            var n = bs[off].toInt() and 0xFF
            n = n or ((bs[off + 1].toInt() and 0xFF) shl 8)
            n = n or ((bs[off + 2].toInt() and 0xFF) shl 16)
            n = n or (bs[off + 3].toInt() shl 24)
            return n
        }

        fun encode(
            x: IntArray,
            z: IntArray,
            zOff: Int
        ) {
            encode128(x, 0, z, zOff)
            encode128(x, 5, z, zOff + 4)
        }

        fun encode(
            x: IntArray,
            z: ByteArray,
            zOff: Int
        ) {
            encode128(x, 0, z, zOff)
            encode128(x, 5, z, zOff + 16)
        }

        private fun encode128(
            x: IntArray,
            xOff: Int,
            packed: IntArray,
            off: Int
        ) {
            val x0 = x[xOff + 0]
            val x1 = x[xOff + 1]
            val x2 = x[xOff + 2]
            val x3 = x[xOff + 3]
            val x4 = x[xOff + 4]

            packed[off + 0] = x0 or (x1 shl 26)
            packed[off + 1] = (x1 ushr 6) or (x2 shl 20)
            packed[off + 2] = (x2 ushr 12) or (x3 shl 13)
            packed[off + 3] = (x3 ushr 19) or (x4 shl 7)
        }

        private fun encode128(
            x: IntArray,
            xOff: Int,
            bs: ByteArray,
            off: Int
        ) {
            val x0 = x[xOff + 0]
            val x1 = x[xOff + 1]
            val x2 = x[xOff + 2]
            val x3 = x[xOff + 3]
            val x4 = x[xOff + 4]

            val t0 = x0 or (x1 shl 26)
            encode32(t0, bs, off + 0)
            val t1 = (x1 ushr 6) or (x2 shl 20)
            encode32(t1, bs, off + 4)
            val t2 = (x2 ushr 12) or (x3 shl 13)
            encode32(t2, bs, off + 8)
            val t3 = (x3 ushr 19) or (x4 shl 7)
            encode32(t3, bs, off + 12)
        }

        private fun encode32(
            n: Int,
            bs: ByteArray,
            off: Int
        ) {
            bs[off] = n.toByte()
            bs[off + 1] = (n ushr 8).toByte()
            bs[off + 2] = (n ushr 16).toByte()
            bs[off + 3] = (n ushr 24).toByte()
        }

        /**
         * 求逆
         */
        fun inv(
            x: IntArray,
            z: IntArray
        ) {
            val t = create()
            val u = IntArray(8)

            copy(x, 0, t, 0)
            normalize(t)
            encode(t, u, 0)

            Mod.modOddInverse(p32, u, u)

            decode255(u, z)
        }

        fun invVar(
            x: IntArray,
            z: IntArray
        ) {
            val t = create()
            val u = IntArray(8)

            copy(x, 0, t, 0)
            normalize(t)
            encode(t, u, 0)

            Mod.modOddInverseVar(p32, u, u)

            decode255(u, z)
        }

        fun isZero(x: IntArray): Int {
            var d = 0
            for (i in 0 until SIZE) {
                d = d or x[i]
            }
            return ((d - 1) and d.inv()) shr 31
        }

        fun isZeroVar(x: IntArray): Boolean = 0 != isZero(x)

        fun mul(
            x: IntArray,
            y: IntArray,
            z: IntArray
        ) {
            var x0 = x[0]
            var y0 = y[0]
            var x1 = x[1]
            var y1 = y[1]
            var x2 = x[2]
            var y2 = y[2]
            var x3 = x[3]
            var y3 = y[3]
            var x4 = x[4]
            var y4 = y[4]

            val u0 = x[5]
            val v0 = y[5]
            val u1 = x[6]
            val v1 = y[6]
            val u2 = x[7]
            val v2 = y[7]
            val u3 = x[8]
            val v3 = y[8]
            val u4 = x[9]
            val v4 = y[9]

            var a0 = x0.toLong() * y0
            var a1 = x0.toLong() * y1 +
                x1.toLong() * y0
            var a2 = x0.toLong() * y2 +
                x1.toLong() * y1 +
                x2.toLong() * y0
            var a3 = x1.toLong() * y2 +
                x2.toLong() * y1
            a3 = a3 shl 1
            a3 += x0.toLong() * y3 +
                x3.toLong() * y0
            var a4 = x2.toLong() * y2
            a4 = a4 shl 1
            a4 += x0.toLong() * y4 +
                x1.toLong() * y3 +
                x3.toLong() * y1 +
                x4.toLong() * y0
            var a5 = x1.toLong() * y4 +
                x2.toLong() * y3 +
                x3.toLong() * y2 +
                x4.toLong() * y1
            a5 = a5 shl 1
            var a6 = x2.toLong() * y4 +
                x4.toLong() * y2
            a6 = a6 shl 1
            a6 += x3.toLong() * y3
            var a7 = x3.toLong() * y4 +
                x4.toLong() * y3
            var a8 = x4.toLong() * y4
            a8 = a8 shl 1

            val b0 = u0.toLong() * v0
            val b1 = u0.toLong() * v1 +
                u1.toLong() * v0
            val b2 = u0.toLong() * v2 +
                u1.toLong() * v1 +
                u2.toLong() * v0
            var b3 = u1.toLong() * v2 +
                u2.toLong() * v1
            b3 = b3 shl 1
            b3 += u0.toLong() * v3 +
                u3.toLong() * v0
            var b4 = u2.toLong() * v2
            b4 = b4 shl 1
            b4 += u0.toLong() * v4 +
                u1.toLong() * v3 +
                u3.toLong() * v1 +
                u4.toLong() * v0
            val b5 = u1.toLong() * v4 +
                u2.toLong() * v3 +
                u3.toLong() * v2 +
                u4.toLong() * v1
            var b6 = u2.toLong() * v4 +
                u4.toLong() * v2
            b6 = b6 shl 1
            b6 += u3.toLong() * v3
            val b7 = u3.toLong() * v4 +
                u4.toLong() * v3
            val b8 = u4.toLong() * v4

            a0 -= b5 * 76
            a1 -= b6 * 38
            a2 -= b7 * 38
            a3 -= b8 * 76

            a5 -= b0
            a6 -= b1
            a7 -= b2
            a8 -= b3

            x0 += u0
            y0 += v0
            x1 += u1
            y1 += v1
            x2 += u2
            y2 += v2
            x3 += u3
            y3 += v3
            x4 += u4
            y4 += v4

            val c0 = x0.toLong() * y0
            val c1 = x0.toLong() * y1 +
                x1.toLong() * y0
            val c2 = x0.toLong() * y2 +
                x1.toLong() * y1 +
                x2.toLong() * y0
            var c3 = x1.toLong() * y2 +
                x2.toLong() * y1
            c3 = c3 shl 1
            c3 += x0.toLong() * y3 +
                x3.toLong() * y0
            var c4 = x2.toLong() * y2
            c4 = c4 shl 1
            c4 += x0.toLong() * y4 +
                x1.toLong() * y3 +
                x3.toLong() * y1 +
                x4.toLong() * y0
            var c5 = x1.toLong() * y4 +
                x2.toLong() * y3 +
                x3.toLong() * y2 +
                x4.toLong() * y1
            c5 = c5 shl 1
            var c6 = x2.toLong() * y4 +
                x4.toLong() * y2
            c6 = c6 shl 1
            c6 += x3.toLong() * y3
            val c7 = x3.toLong() * y4 +
                x4.toLong() * y3
            var c8 = x4.toLong() * y4
            c8 = c8 shl 1

            var z8: Int
            var z9: Int

            var t = a8 + (c3 - a3)
            z8 = t.toInt() and M26
            t = t shr 26
            t += (c4 - a4) - b4
            z9 = t.toInt() and M25
            t = t shr 25
            t = a0 + (t + c5 - a5) * 38
            z[0] = t.toInt() and M26
            t = t shr 26
            t += a1 + (c6 - a6) * 38
            z[1] = t.toInt() and M26
            t = t shr 26
            t += a2 + (c7 - a7) * 38
            z[2] = t.toInt() and M25
            t = t shr 25
            t += a3 + (c8 - a8) * 38
            z[3] = t.toInt() and M26
            t = t shr 26
            t += a4 + b4 * 38
            z[4] = t.toInt() and M25
            t = t shr 25
            t += a5 + (c0 - a0)
            z[5] = t.toInt() and M26
            t = t shr 26
            t += a6 + (c1 - a1)
            z[6] = t.toInt() and M26
            t = t shr 26
            t += a7 + (c2 - a2)
            z[7] = t.toInt() and M25
            t = t shr 25
            t += z8
            z[8] = t.toInt() and M26
            t = t shr 26
            z[9] = z9 + t.toInt()
        }

        fun negate(
            x: IntArray,
            z: IntArray
        ) {
            for (i in 0 until SIZE) {
                z[i] = -x[i]
            }
        }

        fun normalize(z: IntArray) {
            val x = (z[9] ushr (24 - 1)) and 1
            reduce(z, x)
            reduce(z, -x)
        }

        fun one(z: IntArray) {
            z[0] = 1
            for (i in 1 until SIZE) {
                z[i] = 0
            }
        }

        private fun powPm5d8(
            x: IntArray,
            rx2: IntArray,
            rz: IntArray
        ) {
            sqr(x, rx2)
            mul(x, rx2, rx2)
            val x3 = create()
            sqr(rx2, x3)
            mul(x, x3, x3)
            sqr(x3, 2, x3)
            mul(rx2, x3, x3)
            val x10 = create()
            sqr(x3, 5, x10)
            mul(x3, x10, x10)
            val x15 = create()
            sqr(x10, 5, x15)
            mul(x3, x15, x15)
            sqr(x15, 10, x3)
            mul(x10, x3, x3)
            sqr(x3, 25, x10)
            mul(x3, x10, x10)
            sqr(x10, 25, x15)
            mul(x3, x15, x15)
            sqr(x15, 50, x3)
            mul(x10, x3, x3)
            sqr(x3, 125, x10)
            mul(x3, x10, x10)
            sqr(x10, 2, x3)
            mul(x3, x, rz)
        }

        private fun reduce(
            z: IntArray,
            x: Int
        ) {
            val t = z[9]
            val z9 = t and M24
            val tt = (t shr 24) + x

            var cc = (tt * 19).toLong()
            cc += z[0]
            z[0] = cc.toInt() and M26
            cc = cc shr 26
            cc += z[1]
            z[1] = cc.toInt() and M26
            cc = cc shr 26
            cc += z[2]
            z[2] = cc.toInt() and M25
            cc = cc shr 25
            cc += z[3]
            z[3] = cc.toInt() and M26
            cc = cc shr 26
            cc += z[4]
            z[4] = cc.toInt() and M25
            cc = cc shr 25
            cc += z[5]
            z[5] = cc.toInt() and M26
            cc = cc shr 26
            cc += z[6]
            z[6] = cc.toInt() and M26
            cc = cc shr 26
            cc += z[7]
            z[7] = cc.toInt() and M25
            cc = cc shr 25
            cc += z[8]
            z[8] = cc.toInt() and M26
            cc = cc shr 26
            z[9] = z9 + cc.toInt()
        }

        fun sqr(
            x: IntArray,
            z: IntArray
        ) {
            var x0 = x[0]
            var x1 = x[1]
            var x2 = x[2]
            var x3 = x[3]
            var x4 = x[4]

            val u0 = x[5]
            val u1 = x[6]
            val u2 = x[7]
            val u3 = x[8]
            val u4 = x[9]

            var x1Doubled = x1 * 2
            var x2Doubled = x2 * 2
            var x3Doubled = x3 * 2
            var x4Doubled = x4 * 2

            var a0 = x0.toLong() * x0
            var a1 = x0.toLong() * x1Doubled
            var a2 = x0.toLong() * x2Doubled +
                x1.toLong() * x1
            var a3 = x1Doubled.toLong() * x2Doubled +
                x0.toLong() * x3Doubled
            val a4 = x2.toLong() * x2Doubled +
                x0.toLong() * x4Doubled +
                x1.toLong() * x3Doubled
            var a5 = x1Doubled.toLong() * x4Doubled +
                x2Doubled.toLong() * x3Doubled
            var a6 = x2Doubled.toLong() * x4Doubled +
                x3.toLong() * x3
            var a7 = x3.toLong() * x4Doubled
            var a8 = x4.toLong() * x4Doubled

            val u1Doubled = u1 * 2
            val u2Doubled = u2 * 2
            val u3Doubled = u3 * 2
            val u4Doubled = u4 * 2

            val b0 = u0.toLong() * u0
            val b1 = u0.toLong() * u1Doubled
            val b2 = u0.toLong() * u2Doubled +
                u1.toLong() * u1
            val b3 = u1Doubled.toLong() * u2Doubled +
                u0.toLong() * u3Doubled
            val b4 = u2.toLong() * u2Doubled +
                u0.toLong() * u4Doubled +
                u1.toLong() * u3Doubled
            val b5 = u1Doubled.toLong() * u4Doubled +
                u2Doubled.toLong() * u3Doubled
            val b6 = u2Doubled.toLong() * u4Doubled +
                u3.toLong() * u3
            val b7 = u3.toLong() * u4Doubled
            val b8 = u4.toLong() * u4Doubled

            a0 -= b5 * 38
            a1 -= b6 * 38
            a2 -= b7 * 38
            a3 -= b8 * 38

            a5 -= b0
            a6 -= b1
            a7 -= b2
            a8 -= b3

            x0 += u0
            x1 += u1
            x2 += u2
            x3 += u3
            x4 += u4

            x1Doubled = x1 * 2
            x2Doubled = x2 * 2
            x3Doubled = x3 * 2
            x4Doubled = x4 * 2

            val c0 = x0.toLong() * x0
            val c1 = x0.toLong() * x1Doubled
            val c2 = x0.toLong() * x2Doubled +
                x1.toLong() * x1
            val c3 = x1Doubled.toLong() * x2Doubled +
                x0.toLong() * x3Doubled
            val c4 = x2.toLong() * x2Doubled +
                x0.toLong() * x4Doubled +
                x1.toLong() * x3Doubled
            val c5 = x1Doubled.toLong() * x4Doubled +
                x2Doubled.toLong() * x3Doubled
            val c6 = x2Doubled.toLong() * x4Doubled +
                x3.toLong() * x3
            val c7 = x3.toLong() * x4Doubled
            val c8 = x4.toLong() * x4Doubled

            var z8: Int
            var z9: Int

            var t = a8 + (c3 - a3)
            z8 = t.toInt() and M26
            t = t shr 26
            t += (c4 - a4) - b4
            z9 = t.toInt() and M25
            t = t shr 25
            t = a0 + (t + c5 - a5) * 38
            z[0] = t.toInt() and M26
            t = t shr 26
            t += a1 + (c6 - a6) * 38
            z[1] = t.toInt() and M26
            t = t shr 26
            t += a2 + (c7 - a7) * 38
            z[2] = t.toInt() and M25
            t = t shr 25
            t += a3 + (c8 - a8) * 38
            z[3] = t.toInt() and M26
            t = t shr 26
            t += a4 + b4 * 38
            z[4] = t.toInt() and M25
            t = t shr 25
            t += a5 + (c0 - a0)
            z[5] = t.toInt() and M26
            t = t shr 26
            t += a6 + (c1 - a1)
            z[6] = t.toInt() and M26
            t = t shr 26
            t += a7 + (c2 - a2)
            z[7] = t.toInt() and M25
            t = t shr 25
            t += z8
            z[8] = t.toInt() and M26
            t = t shr 26
            z[9] = z9 + t.toInt()
        }

        fun sqr(
            x: IntArray,
            n: Int,
            z: IntArray
        ) {
            sqr(x, z)

            var nn = n
            while (--nn > 0) {
                sqr(z, z)
            }
        }

        fun sqrtRatioVar(
            u: IntArray,
            v: IntArray,
            z: IntArray
        ): Boolean {
            val uv3 = create()
            val uv7 = create()

            mul(u, v, uv3)
            sqr(v, uv7)
            mul(uv3, uv7, uv3)
            sqr(uv7, uv7)
            mul(uv7, uv3, uv7)

            val t = create()
            val x = create()
            powPm5d8(uv7, t, x)
            mul(x, uv3, x)

            val vx2 = create()
            sqr(x, vx2)
            mul(vx2, v, vx2)

            sub(vx2, u, t)
            normalize(t)
            if (isZeroVar(t)) {
                copy(x, 0, z, 0)
                return true
            }

            add(vx2, u, t)
            normalize(t)
            if (isZeroVar(t)) {
                mul(x, rootNegOne, z)
                return true
            }

            return false
        }

        fun sub(
            x: IntArray,
            y: IntArray,
            z: IntArray
        ) {
            for (i in 0 until SIZE) {
                z[i] = x[i] - y[i]
            }
        }

        fun subOne(z: IntArray) {
            z[0] -= 1
        }

        fun zero(z: IntArray) {
            for (i in 0 until SIZE) {
                z[i] = 0
            }
        }
    }

    private object Nat {
        private const val M = 0xFFFFFFFFL

        fun cAddTo(
            len: Int,
            mask: Int,
            x: IntArray,
            z: IntArray
        ): Int {
            val maskL = (-(mask and 1)).toLong() and M
            var c = 0L
            for (i in 0 until len) {
                c += (z[i].toLong() and M) + (x[i].toLong() and maskL)
                z[i] = c.toInt()
                c = c ushr 32
            }
            return c.toInt()
        }

        fun cZero(x: Int): Int = ((x - 1) and x.inv()) shr 31

        fun getBitLength(
            len: Int,
            x: IntArray
        ): Int {
            for (i in len - 1 downTo 0) {
                val xi = x[i]
                if (xi != 0) {
                    return i * Integers.SIZE + Integers.bitLength(xi)
                }
            }
            return 0
        }

        fun shiftDownBit(
            len: Int,
            z: IntArray,
            c: Int
        ): Int {
            var cc = c
            var i = len
            while (--i >= 0) {
                val next = z[i]
                z[i] = (next ushr 1) or (cc shl 31)
                cc = next
            }
            return cc shl 31
        }
    }

    private object Nat256 {
        private const val M = 0xFFFFFFFFL

        fun addTo(
            x: IntArray,
            xOff: Int,
            z: IntArray,
            zOff: Int,
            cIn: Int
        ): Int {
            var c = cIn.toLong() and M
            c += (x[xOff + 0].toLong() and M) + (z[zOff + 0].toLong() and M)
            z[zOff + 0] = c.toInt()
            c = c ushr 32
            c += (x[xOff + 1].toLong() and M) + (z[zOff + 1].toLong() and M)
            z[zOff + 1] = c.toInt()
            c = c ushr 32
            c += (x[xOff + 2].toLong() and M) + (z[zOff + 2].toLong() and M)
            z[zOff + 2] = c.toInt()
            c = c ushr 32
            c += (x[xOff + 3].toLong() and M) + (z[zOff + 3].toLong() and M)
            z[zOff + 3] = c.toInt()
            c = c ushr 32
            c += (x[xOff + 4].toLong() and M) + (z[zOff + 4].toLong() and M)
            z[zOff + 4] = c.toInt()
            c = c ushr 32
            c += (x[xOff + 5].toLong() and M) + (z[zOff + 5].toLong() and M)
            z[zOff + 5] = c.toInt()
            c = c ushr 32
            c += (x[xOff + 6].toLong() and M) + (z[zOff + 6].toLong() and M)
            z[zOff + 6] = c.toInt()
            c = c ushr 32
            c += (x[xOff + 7].toLong() and M) + (z[zOff + 7].toLong() and M)
            z[zOff + 7] = c.toInt()
            c = c ushr 32
            return c.toInt()
        }

        fun subFrom(
            x: IntArray,
            xOff: Int,
            z: IntArray,
            zOff: Int,
            cIn: Int
        ): Int {
            var c = cIn.toLong() and M
            c += (z[zOff + 0].toLong() and M) - (x[xOff + 0].toLong() and M)
            z[zOff + 0] = c.toInt()
            c = c shr 32
            c += (z[zOff + 1].toLong() and M) - (x[xOff + 1].toLong() and M)
            z[zOff + 1] = c.toInt()
            c = c shr 32
            c += (z[zOff + 2].toLong() and M) - (x[xOff + 2].toLong() and M)
            z[zOff + 2] = c.toInt()
            c = c shr 32
            c += (z[zOff + 3].toLong() and M) - (x[xOff + 3].toLong() and M)
            z[zOff + 3] = c.toInt()
            c = c shr 32
            c += (z[zOff + 4].toLong() and M) - (x[xOff + 4].toLong() and M)
            z[zOff + 4] = c.toInt()
            c = c shr 32
            c += (z[zOff + 5].toLong() and M) - (x[xOff + 5].toLong() and M)
            z[zOff + 5] = c.toInt()
            c = c shr 32
            c += (z[zOff + 6].toLong() and M) - (x[xOff + 6].toLong() and M)
            z[zOff + 6] = c.toInt()
            c = c shr 32
            c += (z[zOff + 7].toLong() and M) - (x[xOff + 7].toLong() and M)
            z[zOff + 7] = c.toInt()
            c = c shr 32
            return c.toInt()
        }

        fun gte(
            x: IntArray,
            y: IntArray
        ): Boolean {
            for (i in 7 downTo 0) {
                val xI = x[i] + Integer.MIN_VALUE
                val yI = y[i] + Integer.MIN_VALUE
                if (xI < yI) {
                    return false
                }
                if (xI > yI) {
                    return true
                }
            }
            return true
        }

        fun mul(
            x: IntArray,
            y: IntArray,
            zz: IntArray
        ) {
            val y0 = y[0].toLong() and M
            val y1 = y[1].toLong() and M
            val y2 = y[2].toLong() and M
            val y3 = y[3].toLong() and M
            val y4 = y[4].toLong() and M
            val y5 = y[5].toLong() and M
            val y6 = y[6].toLong() and M
            val y7 = y[7].toLong() and M

            run {
                var c = 0L
                val x0 = x[0].toLong() and M
                c += x0 * y0
                zz[0] = c.toInt()
                c = c ushr 32
                c += x0 * y1
                zz[1] = c.toInt()
                c = c ushr 32
                c += x0 * y2
                zz[2] = c.toInt()
                c = c ushr 32
                c += x0 * y3
                zz[3] = c.toInt()
                c = c ushr 32
                c += x0 * y4
                zz[4] = c.toInt()
                c = c ushr 32
                c += x0 * y5
                zz[5] = c.toInt()
                c = c ushr 32
                c += x0 * y6
                zz[6] = c.toInt()
                c = c ushr 32
                c += x0 * y7
                zz[7] = c.toInt()
                c = c ushr 32
                zz[8] = c.toInt()
            }

            for (i in 1 until 8) {
                var c = 0L
                val xi = x[i].toLong() and M
                c += xi * y0 + (zz[i + 0].toLong() and M)
                zz[i + 0] = c.toInt()
                c = c ushr 32
                c += xi * y1 + (zz[i + 1].toLong() and M)
                zz[i + 1] = c.toInt()
                c = c ushr 32
                c += xi * y2 + (zz[i + 2].toLong() and M)
                zz[i + 2] = c.toInt()
                c = c ushr 32
                c += xi * y3 + (zz[i + 3].toLong() and M)
                zz[i + 3] = c.toInt()
                c = c ushr 32
                c += xi * y4 + (zz[i + 4].toLong() and M)
                zz[i + 4] = c.toInt()
                c = c ushr 32
                c += xi * y5 + (zz[i + 5].toLong() and M)
                zz[i + 5] = c.toInt()
                c = c ushr 32
                c += xi * y6 + (zz[i + 6].toLong() and M)
                zz[i + 6] = c.toInt()
                c = c ushr 32
                c += xi * y7 + (zz[i + 7].toLong() and M)
                zz[i + 7] = c.toInt()
                c = c ushr 32
                zz[i + 8] = c.toInt()
            }
        }

        fun mul128(
            x: IntArray,
            y128: IntArray,
            zz: IntArray
        ) {
            val x0 = x[0].toLong() and M
            val x1 = x[1].toLong() and M
            val x2 = x[2].toLong() and M
            val x3 = x[3].toLong() and M
            val x4 = x[4].toLong() and M
            val x5 = x[5].toLong() and M
            val x6 = x[6].toLong() and M
            val x7 = x[7].toLong() and M

            run {
                var c = 0L
                val y0 = y128[0].toLong() and M
                c += y0 * x0
                zz[0] = c.toInt()
                c = c ushr 32
                c += y0 * x1
                zz[1] = c.toInt()
                c = c ushr 32
                c += y0 * x2
                zz[2] = c.toInt()
                c = c ushr 32
                c += y0 * x3
                zz[3] = c.toInt()
                c = c ushr 32
                c += y0 * x4
                zz[4] = c.toInt()
                c = c ushr 32
                c += y0 * x5
                zz[5] = c.toInt()
                c = c ushr 32
                c += y0 * x6
                zz[6] = c.toInt()
                c = c ushr 32
                c += y0 * x7
                zz[7] = c.toInt()
                c = c ushr 32
                zz[8] = c.toInt()
            }

            for (i in 1 until 4) {
                var c = 0L
                val yi = y128[i].toLong() and M
                c += yi * x0 + (zz[i + 0].toLong() and M)
                zz[i + 0] = c.toInt()
                c = c ushr 32
                c += yi * x1 + (zz[i + 1].toLong() and M)
                zz[i + 1] = c.toInt()
                c = c ushr 32
                c += yi * x2 + (zz[i + 2].toLong() and M)
                zz[i + 2] = c.toInt()
                c = c ushr 32
                c += yi * x3 + (zz[i + 3].toLong() and M)
                zz[i + 3] = c.toInt()
                c = c ushr 32
                c += yi * x4 + (zz[i + 4].toLong() and M)
                zz[i + 4] = c.toInt()
                c = c ushr 32
                c += yi * x5 + (zz[i + 5].toLong() and M)
                zz[i + 5] = c.toInt()
                c = c ushr 32
                c += yi * x6 + (zz[i + 6].toLong() and M)
                zz[i + 6] = c.toInt()
                c = c ushr 32
                c += yi * x7 + (zz[i + 7].toLong() and M)
                zz[i + 7] = c.toInt()
                c = c ushr 32
                zz[i + 8] = c.toInt()
            }
        }

        fun mulAddTo(
            x: IntArray,
            y: IntArray,
            zz: IntArray
        ): Int {
            val y0 = y[0].toLong() and M
            val y1 = y[1].toLong() and M
            val y2 = y[2].toLong() and M
            val y3 = y[3].toLong() and M
            val y4 = y[4].toLong() and M
            val y5 = y[5].toLong() and M
            val y6 = y[6].toLong() and M
            val y7 = y[7].toLong() and M

            var zc = 0L
            for (i in 0 until 8) {
                var c = 0L
                val xi = x[i].toLong() and M
                c += xi * y0 + (zz[i + 0].toLong() and M)
                zz[i + 0] = c.toInt()
                c = c ushr 32
                c += xi * y1 + (zz[i + 1].toLong() and M)
                zz[i + 1] = c.toInt()
                c = c ushr 32
                c += xi * y2 + (zz[i + 2].toLong() and M)
                zz[i + 2] = c.toInt()
                c = c ushr 32
                c += xi * y3 + (zz[i + 3].toLong() and M)
                zz[i + 3] = c.toInt()
                c = c ushr 32
                c += xi * y4 + (zz[i + 4].toLong() and M)
                zz[i + 4] = c.toInt()
                c = c ushr 32
                c += xi * y5 + (zz[i + 5].toLong() and M)
                zz[i + 5] = c.toInt()
                c = c ushr 32
                c += xi * y6 + (zz[i + 6].toLong() and M)
                zz[i + 6] = c.toInt()
                c = c ushr 32
                c += xi * y7 + (zz[i + 7].toLong() and M)
                zz[i + 7] = c.toInt()
                c = c ushr 32

                zc += c + (zz[i + 8].toLong() and M)
                zz[i + 8] = zc.toInt()
                zc = zc ushr 32
            }
            return zc.toInt()
        }

        fun square(
            x: IntArray,
            zz: IntArray
        ) {
            val x0 = x[0].toLong() and M
            var zz1 = 0L

            var c = 0
            var w: Int
            run {
                var i = 7
                var j = 16
                do {
                    val xVal = x[i--].toLong() and M
                    val p = xVal * xVal
                    zz[--j] = (c shl 31) or (p ushr 33).toInt()
                    zz[--j] = (p ushr 1).toInt()
                    c = p.toInt()
                } while (i > 0)

                run {
                    val p = x0 * x0
                    zz1 = ((c shl 31).toLong() and M) or (p ushr 33)
                    zz[0] = p.toInt()
                    c = ((p ushr 32).toInt()) and 1
                }
            }

            val x1 = x[1].toLong() and M
            var zz2 = zz[2].toLong() and M

            run {
                zz1 += x1 * x0
                w = zz1.toInt()
                zz[1] = (w shl 1) or c
                c = w ushr 31
                zz2 += zz1 ushr 32
            }

            val x2 = x[2].toLong() and M
            var zz3 = zz[3].toLong() and M
            var zz4 = zz[4].toLong() and M
            run {
                zz2 += x2 * x0
                w = zz2.toInt()
                zz[2] = (w shl 1) or c
                c = w ushr 31
                zz3 += (zz2 ushr 32) + x2 * x1
                zz4 += zz3 ushr 32
                zz3 = zz3 and M
            }

            val x3 = x[3].toLong() and M
            var zz5 = (zz[5].toLong() and M) + (zz4 ushr 32)
            zz4 = zz4 and M
            var zz6 = (zz[6].toLong() and M) + (zz5 ushr 32)
            zz5 = zz5 and M
            run {
                zz3 += x3 * x0
                w = zz3.toInt()
                zz[3] = (w shl 1) or c
                c = w ushr 31
                zz4 += (zz3 ushr 32) + x3 * x1
                zz5 += (zz4 ushr 32) + x3 * x2
                zz4 = zz4 and M
                zz6 += zz5 ushr 32
                zz5 = zz5 and M
            }

            val x4 = x[4].toLong() and M
            var zz7 = (zz[7].toLong() and M) + (zz6 ushr 32)
            zz6 = zz6 and M
            var zz8 = (zz[8].toLong() and M) + (zz7 ushr 32)
            zz7 = zz7 and M
            run {
                zz4 += x4 * x0
                w = zz4.toInt()
                zz[4] = (w shl 1) or c
                c = w ushr 31
                zz5 += (zz4 ushr 32) + x4 * x1
                zz6 += (zz5 ushr 32) + x4 * x2
                zz5 = zz5 and M
                zz7 += (zz6 ushr 32) + x4 * x3
                zz6 = zz6 and M
                zz8 += zz7 ushr 32
                zz7 = zz7 and M
            }

            val x5 = x[5].toLong() and M
            var zz9 = (zz[9].toLong() and M) + (zz8 ushr 32)
            zz8 = zz8 and M
            var zz10 = (zz[10].toLong() and M) + (zz9 ushr 32)
            zz9 = zz9 and M
            run {
                zz5 += x5 * x0
                w = zz5.toInt()
                zz[5] = (w shl 1) or c
                c = w ushr 31
                zz6 += (zz5 ushr 32) + x5 * x1
                zz7 += (zz6 ushr 32) + x5 * x2
                zz6 = zz6 and M
                zz8 += (zz7 ushr 32) + x5 * x3
                zz7 = zz7 and M
                zz9 += (zz8 ushr 32) + x5 * x4
                zz8 = zz8 and M
                zz10 += zz9 ushr 32
                zz9 = zz9 and M
            }

            val x6 = x[6].toLong() and M
            var zz11 = (zz[11].toLong() and M) + (zz10 ushr 32)
            zz10 = zz10 and M
            var zz12 = (zz[12].toLong() and M) + (zz11 ushr 32)
            zz11 = zz11 and M
            run {
                zz6 += x6 * x0
                w = zz6.toInt()
                zz[6] = (w shl 1) or c
                c = w ushr 31
                zz7 += (zz6 ushr 32) + x6 * x1
                zz8 += (zz7 ushr 32) + x6 * x2
                zz7 = zz7 and M
                zz9 += (zz8 ushr 32) + x6 * x3
                zz8 = zz8 and M
                zz10 += (zz9 ushr 32) + x6 * x4
                zz9 = zz9 and M
                zz11 += (zz10 ushr 32) + x6 * x5
                zz10 = zz10 and M
                zz12 += zz11 ushr 32
                zz11 = zz11 and M
            }

            val x7 = x[7].toLong() and M
            var zz13 = (zz[13].toLong() and M) + (zz12 ushr 32)
            zz12 = zz12 and M
            var zz14 = (zz[14].toLong() and M) + (zz13 ushr 32)
            zz13 = zz13 and M
            run {
                zz7 += x7 * x0
                w = zz7.toInt()
                zz[7] = (w shl 1) or c
                c = w ushr 31
                zz8 += (zz7 ushr 32) + x7 * x1
                zz9 += (zz8 ushr 32) + x7 * x2
                zz10 += (zz9 ushr 32) + x7 * x3
                zz11 += (zz10 ushr 32) + x7 * x4
                zz12 += (zz11 ushr 32) + x7 * x5
                zz13 += (zz12 ushr 32) + x7 * x6
                zz14 += zz13 ushr 32
            }

            w = zz8.toInt()
            zz[8] = (w shl 1) or c
            c = w ushr 31
            w = zz9.toInt()
            zz[9] = (w shl 1) or c
            c = w ushr 31
            w = zz10.toInt()
            zz[10] = (w shl 1) or c
            c = w ushr 31
            w = zz11.toInt()
            zz[11] = (w shl 1) or c
            c = w ushr 31
            w = zz12.toInt()
            zz[12] = (w shl 1) or c
            c = w ushr 31
            w = zz13.toInt()
            zz[13] = (w shl 1) or c
            c = w ushr 31
            w = zz14.toInt()
            zz[14] = (w shl 1) or c
            c = w ushr 31
            w = zz[15] + (zz14 ushr 32).toInt()
            zz[15] = (w shl 1) or c
        }
    }

    private object Mod {
        private const val M30 = 1073741823
        private const val M32L = 4294967295L

        /**
         * 定长 divSteps 求逆
         *
         * @return 是否求逆成功
         */
        fun modOddInverse(
            m: IntArray,
            x: IntArray,
            z: IntArray
        ): Int {
            val bits = m.size * 32 -
                Integers.numberOfLeadingZeros(m[m.size - 1])
            val len = (bits + 29) / 30

            val t = IntArray(4)
            val d = IntArray(len)
            val e = IntArray(len)
            val f = IntArray(len)
            val g = IntArray(len)
            val m30 = IntArray(len)

            e[0] = 1
            encode30(bits, x, g)
            encode30(bits, m, m30)
            System.arraycopy(m30, 0, f, 0, len)

            var divSteps = 0
            val m0 = inverse32(m30[0])
            val maxDivSteps = getMaximumHDDivSteps(bits)

            var i = 0
            while (i < maxDivSteps) {
                divSteps = hdDivSteps30(divSteps, f[0], g[0], t)
                updateDE30(len, d, e, t, m0, m30)
                updateFG30(len, f, g, t)
                i += 30
            }

            val cF = f[len - 1] shr 31
            cNegate30(len, cF, f)
            cNormalize30(len, cF, d, m30)
            decode30(bits, d, z)
            return equalTo(len, f, 1) and equalTo(len, g, 0)
        }

        /**
         * 变时 divSteps 求逆
         *
         * @return 是否求逆成功
         */
        fun modOddInverseVar(
            m: IntArray,
            x: IntArray,
            z: IntArray
        ): Boolean {
            val bits = m.size * 32 -
                Integers.numberOfLeadingZeros(m[m.size - 1])
            val len = (bits + 29) / 30

            val limit = bits - Nat.getBitLength(m.size, x)

            val t = IntArray(4)
            val d = IntArray(len)
            val e = IntArray(len)
            val f = IntArray(len)
            val g = IntArray(len)
            val m30 = IntArray(len)

            e[0] = 1
            encode30(bits, x, g)
            encode30(bits, m, m30)
            System.arraycopy(m30, 0, f, 0, len)

            var divSteps = -limit
            var lenF = len
            val m0 = inverse32(m30[0])
            val maxDivSteps = getMaximumDivSteps(bits)

            var iter = limit
            while (!equalToVar(lenF, g, 0)) {
                if (iter >= maxDivSteps) {
                    return false
                }

                iter += 30
                divSteps = divSteps30Var(divSteps, f[0], g[0], t)
                updateDE30(len, d, e, t, m0, m30)
                updateFG30(lenF, f, g, t)
                lenF = trimFG30(lenF, f, g)
            }

            val cF = f[lenF - 1] shr 31
            var cD = d[len - 1] shr 31
            if (cD < 0) {
                cD = add30(len, d, m30)
            }

            if (cF < 0) {
                cD = negate30(len, d)
                negate30(lenF, f)
            }

            if (!equalToVar(lenF, f, 1)) {
                return false
            }

            if (cD < 0) {
                add30(len, d, m30)
            }

            decode30(bits, d, z)
            return true
        }

        private fun inverse32(x: Int): Int {
            var z = x * (2 - x * x)
            z *= 2 - x * z
            z *= 2 - x * z
            z *= 2 - x * z
            return z
        }

        private fun add30(
            len: Int,
            x: IntArray,
            y: IntArray
        ): Int {
            var c = 0
            val last = len - 1

            for (i in 0 until last) {
                c += x[i] + y[i]
                x[i] = c and M30
                c = c shr 30
            }

            c += x[last] + y[last]
            x[last] = c
            c = c shr 30
            return c
        }

        private fun cNegate30(
            len: Int,
            c: Int,
            x: IntArray
        ) {
            var cc = 0
            val last = len - 1

            for (i in 0 until last) {
                cc += (x[i] xor c) - c
                x[i] = cc and M30
                cc = cc shr 30
            }

            cc += (x[last] xor c) - c
            x[last] = cc
        }

        private fun cNormalize30(
            len: Int,
            c: Int,
            d: IntArray,
            m: IntArray
        ) {
            val last = len - 1
            var cc = 0
            var cD = d[last] shr 31

            for (i in 0 until last) {
                var t = d[i] + (m[i] and cD)
                t = (t xor c) - c
                cc += t
                d[i] = cc and M30
                cc = cc shr 30
            }

            var t = d[last] + (m[last] and cD)
            t = (t xor c) - c
            cc += t
            d[last] = cc
            cc = 0
            cD = d[last] shr 31

            for (i in 0 until last) {
                t = d[i] + (m[i] and cD)
                cc += t
                d[i] = cc and M30
                cc = cc shr 30
            }

            t = d[last] + (m[last] and cD)
            cc += t
            d[last] = cc
        }

        private fun decode30(
            bitsIn: Int,
            x: IntArray,
            z: IntArray
        ) {
            var bits = bitsIn
            var xOff = 0
            var zOff = 0
            var acc = 0L
            var i = 0

            while (bits > 0) {
                while (i < minOf(32, bits)) {
                    acc = acc or ((x[xOff++].toLong()) shl i)
                    i += 30
                }

                z[zOff++] = acc.toInt()
                acc = acc ushr 32
                i -= 32
                bits -= 32
            }
        }

        private fun encode30(
            bitsIn: Int,
            x: IntArray,
            z: IntArray
        ) {
            var bits = bitsIn
            var xOff = 0
            var zOff = 0
            var acc = 0L
            var i = 0

            while (bits > 0) {
                if (i < minOf(30, bits)) {
                    acc = acc or ((x[xOff++].toLong() and M32L) shl i)
                    i += 32
                }

                z[zOff++] = acc.toInt() and M30
                acc = acc ushr 30
                i -= 30
                bits -= 30
            }
        }

        private fun divSteps30Var(
            dIn: Int,
            f0: Int,
            g0: Int,
            t: IntArray
        ): Int {
            var d = dIn
            var u = 1
            var v = 0
            var q = 0
            var r = 1
            var f = f0
            var g = g0
            var i = 30

            while (true) {
                val zeros = Integers.numberOfTrailingZeros(g or (-1 shl i))
                g = g shr zeros
                u = u shl zeros
                v = v shl zeros
                d -= zeros
                i -= zeros

                if (i <= 0) {
                    t[0] = u
                    t[1] = v
                    t[2] = q
                    t[3] = r
                    return d
                }

                val m: Int
                if (d <= 0) {
                    d = 2 - d

                    var tmp = f
                    f = g
                    g = -tmp
                    tmp = u
                    u = q
                    q = -tmp
                    tmp = v
                    v = r
                    r = -tmp

                    val limit = if (d > i) i else d
                    val mask = (-1 ushr (32 - limit)) and 63
                    m = f * g * (f * f - 2) and mask
                } else {
                    val limit = if (d > i) i else d
                    val mask = (-1 ushr (32 - limit)) and 15
                    m = (f + (((f + 1) and 4) shl 1)) * -g and mask
                }

                g += f * m
                q += u * m
                r += v * m
            }
        }

        private fun equalTo(
            len: Int,
            x: IntArray,
            y: Int
        ): Int {
            var d = x[0] xor y
            for (i in 1 until len) {
                d = d or x[i]
            }
            return Nat.cZero(d)
        }

        private fun equalToVar(
            len: Int,
            x: IntArray,
            y: Int
        ): Boolean {
            var d = x[0] xor y
            if (d != 0) {
                return false
            }

            for (i in 1 until len) {
                d = d or x[i]
            }

            return d == 0
        }

        private fun getMaximumDivSteps(bits: Int): Int {
            val bias = if (bits < 46) 308405L else 181188L
            return ((188898L * bits + bias) ushr 16).toInt()
        }

        private fun getMaximumHDDivSteps(bits: Int): Int =
            ((150964L * bits + 99243L) ushr 16).toInt()

        private fun hdDivSteps30(
            dIn: Int,
            f0: Int,
            g0: Int,
            t: IntArray
        ): Int {
            var d = dIn
            var u = 0x40000000
            var v = 0
            var q = 0
            var r = 0x40000000
            var f = f0
            var g = g0

            repeat(30) {
                val c1 = d shr 31
                val c2 = -(g and 1)

                val x = f xor c1
                val y = u xor c1
                val z = v xor c1

                g -= x and c2
                q -= y and c2
                r -= z and c2

                val c3 = c2 and c1.inv()

                d = (d xor c3) + 1
                f += g and c3
                u += q and c3
                v += r and c3

                g = g shr 1
                q = q shr 1
                r = r shr 1
            }

            t[0] = u
            t[1] = v
            t[2] = q
            t[3] = r
            return d
        }

        private fun negate30(
            len: Int,
            x: IntArray
        ): Int {
            var c = 0
            val last = len - 1

            for (i in 0 until last) {
                c -= x[i]
                x[i] = c and M30
                c = c shr 30
            }

            c -= x[last]
            x[last] = c
            c = c shr 30
            return c
        }

        private fun trimFG30(
            lenIn: Int,
            f: IntArray,
            g: IntArray
        ): Int {
            var len = lenIn
            val fLimb = f[len - 1]
            val gLimb = g[len - 1]
            var cond = (len - 2) shr 31
            cond = cond or (fLimb xor (fLimb shr 31))
            cond = cond or (gLimb xor (gLimb shr 31))

            if (cond == 0) {
                f[len - 2] = f[len - 2] or (fLimb shl 30)
                g[len - 2] = g[len - 2] or (gLimb shl 30)
                --len
            }

            return len
        }

        private fun updateDE30(
            len: Int,
            d: IntArray,
            e: IntArray,
            t: IntArray,
            m0Inv: Int,
            m: IntArray
        ) {
            val u = t[0]
            val v = t[1]
            val q = t[2]
            val r = t[3]

            val cD = d[len - 1] shr 31
            val cE = e[len - 1] shr 31

            var x = (u and cD) + (v and cE)
            var y = (q and cD) + (r and cE)

            var m0 = m[0]
            var d0 = d[0]
            var e0 = e[0]

            var t0 = u.toLong() * d0 + v.toLong() * e0
            var t1 = q.toLong() * d0 + r.toLong() * e0

            x -= (m0Inv * t0.toInt() + x) and M30
            y -= (m0Inv * t1.toInt() + y) and M30

            t0 += m0.toLong() * x
            t1 += m0.toLong() * y

            t0 = t0 shr 30
            t1 = t1 shr 30

            for (i in 1 until len) {
                m0 = m[i]
                d0 = d[i]
                e0 = e[i]
                t0 += u.toLong() * d0 + v.toLong() * e0 + m0.toLong() * x
                t1 += q.toLong() * d0 + r.toLong() * e0 + m0.toLong() * y
                d[i - 1] = t0.toInt() and M30
                t0 = t0 shr 30
                e[i - 1] = t1.toInt() and M30
                t1 = t1 shr 30
            }

            d[len - 1] = t0.toInt()
            e[len - 1] = t1.toInt()
        }

        private fun updateFG30(
            len: Int,
            f: IntArray,
            g: IntArray,
            t: IntArray
        ) {
            val u = t[0]
            val v = t[1]
            val q = t[2]
            val r = t[3]

            var f0 = f[0]
            var g0 = g[0]

            var t0 = u.toLong() * f0 + v.toLong() * g0
            var t1 = q.toLong() * f0 + r.toLong() * g0

            t0 = t0 shr 30
            t1 = t1 shr 30

            for (i in 1 until len) {
                f0 = f[i]
                g0 = g[i]
                t0 += u.toLong() * f0 + v.toLong() * g0
                t1 += q.toLong() * f0 + r.toLong() * g0
                f[i - 1] = t0.toInt() and M30
                t0 = t0 shr 30
                g[i - 1] = t1.toInt() and M30
                t1 = t1 shr 30
            }

            f[len - 1] = t0.toInt()
            g[len - 1] = t1.toInt()
        }
    }

    private object Scalar25519 {
        const val SIZE = 8

        private const val SCALAR_BYTES = SIZE * 4

        private const val M08L = 0x000000FFL
        private const val M28L = 0x0FFFFFFFL
        private const val M32L = 0xFFFFFFFFL

        private const val TARGET_LENGTH = 254

        private val orderL = intArrayOf(
            0x5CF5D3ED, 0x5812631A, 0xA2F79CD6.toInt(), 0x14DEF9DE,
            0x00000000, 0x00000000, 0x00000000, 0x10000000
        )
        private val orderLSq = intArrayOf(
            0xAB128969.toInt(), 0xE2EDF685.toInt(), 0x2298A31D, 0x68039276,
            0xD217F5BE.toInt(), 0x3DCEEC73, 0x1B7C309A, 0xA1B39941.toInt(),
            0x4B9EBA7D, 0xCB024C63.toInt(), 0xD45EF39A.toInt(), 0x029BDF3B,
            0x00000000, 0x00000000, 0x00000000, 0x01000000
        )

        private const val L0 = -0x030A2C13
        private const val L1 = 0x012631A6
        private const val L2 = 0x079CD658
        private const val L3 = -0x006215D1
        private const val L4 = 0x000014DF

        fun checkVar(
            s: ByteArray,
            n: IntArray
        ): Boolean {
            decode(s, n)
            return !Nat256.gte(n, orderL)
        }

        fun decode(
            k: ByteArray,
            n: IntArray
        ) {
            Codec.decode32(k, 0, n, 0, SIZE)
        }

        fun multiply128Var(
            x: IntArray,
            y128: IntArray,
            z: IntArray
        ) {
            val tt = IntArray(12)
            Nat256.mul128(x, y128, tt)

            if (y128[3] < 0) {
                Nat256.addTo(orderL, 0, tt, 4, 0)
                Nat256.subFrom(x, 0, tt, 4, 0)
            }

            val bytes = ByteArray(48)
            Codec.encode32(tt, 0, 12, bytes, 0)

            val r = reduce384(bytes)
            decode(r, z)
        }

        fun reduce384(n: ByteArray): ByteArray {
            var x00 = Codec.decode32(n, 0).toLong() and M32L
            var x01 = (Codec.decode24(n, 4) shl 4).toLong() and M32L
            var x02 = Codec.decode32(n, 7).toLong() and M32L
            var x03 = (Codec.decode24(n, 11) shl 4).toLong() and M32L
            var x04 = Codec.decode32(n, 14).toLong() and M32L
            var x05 = (Codec.decode24(n, 18) shl 4).toLong() and M32L
            var x06 = Codec.decode32(n, 21).toLong() and M32L
            var x07 = (Codec.decode24(n, 25) shl 4).toLong() and M32L
            var x08 = Codec.decode32(n, 28).toLong() and M32L
            var x09 = (Codec.decode24(n, 32) shl 4).toLong() and M32L
            var x10 = Codec.decode32(n, 35).toLong() and M32L
            var x11 = (Codec.decode24(n, 39) shl 4).toLong() and M32L
            var x12 = Codec.decode32(n, 42).toLong() and M32L
            var x13 = (Codec.decode16(n, 46) shl 4).toLong() and M32L

            x13 += x12 shr 28
            x12 = x12 and M28L
            x04 -= x13 * L0
            x05 -= x13 * L1
            x06 -= x13 * L2
            x07 -= x13 * L3
            x08 -= x13 * L4

            x12 += x11 shr 28
            x11 = x11 and M28L
            x03 -= x12 * L0
            x04 -= x12 * L1
            x05 -= x12 * L2
            x06 -= x12 * L3
            x07 -= x12 * L4

            x11 += x10 shr 28
            x10 = x10 and M28L
            x02 -= x11 * L0
            x03 -= x11 * L1
            x04 -= x11 * L2
            x05 -= x11 * L3
            x06 -= x11 * L4

            x10 += x09 shr 28
            x09 = x09 and M28L
            x01 -= x10 * L0
            x02 -= x10 * L1
            x03 -= x10 * L2
            x04 -= x10 * L3
            x05 -= x10 * L4

            x08 += x07 shr 28
            x07 = x07 and M28L
            x09 += x08 shr 28
            x08 = x08 and M28L

            val t = x08 ushr 27
            x09 += t

            x00 -= x09 * L0
            x01 -= x09 * L1
            x02 -= x09 * L2
            x03 -= x09 * L3
            x04 -= x09 * L4

            x01 += x00 shr 28
            x00 = x00 and M28L
            x02 += x01 shr 28
            x01 = x01 and M28L
            x03 += x02 shr 28
            x02 = x02 and M28L
            x04 += x03 shr 28
            x03 = x03 and M28L
            x05 += x04 shr 28
            x04 = x04 and M28L
            x06 += x05 shr 28
            x05 = x05 and M28L
            x07 += x06 shr 28
            x06 = x06 and M28L
            x08 += x07 shr 28
            x07 = x07 and M28L
            x09 = x08 shr 28
            x08 = x08 and M28L

            x09 -= t

            x00 += x09 and L0.toLong()
            x01 += x09 and L1.toLong()
            x02 += x09 and L2.toLong()
            x03 += x09 and L3.toLong()
            x04 += x09 and L4.toLong()

            x01 += x00 shr 28
            x00 = x00 and M28L
            x02 += x01 shr 28
            x01 = x01 and M28L
            x03 += x02 shr 28
            x02 = x02 and M28L
            x04 += x03 shr 28
            x03 = x03 and M28L
            x05 += x04 shr 28
            x04 = x04 and M28L
            x06 += x05 shr 28
            x05 = x05 and M28L
            x07 += x06 shr 28
            x06 = x06 and M28L
            x08 += x07 shr 28
            x07 = x07 and M28L

            val r = ByteArray(64)
            Codec.encode56(x00 or (x01 shl 28), r, 0)
            Codec.encode56(x02 or (x03 shl 28), r, 7)
            Codec.encode56(x04 or (x05 shl 28), r, 14)
            Codec.encode56(x06 or (x07 shl 28), r, 21)
            Codec.encode32(x08.toInt(), r, 28)
            return r
        }

        fun reduce512(n: ByteArray): ByteArray {
            var x00 = Codec.decode32(n, 0).toLong() and M32L
            var x01 = (Codec.decode24(n, 4) shl 4).toLong() and M32L
            var x02 = Codec.decode32(n, 7).toLong() and M32L
            var x03 = (Codec.decode24(n, 11) shl 4).toLong() and M32L
            var x04 = Codec.decode32(n, 14).toLong() and M32L
            var x05 = (Codec.decode24(n, 18) shl 4).toLong() and M32L
            var x06 = Codec.decode32(n, 21).toLong() and M32L
            var x07 = (Codec.decode24(n, 25) shl 4).toLong() and M32L
            var x08 = Codec.decode32(n, 28).toLong() and M32L
            var x09 = (Codec.decode24(n, 32) shl 4).toLong() and M32L
            var x10 = Codec.decode32(n, 35).toLong() and M32L
            var x11 = (Codec.decode24(n, 39) shl 4).toLong() and M32L
            var x12 = Codec.decode32(n, 42).toLong() and M32L
            var x13 = (Codec.decode24(n, 46) shl 4).toLong() and M32L
            var x14 = Codec.decode32(n, 49).toLong() and M32L
            var x15 = (Codec.decode24(n, 53) shl 4).toLong() and M32L
            var x16 = Codec.decode32(n, 56).toLong() and M32L
            var x17 = (Codec.decode24(n, 60) shl 4).toLong() and M32L
            val x18 = n[63].toLong() and M08L

            x09 -= x18 * L0
            x10 -= x18 * L1
            x11 -= x18 * L2
            x12 -= x18 * L3
            x13 -= x18 * L4

            x17 += x16 shr 28
            x16 = x16 and M28L
            x08 -= x17 * L0
            x09 -= x17 * L1
            x10 -= x17 * L2
            x11 -= x17 * L3
            x12 -= x17 * L4

            x07 -= x16 * L0
            x08 -= x16 * L1
            x09 -= x16 * L2
            x10 -= x16 * L3
            x11 -= x16 * L4

            x15 += x14 shr 28
            x14 = x14 and M28L
            x06 -= x15 * L0
            x07 -= x15 * L1
            x08 -= x15 * L2
            x09 -= x15 * L3
            x10 -= x15 * L4

            x05 -= x14 * L0
            x06 -= x14 * L1
            x07 -= x14 * L2
            x08 -= x14 * L3
            x09 -= x14 * L4

            x13 += x12 shr 28
            x12 = x12 and M28L
            x04 -= x13 * L0
            x05 -= x13 * L1
            x06 -= x13 * L2
            x07 -= x13 * L3
            x08 -= x13 * L4

            x12 += x11 shr 28
            x11 = x11 and M28L
            x03 -= x12 * L0
            x04 -= x12 * L1
            x05 -= x12 * L2
            x06 -= x12 * L3
            x07 -= x12 * L4

            x11 += x10 shr 28
            x10 = x10 and M28L
            x02 -= x11 * L0
            x03 -= x11 * L1
            x04 -= x11 * L2
            x05 -= x11 * L3
            x06 -= x11 * L4

            x10 += x09 shr 28
            x09 = x09 and M28L
            x01 -= x10 * L0
            x02 -= x10 * L1
            x03 -= x10 * L2
            x04 -= x10 * L3
            x05 -= x10 * L4

            x08 += x07 shr 28
            x07 = x07 and M28L
            x09 += x08 shr 28
            x08 = x08 and M28L

            val t: Long = x08 ushr 27
            x09 += t

            x00 -= x09 * L0
            x01 -= x09 * L1
            x02 -= x09 * L2
            x03 -= x09 * L3
            x04 -= x09 * L4

            x01 += x00 shr 28
            x00 = x00 and M28L
            x02 += x01 shr 28
            x01 = x01 and M28L
            x03 += x02 shr 28
            x02 = x02 and M28L
            x04 += x03 shr 28
            x03 = x03 and M28L
            x05 += x04 shr 28
            x04 = x04 and M28L
            x06 += x05 shr 28
            x05 = x05 and M28L
            x07 += x06 shr 28
            x06 = x06 and M28L
            x08 += x07 shr 28
            x07 = x07 and M28L
            x09 = x08 shr 28
            x08 = x08 and M28L

            x09 -= t

            x00 += x09 and L0.toLong()
            x01 += x09 and L1.toLong()
            x02 += x09 and L2.toLong()
            x03 += x09 and L3.toLong()
            x04 += x09 and L4.toLong()

            x01 += x00 shr 28
            x00 = x00 and M28L
            x02 += x01 shr 28
            x01 = x01 and M28L
            x03 += x02 shr 28
            x02 = x02 and M28L
            x04 += x03 shr 28
            x03 = x03 and M28L
            x05 += x04 shr 28
            x04 = x04 and M28L
            x06 += x05 shr 28
            x05 = x05 and M28L
            x07 += x06 shr 28
            x06 = x06 and M28L
            x08 += x07 shr 28
            x07 = x07 and M28L

            val r = ByteArray(SCALAR_BYTES)
            Codec.encode56(x00 or (x01 shl 28), r, 0)
            Codec.encode56(x02 or (x03 shl 28), r, 7)
            Codec.encode56(x04 or (x05 shl 28), r, 14)
            Codec.encode56(x06 or (x07 shl 28), r, 21)
            Codec.encode32(x08.toInt(), r, 28)
            return r
        }

        /**
         * 把标量 k 拆成两个半长标量 z0 与 z1
         */
        fun reduceBasisVar(
            k: IntArray,
            z0: IntArray,
            z1: IntArray
        ): Boolean {
            var nu = IntArray(16)
            System.arraycopy(orderLSq, 0, nu, 0, 16)
            var nv = IntArray(16)
            Nat256.square(k, nv)
            ++nv[0]
            val p = IntArray(16)
            Nat256.mul(orderL, k, p)
            val t = IntArray(16)
            var u0 = IntArray(4)
            System.arraycopy(orderL, 0, u0, 0, 4)
            var u1 = IntArray(4)
            var v0 = IntArray(4)
            System.arraycopy(k, 0, v0, 0, 4)
            var v1 = IntArray(4)
            v1[0] = 1

            var iterations = TARGET_LENGTH * 4
            var last = 15
            var lenNv = ScalarUtil.getBitLengthPositive(last, nv)

            while (lenNv > TARGET_LENGTH) {
                if (--iterations < 0) {
                    return false
                }

                val lenP = ScalarUtil.getBitLength(last, p)
                var s = lenP - lenNv
                s = s and (s shr 31).inv()

                if (p[last] < 0) {
                    ScalarUtil.addShiftedNP(last, s, nu, nv, p, t)
                    ScalarUtil.addShiftedUV(3, s, u0, u1, v0, v1)
                } else {
                    ScalarUtil.subShiftedNP(last, s, nu, nv, p, t)
                    ScalarUtil.subShiftedUV(3, s, u0, u1, v0, v1)
                }

                if (ScalarUtil.lessThanUnsigned(last, nu, nv)) {
                    val t0 = u0
                    u0 = v0
                    v0 = t0
                    val t1 = u1
                    u1 = v1
                    v1 = t1
                    val tN = nu
                    nu = nv
                    nv = tN

                    last = lenNv ushr 5
                    lenNv = ScalarUtil.getBitLengthPositive(last, nv)
                }
            }

            System.arraycopy(v0, 0, z0, 0, 4)
            System.arraycopy(v1, 0, z1, 0, 4)
            return true
        }

        fun toSignedDigits(
            z: IntArray
        ) {
            Nat.cAddTo(SIZE, z[0].inv() and 1, orderL, z)
            Nat.shiftDownBit(SIZE, z, 1)
        }
    }

    private object ScalarUtil {
        private const val M = 0xFFFFFFFFL

        fun addShiftedNP(
            last: Int,
            s: Int,
            nu: IntArray,
            nv: IntArray,
            p: IntArray,
            t: IntArray
        ) {
            var ccP = 0L
            var ccNu = 0L

            if (s == 0) {
                for (i in 0..last) {
                    var pi = p[i]

                    ccNu += nu[i].toLong() and M
                    ccNu += pi.toLong() and M

                    ccP += pi.toLong() and M
                    ccP += nv[i].toLong() and M
                    pi = ccP.toInt()
                    ccP = ccP ushr 32
                    p[i] = pi

                    ccNu += pi.toLong() and M
                    nu[i] = ccNu.toInt()
                    ccNu = ccNu ushr 32
                }
            } else if (s < 32) {
                var prevP = 0
                var prevQ = 0
                var prevV = 0

                for (i in 0..last) {
                    val pi = p[i]
                    val ps = (pi shl s) or (prevP ushr -s)
                    prevP = pi

                    ccNu += nu[i].toLong() and M
                    ccNu += ps.toLong() and M

                    val nextV = nv[i]
                    val vs = (nextV shl s) or (prevV ushr -s)
                    prevV = nextV

                    ccP += pi.toLong() and M
                    ccP += vs.toLong() and M
                    val pi2 = ccP.toInt()
                    ccP = ccP ushr 32
                    p[i] = pi2

                    val qs = (pi2 shl s) or (prevQ ushr -s)
                    prevQ = pi2

                    ccNu += qs.toLong() and M
                    nu[i] = ccNu.toInt()
                    ccNu = ccNu ushr 32
                }
            } else {
                System.arraycopy(p, 0, t, 0, last)

                val sWords = s ushr 5
                val sBits = s and 31
                if (sBits == 0) {
                    for (i in sWords..last) {
                        ccNu += nu[i].toLong() and M
                        ccNu += t[i - sWords].toLong() and M

                        ccP += p[i].toLong() and M
                        ccP += nv[i - sWords].toLong() and M
                        p[i] = ccP.toInt()
                        ccP = ccP ushr 32

                        ccNu += p[i - sWords].toLong() and M
                        nu[i] = ccNu.toInt()
                        ccNu = ccNu ushr 32
                    }
                } else {
                    var prevT = 0
                    var prevQ = 0
                    var prevV = 0

                    for (i in sWords..last) {
                        val nextT = t[i - sWords]
                        val ts = (nextT shl sBits) or (prevT ushr -sBits)
                        prevT = nextT

                        ccNu += nu[i].toLong() and M
                        ccNu += ts.toLong() and M

                        val nextV = nv[i - sWords]
                        val vs = (nextV shl sBits) or (prevV ushr -sBits)
                        prevV = nextV

                        ccP += p[i].toLong() and M
                        ccP += vs.toLong() and M
                        p[i] = ccP.toInt()
                        ccP = ccP ushr 32

                        val nextQ = p[i - sWords]
                        val qs = (nextQ shl sBits) or (prevQ ushr -sBits)
                        prevQ = nextQ

                        ccNu += qs.toLong() and M
                        nu[i] = ccNu.toInt()
                        ccNu = ccNu ushr 32
                    }
                }
            }
        }

        fun addShiftedUV(
            last: Int,
            s: Int,
            u0: IntArray,
            u1: IntArray,
            v0: IntArray,
            v1: IntArray
        ) {
            val sWords = s ushr 5
            val sBits = s and 31

            var ccU0 = 0L
            var ccU1 = 0L

            if (sBits == 0) {
                for (i in sWords..last) {
                    ccU0 += u0[i].toLong() and M
                    ccU1 += u1[i].toLong() and M
                    ccU0 += v0[i - sWords].toLong() and M
                    ccU1 += v1[i - sWords].toLong() and M
                    u0[i] = ccU0.toInt()
                    ccU0 = ccU0 ushr 32
                    u1[i] = ccU1.toInt()
                    ccU1 = ccU1 ushr 32
                }
            } else {
                var prevV0 = 0
                var prevV1 = 0

                for (i in sWords..last) {
                    val nextV0 = v0[i - sWords]
                    val nextV1 = v1[i - sWords]
                    val v0s = (nextV0 shl sBits) or (prevV0 ushr -sBits)
                    val v1s = (nextV1 shl sBits) or (prevV1 ushr -sBits)
                    prevV0 = nextV0
                    prevV1 = nextV1

                    ccU0 += u0[i].toLong() and M
                    ccU1 += u1[i].toLong() and M
                    ccU0 += v0s.toLong() and M
                    ccU1 += v1s.toLong() and M
                    u0[i] = ccU0.toInt()
                    ccU0 = ccU0 ushr 32
                    u1[i] = ccU1.toInt()
                    ccU1 = ccU1 ushr 32
                }
            }
        }

        fun getBitLength(
            last: Int,
            x: IntArray
        ): Int {
            var i = last
            val sign = x[i] shr 31
            while (i > 0 && x[i] == sign) {
                --i
            }
            return i * Integers.SIZE + Integers.bitLength(x[i] xor sign)
        }

        fun getBitLengthPositive(
            last: Int,
            x: IntArray
        ): Int {
            var i = last
            while (i > 0 && x[i] == 0) {
                --i
            }
            return i * Integers.SIZE + Integers.bitLength(x[i])
        }

        fun lessThanUnsigned(
            last: Int,
            x: IntArray,
            y: IntArray
        ): Boolean {
            var i = last
            do {
                val xi = x[i] + Integer.MIN_VALUE
                val yi = y[i] + Integer.MIN_VALUE
                if (xi < yi) {
                    return true
                }
                if (xi > yi) {
                    return false
                }
            } while (--i >= 0)
            return false
        }

        fun subShiftedNP(
            last: Int,
            s: Int,
            nu: IntArray,
            nv: IntArray,
            p: IntArray,
            t: IntArray
        ) {
            var ccP = 0L
            var ccNu = 0L

            if (s == 0) {
                for (i in 0..last) {
                    var pi = p[i]

                    ccNu += nu[i].toLong() and M
                    ccNu -= pi.toLong() and M

                    ccP += pi.toLong() and M
                    ccP -= nv[i].toLong() and M
                    pi = ccP.toInt()
                    ccP = ccP shr 32
                    p[i] = pi

                    ccNu -= pi.toLong() and M
                    nu[i] = ccNu.toInt()
                    ccNu = ccNu shr 32
                }
            } else if (s < 32) {
                var prevP = 0
                var prevQ = 0
                var prevV = 0

                for (i in 0..last) {
                    val pi = p[i]
                    val ps = (pi shl s) or (prevP ushr -s)
                    prevP = pi

                    ccNu += nu[i].toLong() and M
                    ccNu -= ps.toLong() and M

                    val nextV = nv[i]
                    val vs = (nextV shl s) or (prevV ushr -s)
                    prevV = nextV

                    ccP += pi.toLong() and M
                    ccP -= vs.toLong() and M
                    val pi2 = ccP.toInt()
                    ccP = ccP shr 32
                    p[i] = pi2

                    val qs = (pi2 shl s) or (prevQ ushr -s)
                    prevQ = pi2

                    ccNu -= qs.toLong() and M
                    nu[i] = ccNu.toInt()
                    ccNu = ccNu shr 32
                }
            } else {
                System.arraycopy(p, 0, t, 0, last)

                val sWords = s ushr 5
                val sBits = s and 31
                if (sBits == 0) {
                    for (i in sWords..last) {
                        ccNu += nu[i].toLong() and M
                        ccNu -= t[i - sWords].toLong() and M

                        ccP += p[i].toLong() and M
                        ccP -= nv[i - sWords].toLong() and M
                        p[i] = ccP.toInt()
                        ccP = ccP shr 32

                        ccNu -= p[i - sWords].toLong() and M
                        nu[i] = ccNu.toInt()
                        ccNu = ccNu shr 32
                    }
                } else {
                    var prevT = 0
                    var prevQ = 0
                    var prevV = 0

                    for (i in sWords..last) {
                        val nextT = t[i - sWords]
                        val ts = (nextT shl sBits) or (prevT ushr -sBits)
                        prevT = nextT

                        ccNu += nu[i].toLong() and M
                        ccNu -= ts.toLong() and M

                        val nextV = nv[i - sWords]
                        val vs = (nextV shl sBits) or (prevV ushr -sBits)
                        prevV = nextV

                        ccP += p[i].toLong() and M
                        ccP -= vs.toLong() and M
                        p[i] = ccP.toInt()
                        ccP = ccP shr 32

                        val nextQ = p[i - sWords]
                        val qs = (nextQ shl sBits) or (prevQ ushr -sBits)
                        prevQ = nextQ

                        ccNu -= qs.toLong() and M
                        nu[i] = ccNu.toInt()
                        ccNu = ccNu shr 32
                    }
                }
            }
        }

        fun subShiftedUV(
            last: Int,
            s: Int,
            u0: IntArray,
            u1: IntArray,
            v0: IntArray,
            v1: IntArray
        ) {
            val sWords = s ushr 5
            val sBits = s and 31

            var ccU0 = 0L
            var ccU1 = 0L

            if (sBits == 0) {
                for (i in sWords..last) {
                    ccU0 += u0[i].toLong() and M
                    ccU1 += u1[i].toLong() and M
                    ccU0 -= v0[i - sWords].toLong() and M
                    ccU1 -= v1[i - sWords].toLong() and M
                    u0[i] = ccU0.toInt()
                    ccU0 = ccU0 shr 32
                    u1[i] = ccU1.toInt()
                    ccU1 = ccU1 shr 32
                }
            } else {
                var prevV0 = 0
                var prevV1 = 0

                for (i in sWords..last) {
                    val nextV0 = v0[i - sWords]
                    val nextV1 = v1[i - sWords]
                    val v0s = (nextV0 shl sBits) or (prevV0 ushr -sBits)
                    val v1s = (nextV1 shl sBits) or (prevV1 ushr -sBits)
                    prevV0 = nextV0
                    prevV1 = nextV1

                    ccU0 += u0[i].toLong() and M
                    ccU1 += u1[i].toLong() and M
                    ccU0 -= v0s.toLong() and M
                    ccU1 -= v1s.toLong() and M
                    u0[i] = ccU0.toInt()
                    ccU0 = ccU0 shr 32
                    u1[i] = ccU1.toInt()
                    ccU1 = ccU1 shr 32
                }
            }
        }
    }

    private object Codec {
        fun decode16(
            bs: ByteArray,
            off: Int
        ): Int {
            var n = bs[off].toInt() and 0xFF
            n = n or ((bs[off + 1].toInt() and 0xFF) shl 8)
            return n
        }

        fun decode24(
            bs: ByteArray,
            off: Int
        ): Int {
            var n = bs[off].toInt() and 0xFF
            n = n or ((bs[off + 1].toInt() and 0xFF) shl 8)
            n = n or ((bs[off + 2].toInt() and 0xFF) shl 16)
            return n
        }

        fun decode32(
            bs: ByteArray,
            off: Int
        ): Int {
            var n = bs[off].toInt() and 0xFF
            n = n or ((bs[off + 1].toInt() and 0xFF) shl 8)
            n = n or ((bs[off + 2].toInt() and 0xFF) shl 16)
            n = n or (bs[off + 3].toInt() shl 24)
            return n
        }

        fun decode32(
            bs: ByteArray,
            bsOff: Int,
            n: IntArray,
            nOff: Int,
            nLen: Int
        ) {
            for (i in 0 until nLen) {
                n[nOff + i] = decode32(bs, bsOff + i * 4)
            }
        }

        fun encode24(
            n: Int,
            bs: ByteArray,
            off: Int
        ) {
            bs[off] = n.toByte()
            bs[off + 1] = (n ushr 8).toByte()
            bs[off + 2] = (n ushr 16).toByte()
        }

        fun encode32(
            n: Int,
            bs: ByteArray,
            off: Int
        ) {
            bs[off] = n.toByte()
            bs[off + 1] = (n ushr 8).toByte()
            bs[off + 2] = (n ushr 16).toByte()
            bs[off + 3] = (n ushr 24).toByte()
        }

        fun encode32(
            n: IntArray,
            nOff: Int,
            nLen: Int,
            bs: ByteArray,
            bsOff: Int
        ) {
            for (i in 0 until nLen) {
                encode32(n[nOff + i], bs, bsOff + i * 4)
            }
        }

        fun encode56(
            n: Long,
            bs: ByteArray,
            off: Int
        ) {
            encode32(n.toInt(), bs, off)
            encode24((n ushr 32).toInt(), bs, off + 4)
        }
    }

    private object Wnaf {
        fun getSignedVar(
            n: IntArray,
            width: Int,
            ws: ByteArray
        ) {
            val t = IntArray(n.size * 2)
            run {
                var c = n[n.size - 1] shr 31
                var i = n.size
                var tPos = t.size
                while (--i >= 0) {
                    val next = n[i]
                    t[--tPos] = (next ushr 16) or (c shl 16)
                    c = next
                    t[--tPos] = c
                }
            }

            val lead = 32 - width

            var j = 0
            var carry = 0
            var i = 0
            while (i < t.size) {
                val word = t[i]
                while (j < 16) {
                    val word16 = word ushr j

                    val bit = word16 and 1
                    if (bit == carry) {
                        ++j
                        continue
                    }

                    val digit = (word16 or 1) shl lead
                    carry = digit ushr 31

                    ws[(i shl 4) + j] = (digit shr lead).toByte()

                    j += width
                }
                ++i
                j -= 16
            }
        }
    }

    private object Interleave {
        /**
         * 移植自 BouncyCastle 的 Bits.bitPermuteStep
         */
        private fun bitPermuteStep(
            x: Int,
            m: Int,
            s: Int
        ): Int {
            val t = (x xor (x ushr s)) and m
            return (t xor (t shl s)) xor x
        }

        fun shuffle2(x: Int): Int {
            var r = x
            r = bitPermuteStep(r, 0x00AA00AA, 7)
            r = bitPermuteStep(r, 0x0000CCCC, 14)
            r = bitPermuteStep(r, 0x00F000F0, 4)
            r = bitPermuteStep(r, 0x0000FF00, 8)
            return r
        }
    }

    private object Integers {
        const val SIZE = 32

        fun bitLength(i: Int): Int = SIZE - Integer.numberOfLeadingZeros(i)

        fun compare(
            x: Int,
            y: Int
        ): Int = if (x < y) -1 else if (x == y) 0 else 1

        fun compareUnsigned(
            x: Int,
            y: Int
        ): Int =
            compare(x + Integer.MIN_VALUE, y + Integer.MIN_VALUE)

        fun numberOfLeadingZeros(i: Int): Int = Integer.numberOfLeadingZeros(i)

        fun numberOfTrailingZeros(i: Int): Int =
            Integer.numberOfTrailingZeros(i)
    }

    private object Arrays {
        fun validateSegment(
            buf: ByteArray?,
            off: Int,
            len: Int
        ) {
            if (buf == null) {
                throw NullPointerException("'buf' cannot be null")
            }
            val available = buf.size - off
            val remaining = available - len
            if ((off or len or available or remaining) < 0) {
                throw IndexOutOfBoundsException(
                    "buf.length: " + buf.size +
                        ", off: " + off + ", len: " + len
                )
            }
        }
    }
}