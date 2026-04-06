package xyz.xszq.bot.music

class RatingResponse(
    override val player: PlayerInfo,
    override var settings: PlayerSettings? = null,
    var oldRatingList: List<Record>,
    var newRatingList: List<Record>
): Response