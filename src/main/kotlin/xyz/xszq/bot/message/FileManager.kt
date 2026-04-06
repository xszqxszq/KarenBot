package xyz.xszq.bot.message

import korlibs.io.file.VfsFile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Manages all image files downloaded.
 * The image files will be lazy deleted after `expiresAfter`.
 */
class FileManager(
    private val expiresAfter: Long = 5 * 60 * 1000L,
    private val now: () -> Long = System::currentTimeMillis,
){
    private val mutex = Mutex()
    private val files = mutableListOf<Pair<VfsFile, Long>>()

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

    suspend fun addFile(file: VfsFile) {
        register(listOf(file))
    }

    suspend fun addFiles(files: List<VfsFile>) {
        register(files)
    }
}