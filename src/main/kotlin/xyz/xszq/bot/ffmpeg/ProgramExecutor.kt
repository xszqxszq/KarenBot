package xyz.xszq.bot.ffmpeg

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.coroutines.resume

/**
 * 执行外部命令任务
 */
@Suppress("unused")
class ProgramExecutor(
    private val command: List<String>,
    private val showOutput: Boolean = false,
    private val builder: Builder.() -> Unit = {}
) {
    class Builder {
        var env = emptyArray<String>()
        var timeout: Long? = null
        var outputFile: File? = null
        fun environment(builder: EnvironmentBuilder.() -> Unit) {
            env = EnvironmentBuilder().apply(builder).env.toTypedArray()
        }
        fun timeout(timeMs: Long) { timeout = timeMs }
    }
    class EnvironmentBuilder {
        val env = mutableListOf<String>()
        fun append(str: String) = if (str.isNotBlank()) env.add(str) else false
        fun append(str: String?) = str?.let { if (it.isNotBlank()) env.add(it) }
    }

    /**
     * 运行外部命令
     */
    suspend fun start() = withContext(Dispatchers.IO) {
        Builder().apply(builder).run {
            val procBuilder = ProcessBuilder(command)
            env.forEach {
                procBuilder.environment().putIfAbsent(it.substringBefore("="), it.substringAfter("="))
            }
            if (showOutput) {
                procBuilder.inheritIO()
            } else {
                procBuilder.redirectOutput(
                    outputFile?.let { ProcessBuilder.Redirect.to(it) } ?: ProcessBuilder.Redirect.DISCARD
                )
                procBuilder.redirectError(ProcessBuilder.Redirect.DISCARD)
            }
            val proc = procBuilder.start()
            if (proc.awaitExit(timeout ?: DEFAULT_TIMEOUT_MS))
                return@run
            proc.destroy()
            if (!proc.awaitExit(DESTROY_TIMEOUT_MS))
                proc.destroyForcibly()
        }
    }

    /**
     * 等待进程结束
     *
     * 等待期间不占用线程
     *
     * @param timeout 超时时间（毫秒）
     * @return 是否在超时前结束
     */
    private suspend fun Process.awaitExit(
        timeout: Long
    ): Boolean = withTimeoutOrNull(timeout) {
        suspendCancellableCoroutine { continuation ->
            onExit().whenComplete { _, _ ->
                if (continuation.isActive)
                    continuation.resume(Unit)
            }
        }
    } != null

    companion object {
        const val DEFAULT_TIMEOUT_MS = 60_000L
        private const val DESTROY_TIMEOUT_MS = 5_000L
    }
}