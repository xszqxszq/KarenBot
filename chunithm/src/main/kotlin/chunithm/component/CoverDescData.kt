package xyz.xszq.bot.chunithm.component

import kotlinx.serialization.Serializable

/**
 * 封面描述与向量
 *
 * @property desc 封面描述文本
 * @property vec 描述文本的嵌入向量
 */
@Serializable
data class CoverDescData(
    val desc: String,
    val vec: FloatArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other)
            return true
        if (other !is CoverDescData)
            return false
        return desc == other.desc && vec.contentEquals(other.vec)
    }

    override fun hashCode(): Int = 31 * desc.hashCode() + vec.contentHashCode()
}