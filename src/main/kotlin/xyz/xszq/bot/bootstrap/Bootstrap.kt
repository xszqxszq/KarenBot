package xyz.xszq.bot.bootstrap

import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile

/**
 * 应用启动入口
 *
 * 启动时先解析运行时依赖，再进行类加载
 */
object Bootstrap {
    private const val MAIN_CLASS = "xyz.xszq.bot.KarenBotApplication"

    /**
     * 入口点
     */
    @JvmStatic
    @Suppress("unused")
    fun main(args: Array<String>) {
        RuntimePaths.relaunchIfNeeded(Bootstrap::class.java.name, args)
        val currentJar = File(Bootstrap::class.java.protectionDomain.codeSource.location.toURI())

        val dependencyFiles = JarFile(currentJar).use { jarFile ->
            RuntimeDependencyResolver.resolveDependencies(jarFile, File("libs"))
        }

        val urls = buildList {
            add(currentJar.toURI().toURL())
            dependencyFiles.forEach { add(it.toURI().toURL()) }
        }.toTypedArray()

        val classLoader = URLClassLoader(urls, ClassLoader.getPlatformClassLoader())
        Thread.currentThread().contextClassLoader = classLoader
        val mainClass = Class.forName(MAIN_CLASS, true, classLoader)
        val mainMethod = mainClass.getMethod("main", Array<String>::class.java)
        mainMethod.invoke(null, args)
    }
}