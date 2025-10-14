package xyz.xszq.bot.music

sealed interface Response {
    val name: String
    val rating: Int
    val course: Int
    val icon: Int
    val plate: Int
}