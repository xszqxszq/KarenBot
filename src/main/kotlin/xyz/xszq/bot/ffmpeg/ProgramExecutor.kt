package xyz.xszq.bot.ffmpeg

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

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
            val finished = proc.waitFor(timeout ?: DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            if (!finished) {
                proc.destroy()
                proc.waitFor(5, TimeUnit.SECONDS)
                proc.destroyForcibly()
            }
        }
    }

    companion object {
        const val DEFAULT_TIMEOUT_MS = 60_000L
    }
}
