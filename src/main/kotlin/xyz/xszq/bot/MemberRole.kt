package xyz.xszq.bot

/**
 * 群成员在群内的职能
 */
enum class MemberRole {
    Member, Admin, Owner;
    companion object {
        /**
         * 由名称解析对应职能
         *
         * @param name 职能名称
         * @return 解析出的职能
         */
        fun of(name: String) = when (name) {
            "admin" -> Admin
            "owner" -> Owner
            else -> Member
        }
    }
}