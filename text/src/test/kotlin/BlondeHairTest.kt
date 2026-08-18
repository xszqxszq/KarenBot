package xyz.xszq.bot

import korlibs.io.file.extensionLC
import korlibs.io.file.std.localCurrentDirVfs
import kotlinx.coroutines.runBlocking
import xyz.xszq.bot.text.BlondeHairDetector

fun main() = runBlocking {
    val recognizer = BlondeHairDetector(
        "../KarenBot-runtime/models/wd-v1-4-moat-tagger-v2/wd-v1-4-moat-tagger-v2.onnx",
        "../KarenBot-runtime/models/wd-v1-4-moat-tagger-v2/selected_tags.csv")

    recognizer.init()

    val files = localCurrentDirVfs["../KarenBot-runtime/data/random/kinpatsu"].listSimple()
        .filter { it.extensionLC == "jpg" }
        .shuffled().take(10)

    files.withIndex().forEach { (i, f) ->
        print("[${i + 1}/10] ${f.toString().substringAfterLast('/')} ... ")
        val r = recognizer.recognize(f)
        println(r)
    }
}