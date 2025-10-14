package xyz.xszq.bot.music

class RatingResponse(
    override val name: String,
    override var rating: Int,
    override val course: Int,
    override val icon: Int,
    override val plate: Int,
    var ratingList: List<Record>,
    var newRatingList: List<Record>
): Response