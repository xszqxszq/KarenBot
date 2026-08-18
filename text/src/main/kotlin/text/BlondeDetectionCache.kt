package xyz.xszq.bot.text

import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.sql.SQLIntegrityConstraintViolationException

object BlondeDetectionCache : Table() {
    val md5 = varchar("md5", 32)
    val result = bool("result")
    val detected = long("detected")
    override val primaryKey = PrimaryKey(md5)

    suspend fun get(md5: String): Boolean? = newSuspendedTransaction {
        select(result).where { BlondeDetectionCache.md5 eq md5 }
            .map { it[result] }.firstOrNull()
    }

    suspend fun put(md5: String, result: Boolean, detectedAt: Long) = newSuspendedTransaction {
        if (select(BlondeDetectionCache.md5).where { BlondeDetectionCache.md5 eq md5 }.empty()) {
            runCatching {
                insert {
                    it[this.md5] = md5
                    it[this.result] = result
                    it[this.detected] = detectedAt
                }
            }.onFailure { e ->
                if (e !is ExposedSQLException || e.cause !is SQLIntegrityConstraintViolationException)
                    throw e
            }
        }
    }
}
