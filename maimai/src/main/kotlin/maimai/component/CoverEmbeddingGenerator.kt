package xyz.xszq.bot.maimai.component

import kotlinx.serialization.json.Json
import xyz.xszq.bot.llm.LLMClient
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Paths
import javax.imageio.ImageIO

object CoverEmbeddingGenerator {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    private const val MAX_DIMENSION = 256

    private fun compressImage(file: File): ByteArray {
        val original = ImageIO.read(file) ?: throw Exception("无法读取图片：${file.name}")
        val (width, height) = if (original.width > MAX_DIMENSION || original.height > MAX_DIMENSION) {
            val scale = MAX_DIMENSION.toDouble() / maxOf(original.width, original.height)
            (original.width * scale).toInt() to (original.height * scale).toInt()
        } else original.width to original.height

        val scaled = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = scaled.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.drawImage(original, 0, 0, width, height, null)
        g.dispose()

        val baos = ByteArrayOutputStream()
        ImageIO.write(scaled, "jpeg", baos)
        return baos.toByteArray()
    }

    suspend fun generate(
        client: LLMClient,
        coversDir: String,
        outputPath: String = "${coversDir}/../cover-embeddings.json",
        endpoint: String,
    ) {
        val coverDir = Paths.get(coversDir).toAbsolutePath().normalize()
        val allFiles = coverDir.toFile().listFiles()
        if (allFiles == null) {
            println("[CoverEmbedding] 无法读取封面目录：$coverDir")
            return
        }
        val files = allFiles.filter { it.name.endsWith(".png") && it.isFile }
            .sortedBy { it.nameWithoutExtension.toIntOrNull() ?: Int.MAX_VALUE }

        if (files.isEmpty()) {
            println("[CoverEmbedding] 未找到封面图片：$coverDir")
            return
        }

        println("[CoverEmbedding] 共发现 ${files.size} 张封面，开始生成向量...")

        val result = mutableMapOf<Int, List<Float>>()
        var success = 0
        var failed = 0

        files.forEachIndexed { index, file ->
            val resourceId = file.nameWithoutExtension.toIntOrNull()
            if (resourceId == null) {
                println("[CoverEmbedding] 跳过非数字文件名：${file.name}")
                return@forEachIndexed
            }

            try {
                val bytes = compressImage(file)
                val vector = client.embed(
                    data = bytes,
                    mediaType = "image/jpeg",
                    model = endpoint,
                )
                if (vector.isNotEmpty()) {
                    result[resourceId] = vector
                    success++
                } else {
                    println("[CoverEmbedding] 警告：${file.name} 返回空向量")
                    failed++
                }
            } catch (e: Exception) {
                println("[CoverEmbedding] 失败：${file.name} - ${e.message}")
                failed++
            }

            if ((index + 1) % 50 == 0 || index == files.size - 1) {
                println("[CoverEmbedding] 进度：${index + 1}/${files.size}（成功 $success，失败 $failed）")
            }
        }

        val outputFile = Paths.get(outputPath).toAbsolutePath().normalize().toFile()
        outputFile.parentFile.mkdirs()
        outputFile.writeText(json.encodeToString(result), Charsets.UTF_8)

        println("[CoverEmbedding] 完成！成功 $success 张，失败 $failed 张")
        println("[CoverEmbedding] 结果已保存至：$outputFile")
    }

    fun load(path: String): Map<Int, List<Float>> {
        val file = File(path)
        if (!file.exists()) return emptyMap()
        return runCatching {
            val map = json.decodeFromString<Map<String, List<Float>>>(file.readText(Charsets.UTF_8))
            map.entries.associate { (key, value) -> key.toInt() to value }
        }.getOrDefault(emptyMap())
    }
}
