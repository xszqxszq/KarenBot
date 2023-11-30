package xyz.xszq.bot.ffmpeg

data class Argument(val key: String, val value: String = "") {
    override fun toString(): String {
        var result = "-$key"
        if (value.isNotBlank())
            result += " $value"
        return result
    }
    fun toTypedArray(): Array<String> {
        val result = mutableListOf("-$key")
        if (value.isNotBlank())
            result.add(value)
        return result.toTypedArray()
    }
}