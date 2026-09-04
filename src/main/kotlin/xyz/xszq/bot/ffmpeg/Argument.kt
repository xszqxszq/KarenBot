package xyz.xszq.bot.ffmpeg

/**
 * FFMpeg 命令行参数
 *
 * @param key 参数名
 * @param value 参数值
 */
data class Argument(val key: String, val value: String = "") {
    override fun toString(): String {
        if (key.isBlank())
            return " $value"
        var result = "-$key"
        if (value.isNotBlank())
            result += " $value"
        return result
    }
    fun toList(): List<String> {
        val result = mutableListOf("-$key")
        if (value.isNotBlank())
            result.add(value)
        return result
    }
}