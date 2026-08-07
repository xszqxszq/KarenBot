package xyz.xszq.bot

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object BlondeDetectionCache : Table("blonde_detection_cache") {
    val md5 = varchar("md5", 32)
    val result = bool("result")
    val detectedAt = long("detected_at")
    override val primaryKey = PrimaryKey(md5)

    suspend fun get(md5: String): Boolean? = newSuspendedTransaction {
        select(result).where { BlondeDetectionCache.md5 eq md5 }
            .map { it[result] }.firstOrNull()
    }

    suspend fun put(md5: String, result: Boolean, detectedAt: Long) = newSuspendedTransaction {
        insert {
            it[this.md5] = md5
            it[this.result] = result
            it[this.detectedAt] = detectedAt
        }
    }
}
