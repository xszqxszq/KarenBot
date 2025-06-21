package xyz.xszq.bot.message

import korlibs.io.file.VfsFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import java.io.File
import kotlin.coroutines.CoroutineContext

/**
 * Manages all image files downloaded.
 * The image files will be lazy deleted after `expiresAfter`.
 */
class ImageManager: CoroutineScope {
    override val coroutineContext: CoroutineContext = Job()

    private val expiresAfter = 5 * 60 * 1000L // 5 min
    private val channel: SendChannel<Any> = fileActor()

    @OptIn(ObsoleteCoroutinesApi::class)
    fun CoroutineScope.fileActor(): SendChannel<Any> = actor {
        val files = mutableListOf<Pair<VfsFile, Long>>()

        suspend fun clean() {
            val currentTime = System.currentTimeMillis()
            files.filter { it.second <= currentTime }.forEach {
                it.first.delete()
            }
            files.removeAll { it.second <= currentTime }
        }

        for (msg in channel) {
            clean()
            val now = System.currentTimeMillis()
            when (msg) {
                is VfsFile -> {
                    files.add(Pair(msg, now + expiresAfter))
                }

                is List<*> -> {
                    files.addAll(msg.filterIsInstance<VfsFile>().map { Pair(it, now + expiresAfter) })
                }
            }
        }
    }

    suspend fun addFile(file: VfsFile) {
        channel.send(file)
    }
    suspend fun addFiles(files: List<VfsFile>) {
        channel.send(files)
    }
}