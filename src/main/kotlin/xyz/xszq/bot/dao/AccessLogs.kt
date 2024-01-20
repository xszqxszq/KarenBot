package xyz.xszq.bot.dao

import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.bson.types.ObjectId
import java.time.LocalDateTime


object AccessLogs {
    lateinit var collection: MongoCollection<AccessLog>
//    private val lock = Semaphore(128)
    suspend fun saveLog(subject: String, context: String, content: String) = kotlin.runCatching {
//        lock.withPermit {
            collection.insertOne(AccessLog(ObjectId(), subject, context, content, LocalDateTime.now()))
//        }
    }
}