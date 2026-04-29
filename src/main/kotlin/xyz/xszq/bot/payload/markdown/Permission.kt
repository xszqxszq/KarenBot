package xyz.xszq.bot.payload.markdown

import kotlinx.serialization.Serializable

@Suppress("unused")
@Serializable
data class Permission(
    val type: Int
) {
    companion object {
        const val SPECIFIED_USERS = 0
        const val OPERATORS = 1
        const val EVERYONE = 2
        const val SPECIFIED_ROLES = 3
    }
}