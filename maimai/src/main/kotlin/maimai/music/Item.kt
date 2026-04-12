package xyz.xszq.bot.maimai.music

import xyz.xszq.bot.maimai.toSimple

object Item {
    val plateTypes = listOf("極", "将", "神", "舞舞")
    val simplifyTable = buildMap {
        put("暁", "晓")
        put("菫", "堇")
    }
    fun toSimplified(before: String): String {
        val version = if (before == "覇者")
            "舞"
        else
            plateTypes.firstOrNull { type -> before.endsWith(type) } ?.let {
                before.replace(it, "")
            } ?: ""
        val simplified = simplifyTable[version] ?: version.toSimple()
        return before
            .replace(version, simplified)
            .replace("極", "极")
            .replace("覇者", "霸者")
    }
}