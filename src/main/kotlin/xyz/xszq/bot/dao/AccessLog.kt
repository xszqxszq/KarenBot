package xyz.xszq.bot.dao

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import java.time.LocalDateTime

data class AccessLog(
    @BsonId
    val id: ObjectId,
    val openid: String,
    val context: String,
    val content: String,
    val date: LocalDateTime
)
