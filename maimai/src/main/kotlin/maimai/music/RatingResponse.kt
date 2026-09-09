package xyz.xszq.bot.maimai.music

/**
 * Best 50 查询结果
 */
class RatingResponse(
    override val player: PlayerInfo,
    override var settings: PlayerSettings? = null,
    var oldRatingList: List<Record>,
    var newRatingList: List<Record>
): Response