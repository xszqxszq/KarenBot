package xyz.xszq.bot.rhythmgame.maimai

import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.upsert
import xyz.xszq.bot.dao.MaimaiSettings
import xyz.xszq.bot.dao.transactionWithLock

object CustomSettings {
    suspend fun get(key: String, subjectId: String) = transactionWithLock {
        MaimaiSettings.select {
            (MaimaiSettings.openid eq subjectId) and (MaimaiSettings.name eq key)
        }.firstOrNull()
    }
    suspend fun set(key: String, value: String, subjectId: String): Unit = transactionWithLock {
        MaimaiSettings.upsert {
            it[openid] = subjectId
            it[name] = key
            it[this.value] = value
        }
    }
    suspend fun getSettings(subjectId: String) = transactionWithLock {
        MaimaiSettings.select {
            MaimaiSettings.openid eq subjectId
        }.associate {
            Pair(it[MaimaiSettings.name], it[MaimaiSettings.value])
        }
    }
}