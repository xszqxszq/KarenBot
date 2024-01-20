@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xszq.nereides.event

import xyz.xszq.nereides.Bot
import xyz.xszq.nereides.Group
import xyz.xszq.nereides.message.Message
import xyz.xszq.nereides.message.MessageChain
import xyz.xszq.nereides.message.Reply
import xyz.xszq.nereides.message.toPlainText

class GroupAtMessageEvent(
    override val bot: Bot,
    override val msgId: String,
    override val groupId: String,
    override val subjectId: String,
    override val message: MessageChain,
    override val contentString: String = message.contentToString(),
    override val timestamp: Long
): GroupEvent, PublicMessageEvent(groupId) {
    private var replySeq = 1
    val group = Group(bot, groupId)
    override suspend fun reply(content: String) = reply(MessageChain(content.toPlainText()))
    override suspend fun reply(content: Message) = reply(MessageChain(content))
    override suspend fun reply(content: MessageChain): Boolean {
        val reply = Reply(msgId, replySeq)
        return group.sendMessage(content.also {
            it.reply = reply
        }).also {
            replySeq = reply.seq
        }
    }
}