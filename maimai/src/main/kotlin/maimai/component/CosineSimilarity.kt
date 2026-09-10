package xyz.xszq.bot.maimai.component

/**
 * 余弦相似度
 */
object CosineSimilarity {
    /**
     * 计算两个向量的余弦相似度
     *
     * @param a 向量 a
     * @param b 向量 b
     * @return a 与 b 的余弦相似度
     */
    fun compute(a: FloatArray, b: FloatArray): Double {
        if (a.size != b.size || a.isEmpty()) return 0.0
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in a.indices) {
            dot += a[i].toDouble() * b[i].toDouble()
            normA += a[i].toDouble() * a[i].toDouble()
            normB += b[i].toDouble() * b[i].toDouble()
        }
        val denominator = kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB)
        return if (denominator == 0.0) 0.0 else dot / denominator
    }
}