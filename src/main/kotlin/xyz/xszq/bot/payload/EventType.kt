package xyz.xszq.bot.payload

/**
 * 事件类型
 */
@Suppress("ConstPropertyName")
object EventType {
    object C2C {
        const val Message = "C2C_MESSAGE_CREATE"
        const val Add = "FRIEND_ADD"
        const val Remove = "FRIEND_DEL"
        const val Receive = "C2C_MSG_RECEIVE"
        const val Reject = "C2C_MSG_REJECT"
    }
    object Group {
        const val Message = "GROUP_MESSAGE_CREATE"
        const val AtMessage = "GROUP_AT_MESSAGE_CREATE"
        const val Add = "GROUP_ADD_ROBOT"
        const val Remove = "GROUP_DEL_ROBOT"
        const val Receive = "GROUP_MSG_RECEIVE"
        const val Reject = "GROUP_MSG_REJECT"
        const val MemberAdd = "GROUP_MEMBER_ADD"
        const val MemberRemove = "GROUP_MEMBER_REMOVE"
    }
    const val Interaction = "INTERACTION_CREATE"
}