package xyz.xszq.bot.image

import com.soywiz.korio.file.std.localCurrentDirVfs
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.random.Random

object RandomImage {
    val list = mutableMapOf<String, MutableList<String>>()
    fun load(dir: String, type: String = dir) {
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
    fun getRandom(type: String): File =
        File(list[type]!![Random.nextInt(list[type]!!.size)])
}