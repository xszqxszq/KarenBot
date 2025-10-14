package xyz.xszq.bot.music

class RecordsResponse(
    override val name: String,
    override val rating: Int,
    override val course: Int,
    override val icon: Int,
    override val plate: Int,
    val records: List<Record>
): Response