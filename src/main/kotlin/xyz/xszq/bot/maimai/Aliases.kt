package xyz.xszq.bot.maimai

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import xyz.xszq.bot.dao.MaimaiAliases
import xyz.xszq.bot.maimai.payload.MusicInfo

class Aliases(private val musicsInfo: MusicsInfo) {
    private val json = Json {
        prettyPrint = true; isLenient = true; ignoreUnknownKeys = true
    }
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
        expectSuccess = false
    }
    suspend fun updateXrayAliases(url: String) {
        val data = client.get(url).body<Map<String, List<String>>>().flatMap { entry ->
            entry.value.map { name ->
                Pair(entry.key, name)
            }
        }
        newSuspendedTransaction(Dispatchers.IO) {
            MaimaiAliases.batchInsert(data, ignore = true) { (id, alias) ->
                this[MaimaiAliases.id] = id
                this[MaimaiAliases.name] = alias
            }
        }
    }
    suspend fun findByAlias(name: String): List<MusicInfo> = suspendedTransactionAsync(Dispatchers.IO) {
        val alias = name.trim().lowercase()
        val result = MaimaiAliases.select {
            MaimaiAliases.name.trim().lowerCase() eq alias
        }
        if (result.count() == 0L)
            listOf<MusicInfo>()
        result.mapNotNull { musicsInfo.getById(it[MaimaiAliases.id]) }.take(16)
    }.await()
    suspend fun getAllAliases(id: String) = suspendedTransactionAsync(Dispatchers.IO) {
        MaimaiAliases.select {
            MaimaiAliases.id eq id
        }.map { it[MaimaiAliases.name] }
    }.await()
    suspend fun add(id: String, alias: String) =
        newSuspendedTransaction {
            MaimaiAliases.upsert {
                it[this.id] = id
                it[this.name] = alias
            }
        }
    suspend fun remove(id: String, alias: String) =
        newSuspendedTransaction {
            MaimaiAliases.deleteWhere {
                (this.id eq id) and (name eq alias)
            }
        }
}