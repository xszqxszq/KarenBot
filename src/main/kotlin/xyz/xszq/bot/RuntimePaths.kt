package xyz.xszq.bot

import java.io.File
import java.lang.management.ManagementFactory
import kotlin.system.exitProcess

@Suppress("unused")
object RuntimePaths {
    private const val RUNTIME_HOME = "karenbot.path"
    private const val RELAUNCHED = "karenbot.relaunched"

    val home: File = detectHome()
    fun resolve(path: String): File = home.resolve(path)

    fun relaunchIfNeeded(mainClass: String, args: Array<String>) {
        val targetHome = System.getProperty(RUNTIME_HOME)
            ?.takeIf { it.isNotBlank() }
            ?.let { File(it).canonicalFile }
            ?: return

        val currentDirectory = File(System.getProperty("user.dir")).canonicalFile
        if (currentDirectory == targetHome || System.getProperty(RELAUNCHED) == "true") {
            return
        }

        targetHome.mkdirs()

        val javaExecutable = File(
            File(System.getProperty("java.home"), "bin"),
            if (System.getProperty("os.name").startsWith("Windows")) "java.exe" else "java"
        ).absolutePath

        val classpath = System.getProperty("java.class.path")
            .split(File.pathSeparator)
            .joinToString(File.pathSeparator) { entry ->
                val file = File(entry)
                if (file.isAbsolute) file.absolutePath else File(currentDirectory, entry).absolutePath
            }

        val command = buildList {
            add(javaExecutable)
            addAll(ManagementFactory.getRuntimeMXBean().inputArguments)
            add("-D$RUNTIME_HOME=${targetHome.absolutePath}")
            add("-D$RELAUNCHED=true")
            add("-cp")
            add(classpath)
            add(mainClass)
            addAll(args)
        }

        val exitCode = ProcessBuilder(command)
            .directory(targetHome)
            .inheritIO()
            .start()
            .waitFor()

        exitProcess(exitCode)
    }

    private fun detectHome(): File {
        System.getProperty(RUNTIME_HOME)
            ?.takeIf { it.isNotBlank() }
            ?.let { return File(it).canonicalFile }

        return File(System.getProperty("user.dir")).canonicalFile
    }
}