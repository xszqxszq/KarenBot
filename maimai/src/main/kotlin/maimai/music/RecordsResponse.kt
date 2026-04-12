package xyz.xszq.bot.maimai.music

class RecordsResponse(
    override val player: PlayerInfo,
    override var settings: PlayerSettings? = null,
    val records: List<Record>
): Response