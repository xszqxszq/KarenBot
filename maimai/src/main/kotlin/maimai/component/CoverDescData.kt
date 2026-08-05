package xyz.xszq.bot.maimai.component

import kotlinx.serialization.Serializable

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
