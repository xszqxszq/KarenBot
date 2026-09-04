package xyz.xszq.bot.message

import korlibs.io.file.VfsFile
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 管理所有已下载的消息文件
 * 文件在 expiresAfter 之后被延迟删除
 */
class FileManager(
    private val expiresAfter: Long = 5 * 60 * 1000L,
    private val now: () -> Long = System::currentTimeMillis,
){
    private val mutex = Mutex()
    private val files = mutableListOf<Pair<VfsFile, Long>>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private suspend fun cleanExpired(currentTime: Long) {
        files.filter { it.second <= currentTime }.forEach {
            it.first.delete()
        }
        files.removeAll { it.second <= currentTime }
    }

    private suspend fun register(filesToAdd: List<VfsFile>) {
        mutex.withLock {
            val currentTime = now()
            cleanExpired(currentTime)
            files.addAll(filesToAdd.map { Pair(it, currentTime + expiresAfter) })
        }
    }

    fun start() {
        scope.launch {
            while (isActive) {
                delay(30_000)
                mutex.withLock {
                    cleanExpired(now())
                }
            }
        }
    }

    fun stop() {
        scope.cancel()
    }

    suspend fun addFile(file: VfsFile) {
        register(listOf(file))
    }

    suspend fun addFiles(files: List<VfsFile>) {
        register(files)
    }
}