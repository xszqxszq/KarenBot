package xyz.xszq

import com.soywiz.korim.format.readNativeImage
import com.soywiz.korio.file.std.rootLocalVfs
import xyz.xszq.karenbot.image.differenceHashTriple

suspend fun main() {
    val hash1 = differenceHashTriple.calc(rootLocalVfs["C:\\Users\\xszq\\Desktop\\images.jpg"].readNativeImage().toBMP32())
    val hash2 = differenceHashTriple.calc(rootLocalVfs["D:\\Temp\\wege.png"].readNativeImage().toBMP32())
    println(differenceHashTriple.similarity(hash1, hash2));
}