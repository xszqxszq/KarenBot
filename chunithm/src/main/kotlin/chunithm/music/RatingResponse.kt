package xyz.xszq.bot.chunithm.music

class RatingResponse(
    override val player: PlayerInfo,
    override var settings: PlayerSettings? = null,
    var oldRatingList: List<Record>,
    var newRatingList: List<Record>
): Response