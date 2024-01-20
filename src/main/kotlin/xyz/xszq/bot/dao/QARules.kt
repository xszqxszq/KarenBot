package xyz.xszq.bot.dao

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime

object QARules: IntIdTable("qa") {
    val openid = varchar("openid", 32)
    val type = integer("type")
    val rule = text("rule")
    val reply = text("reply")
    val creator = text("creator")
    val createTime = datetime("create_time")
}