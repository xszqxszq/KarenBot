package xyz.xszq

import com.soywiz.korim.format.readNativeImage
import com.soywiz.korio.file.std.localVfs
import com.soywiz.korio.file.std.rootLocalVfs
import xyz.xszq.otomadbot.image.differenceHashTriple
import java.io.File

suspend fun main() {
    val hash1 = differenceHashTriple.calc(rootLocalVfs["C:\\Users\\xszq\\Desktop\\images.jpg"].readNativeImage().toBMP32())
    val hash2 = differenceHashTriple.calc(rootLocalVfs["D:\\Temp\\wege.png"].readNativeImage().toBMP32())
    println(differenceHashTriple.similarity(hash1, hash2));
}