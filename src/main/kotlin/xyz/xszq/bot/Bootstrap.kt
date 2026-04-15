package xyz.xszq.bot

import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile

object Bootstrap {
    private const val MAIN_CLASS = "xyz.xszq.bot.KarenBotApplication"

    @JvmStatic
    fun main(args: Array<String>) {
        RuntimePaths.relaunchIfNeeded(Bootstrap::class.java.name, args)
        val currentJar = File(Bootstrap::class.java.protectionDomain.codeSource.location.toURI())
        val libsDirectory = File("libs")

        val dependencyFiles = JarFile(currentJar).use { jarFile ->
            RuntimeDependencyResolver.resolveDependencies(jarFile, libsDirectory)
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