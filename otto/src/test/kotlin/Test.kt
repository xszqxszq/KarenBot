import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import io.github.kasukusakura.silkcodec.SilkCoder
import korlibs.io.file.VfsFile
import korlibs.io.file.std.localVfs
import xyz.xszq.bot.AudioHandler
import xyz.xszq.bot.OttoConfig
import xyz.xszq.bot.TTSHandler
import xyz.xszq.bot.newTempFile
import java.io.File
import java.lang.ProcessBuilder.Redirect

suspend fun testConcat() {
    val path = "E:\\Workspace\\KarenBot-9.0\\otto\\otto\\tokens"
    val files = listOf(
        localVfs("$path\\da.wav"),
        localVfs("$path\\jia.wav"),
        localVfs("$path\\hao.wav"),
        localVfs("$path\\a.wav"),
    )

    var start = System.currentTimeMillis()
    AudioHandler.mergeWaveFiles(files, localVfs("D:/Temp/output.wav"))
    var end = System.currentTimeMillis()

    println("WaveHandler 合并用时：${end - start}ms")

    val command = mutableListOf("ffmpeg")
    command.addAll(files.map { listOf("-i", it.path) }.flatten())
    command.addAll(listOf(
        "-filter_complex", List(files.size) { index -> "[$index:0]"}.joinToString("") +
                "concat=n=${files.size}:v=0:a=1[out]",
        "-map", "[out]",
        "-y",
        "D:/Temp/output2.wav"
    ))
    println(command.joinToString(" "))
    start = System.currentTimeMillis()

    ProcessBuilder(command)
        .redirectOutput(Redirect.INHERIT)
        .redirectError(Redirect.INHERIT).start().waitFor()
    end = System.currentTimeMillis()

    println("FFMpeg 合并用时：${end - start}ms")
}

suspend fun pcmToSilk(pcmFile: VfsFile, bitRate: Int, sampleRate: Int): VfsFile {
    val silkFile = newTempFile(suffix=".silk")
    SilkCoder.encode(File(pcmFile.absolutePath).inputStream(), File(silkFile.absolutePath).outputStream(), sampleRate, bitRate)
//    pcmFile.delete()
    return silkFile
}

@OptIn(ExperimentalHoplite::class)
suspend fun main() {

    val config = ConfigLoaderBuilder.default()
        .addFileSource("./config/otto.yml")
        .withExplicitSealedTypes()
        .build()
        .loadConfigOrThrow<OttoConfig>()
    val handler = TTSHandler(config)
    var start = System.currentTimeMillis()
//    val pcm = handler.generate("你说的对，但是《原神》是由米哈游自主研发的一款全新开放世界冒险游戏。游戏发生在一个被称作「提瓦特」的幻想世界，在这里，被神选中的人将被授予「神之眼」，导引元素之力。你将扮演一位名为「旅行者」的神秘角色，在自由的旅行中邂逅性格各异、能力独特的同伴们，和他们一起击败强敌，找回失散的亲人——同时，逐步发掘「原神」的真相。")
    val pcm = localVfs("D:/Temp/output.pcm")
    val silk = pcmToSilk(pcm, 24000, 24000)
    var end = System.currentTimeMillis()

    println("生成用时：${end - start}ms")
}