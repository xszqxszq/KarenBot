package xyz.xszq.bot.ffmpeg

import korlibs.memory.Platform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class ProgramExecutor(
    private val command: List<String>,
    private val showOutput: Boolean = false,
    private val builder: Builder.() -> Unit = {}
) {
    inner class Builder {
        var env = emptyArray<String>()
        var timeout: Long? = null
        fun environment(builder: EnvironmentBuilder.() -> Unit) {
            env = EnvironmentBuilder().apply(builder).env.toTypedArray()
        }
        fun timeout(timeMs: Long) { timeout = timeMs }
    }
    inner class EnvironmentBuilder {
        val env = mutableListOf<String>()
        fun append(str: String) = if (str.isNotBlank()) env.add(str) else false
        fun append(str: String?) = str?.let { if (it.isNotBlank()) env.add(it) }
    }

    private val nullFile = File(
        if (Platform.isWindows) "NUL" else "/dev/null"
    )
    private fun startBlocking(): Unit = Builder().apply(builder).run {
        val procBuilder = ProcessBuilder()
        env.forEach {
            val args = it.split("=")
            val name = args.first()
            val value = args.last()
            procBuilder.environment().putIfAbsent(name, value)
        }
        val realCommand = if (Platform.os.isWindows)
            command
        else
            listOf("/bin/bash", "-c", command.joinToString(" "))
        val proc =
            if (showOutput) procBuilder.inheritIO().command(realCommand).start()
            else procBuilder.inheritIO().redirectOutput(nullFile).redirectError(nullFile).command(realCommand).start()
        timeout ?.let {
            proc.waitFor(it, TimeUnit.MILLISECONDS)
        } ?: run {
            proc.waitFor()
        }
    }
    suspend fun start() = withContext(Dispatchers.IO) {
        startBlocking()
    }
}
