package xyz.xszq.bot.dao

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object AccessLog: Table("access_log") {
    val openid = varchar("openid", 32)
    val context = varchar("context", 32)
    val date = datetime("date").clientDefault {
        LocalDateTime.now()
    }
}