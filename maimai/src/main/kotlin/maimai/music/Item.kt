package xyz.xszq.bot.maimai.music

import xyz.xszq.bot.util.toSimple

/**
 * 收藏品
 */
object Item {
    /**
     * 牌子类型
     */
    val plateTypes = listOf("極", "将", "神", "舞舞")

    /**
     * 简体与繁体/日式汉字对照表
     */
    val simplifyTable = buildMap {
        put("暁", "晓")
        put("菫", "堇")
    }

    /**
     * 将牌子名转换为简体
     *
     * @param before 转换前文本
     * @return 转换后文本
     */
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