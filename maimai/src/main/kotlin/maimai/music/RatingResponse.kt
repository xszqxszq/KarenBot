package xyz.xszq.bot.maimai.music

class RatingResponse(
    override val player: PlayerInfo,
    override var settings: PlayerSettings? = null,
    var oldRatingList: List<Record>,
    var newRatingList: List<Record>
): Response