package xyz.xszq.bot.maimai.music

/**
 * 多曲成绩查询结果
 */
class RecordsResponse(
    override val player: PlayerInfo,
    override var settings: PlayerSettings? = null,
    val records: List<Record>
): Response