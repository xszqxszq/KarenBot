@file:Suppress("unused", "MemberVisibilityCanBePrivate")
package tk.xszq.otomadbot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@DslMarker annotation class ExecBuilder
@DslMarker annotation class EnvBuilder
class ProgramExecutor(val command: String, val builder: Builder.() -> Unit = {}) {
    @ExecBuilder
    inner class Builder {
        var env = emptyArray<String>()
        var timeout: Long? = null
        fun environment(builder: EnvironmentBuilder.() -> Unit) {
            env = EnvironmentBuilder().apply(builder).env.toTypedArray()
        }
        fun timeout(timeMs: Long) { timeout = timeMs }
    }
    @EnvBuilder
    inner class EnvironmentBuilder {
        val env = mutableListOf<String>()
        fun append(str: String) = if (str.isNotBlank()) env.add(str) else false
        fun append(str: String?) = str?.let { if (it.isNotBlank()) env.add(it) }
    }
    private fun startBlocking(): Unit = Builder().apply(builder).run {
        val procBuilder = ProcessBuilder()
        env.forEach {
            val args = it.split("=")
            val name = args.first()
            val value = args.last()
            procBuilder.environment().putIfAbsent(name, value)
        }
        val realCommand = if (isLinux())
            listOf("/bin/bash", "-c", command)
        else
            listOf("C:\\Windows\\System32\\cmd.exe", "/C", command)
        val proc = procBuilder.inheritIO().command(realCommand).start()
        timeout?.let {
            proc.waitFor(it, TimeUnit.MILLISECONDS)
        } ?: run {
            proc.waitFor()
        }
    }
    suspend fun start() = withContext(Dispatchers.IO) {
        startBlocking()
    }
}