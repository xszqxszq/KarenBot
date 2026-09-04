package xyz.xszq.bot.bootstrap

import java.io.File
import java.lang.management.ManagementFactory
import kotlin.system.exitProcess

/**
 * 运行时的路径管理
 */
@Suppress("unused")
object RuntimePaths {
    private const val RUNTIME_HOME = "karenbot.path"
    private const val RELAUNCHED = "karenbot.relaunched"

    val home: File = detectHome()

    /**
     * 解析运行目录下的路径
     *
     * @param path 相对路径
     * @return 运行目录下的对应文件
     */
    fun resolve(path: String): File = home.resolve(path)

    /**
     * 按需以目标运行目录重启进程
     *
     * @param mainClass 要运行的主类
     * @param args 传入参数
     */
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