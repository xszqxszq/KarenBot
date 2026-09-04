package xyz.xszq.bot.message

import korlibs.io.file.VfsFile
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 管理所有已下载的消息文件
 *
 * 文件在 expiresAfter 之后被延迟删除
 *
 * @param expiresAfter 文件保留时长（毫秒）
 * @param now 当前时间函数
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

    /**
     * 启动清理协程，定期删除已过期的文件
     */
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

    /**
     * 停止清理协程
     */
    fun stop() {
        scope.cancel()
    }

    /**
     * 添加一个文件
     *
     * @param file 文件
     */
    suspend fun addFile(file: VfsFile) {
        register(listOf(file))
    }

    /**
     * 添加列表中的文件
     *
     * @param files 文件列表
     */
    suspend fun addFiles(files: List<VfsFile>) {
        register(files)
    }
}