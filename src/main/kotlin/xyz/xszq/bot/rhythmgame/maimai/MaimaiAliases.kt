package xyz.xszq.bot.rhythmgame.maimai

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import xyz.xszq.bot.dao.MaimaiAliases
import xyz.xszq.bot.dao.transactionWithLock
import xyz.xszq.bot.rhythmgame.maimai.payload.MusicInfo

class MaimaiAliases(private val musicsInfo: MaimaiMusic) {
    private val json = Json {
        prettyPrint = true; isLenient = true; ignoreUnknownKeys = true
    }
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
        expectSuccess = false
    }
    private var aliases = mapOf<String, List<String>>()
    private val mutex = Mutex()
    private var cacheTime = 0L
    private suspend fun fetchFromDB() = transactionWithLock {
        MaimaiAliases.selectAll().groupBy({ it[MaimaiAliases.id] }, { it[MaimaiAliases.name] })
    }
    private suspend fun update(force: Boolean = false) {
        if (force || System.currentTimeMillis() - cacheTime >= 60 * 1000L) {
            val data = fetchFromDB()
            mutex.withLock {
                aliases = data
                cacheTime = System.currentTimeMillis()
            }
        }
    }
    private suspend fun getAll(id: String): List<String> {
        update()
        return mutex.withLock {
            aliases[id] ?: emptyList()
        }
    }
    suspend fun find(query: String): List<MusicInfo> {
        update()
        val name = query.trim().lowercase()
        return mutex.withLock {
            aliases.filter { it.value.any { n -> n.trim().lowercase() == name } }.mapNotNull { musicsInfo.getById(it.key) }
        }
    }
    suspend fun updateXrayAliases(url: String) {
        val data = client.get(url).body<Map<String, List<String>>>().flatMap { entry ->
            entry.value.map { name ->
                Pair(entry.key, name)
            }
        }
        transactionWithLock {
            MaimaiAliases.batchInsert(data, ignore = true) { (id, alias) ->
                this[MaimaiAliases.id] = id
                this[MaimaiAliases.name] = alias
            }
        }
        update(true)
    }
    suspend fun findByAlias(name: String): List<MusicInfo> = transactionWithLock {
        find(name).take(16)
    }
    suspend fun getAllAliases(id: String) = transactionWithLock {
        getAll(id)
    }
    suspend fun add(id: String, alias: String) =
        transactionWithLock {
            MaimaiAliases.upsert {
                it[this.id] = id
                it[this.name] = alias
            }
        }.also {
            update(true)
        }
    suspend fun remove(id: String, alias: String) =
        transactionWithLock {
            MaimaiAliases.deleteWhere {
                (this.id eq id) and (name eq alias)
            }
        }.also {
            update(true)
        }
}