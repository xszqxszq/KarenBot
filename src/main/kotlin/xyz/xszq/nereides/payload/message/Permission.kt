package xyz.xszq.nereides.payload.message

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Permission(
    val type: Int = EVERYONE
) {
    companion object {
        const val SPECIFIED_USERS = 0
        const val OPERATORS = 1
        const val EVERYONE = 2
        const val SPECIFIED_ROLES = 3
    }
}
