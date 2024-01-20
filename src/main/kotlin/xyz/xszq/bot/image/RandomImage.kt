package xyz.xszq.bot.image

import korlibs.io.file.VfsFile
import korlibs.io.file.std.localCurrentDirVfs
import korlibs.io.file.std.rootLocalVfs
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.random.Random

object RandomImage {
    val list = mutableMapOf<String, MutableList<String>>()
    fun load(dir: String, type: String = dir) {
        if (!list.containsKey(type))
            list[type] = mutableListOf()
        val pwd = localCurrentDirVfs.absolutePath
        if (!File("$pwd/image").exists()) {
            File("$pwd/image").mkdir()
        }
        if (!File("$pwd/image/$dir").exists()) {
            File("$pwd/image/$dir").mkdir()
        }
        Files.walk(Path("$pwd/image/$dir"), 2)
            .filter { i -> Files.isRegularFile(i) }
            .forEach { path -> list[type]!!.add(path.absolutePathString()) }
    }
    fun getRandom(type: String): VfsFile =
        rootLocalVfs[list[type]!![Random.nextInt(list[type]!!.size)]]
}