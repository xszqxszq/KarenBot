package xyz.xszq.bot.database

import xyz.xszq.bot.MemberRole

/**
 * 群信息
 *
 * @property id 群 OpenID
 * @property name 群名称
 * @property description 群简介
 * @property category 群分类
 * @property tags 群标签列表
 * @property memberCount 群成员人数
 * @property botJoinedAt 机器人入群时间
 * @property allowPush 是否接收主动推送
 * @property receiveMessageSetting 机器人接收消息的类型
 * @property botRole 机器人在群内的身份
 * @property muteMode 禁言模式
 * @property fetchedAt 上次成功拉取的时间（毫秒）
 */
data class GroupInfo(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val tags: List<String>,
    val memberCount: Int,
    val botJoinedAt: String,
    val allowPush: Boolean,
    val receiveMessageSetting: String,
    val botRole: MemberRole,
    val muteMode: String,
    val fetchedAt: Long
)