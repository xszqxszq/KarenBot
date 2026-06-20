package xyz.xszq.bot

enum class MemberRole {
    Member, Admin, Owner;
    companion object {
        fun of(name: String) = when (name) {
            "admin" -> Admin
            "owner" -> Owner
            else -> Member
        }
    }
}