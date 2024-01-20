package xyz.xszq.bot.text

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDateTime

object WakeSleep {
    private val sleep = mutableMapOf<String, MutableList<Pair<String, LocalDateTime>>>()
    private val wakeUp = mutableMapOf<String, MutableList<Pair<String, LocalDateTime>>>()
    private val mutex = Mutex()
    suspend fun getValue(map: MutableMap<String, MutableList<Pair<String, LocalDateTime>>>, key: String): MutableList<Pair<String, LocalDateTime>> = mutex.withLock {
        if (!map.containsKey(key))
            map[key] = mutableListOf()
        return map[key]!!
    }
//    suspend fun clearSleep() = WakeSleep.mutex.withLock {
//        sleep.forEach { (context, values) ->
//            values.forEach { (subject, sleptTime) ->
//                // 20:00 - 07:00 睡觉，05:00 - 12:00 起床
//                if (LocalDateTime.now())
//            }
//        }
//    }
//    suspend fun sleep(context: String, subject: String): Int? {
//        val slept = getValue(sleep, context)
//        return mutex.withLock {
//            if (slept.any { it == subject })
//                return@withLock null
//            slept.size.also {
//                slept.add(subject)
//            }
//        }
//    }
//    suspend fun wakeUp(context: String, subject: String): Int? {
//        val wake = getValue(wakeUp, context)
//        return mutex.withLock {
//            if (wake.any { it == subject })
//                return@withLock null
//            wake.size.also {
//                wake.add(subject)
//            }
//        }
//    }
}