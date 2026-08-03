package xyz.xszq.bot.chunithm.component

import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import xyz.xszq.bot.llm.LLMClient
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger
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
    ) {
        val coverDir = Paths.get(coversDir).toAbsolutePath().normalize()
        val existing = load(outputPath)
        val existingIds = existing.keys.toSet()
        val allFiles = coverDir.toFile().listFiles()
        if (allFiles == null) {
            println("[ChuCoverEmbedding] 无法读取封面目录：$coverDir")
            return
        }
        val files = allFiles.filter { it.name.endsWith(".png") && it.isFile }
            .sortedBy { it.nameWithoutExtension.toIntOrNull() ?: Int.MAX_VALUE }
            .filter { it.nameWithoutExtension.toIntOrNull() !in existingIds }

        if (files.isEmpty()) {
            println("[ChuCoverEmbedding] 所有封面已有向量，无需生成。")
            return
        }

        println("[ChuCoverEmbedding] 已有 ${existing.size} 张，还需生成 ${files.size} 张的向量...")

        val result = existing.toMutableMap()
        var success = 0
        var failed = 0

        files.forEachIndexed { index, file ->
            val resourceId = file.nameWithoutExtension.toIntOrNull()
            if (resourceId == null) {
                println("[ChuCoverEmbedding] 跳过非数字文件名：${file.name}")
                return@forEachIndexed
            }

            try {
                val bytes = compressImage(file)
                val vector = client.embed(
                    scene = "embedding",
                    data = bytes,
                    mediaType = "image/jpeg",
                )
                if (vector.isNotEmpty()) {
                    result[resourceId] = vector
                    success++
                } else {
                    println("[ChuCoverEmbedding] 警告：${file.name} 返回空向量")
                    failed++
                }
            } catch (e: Exception) {
                println("[ChuCoverEmbedding] 失败：${file.name} - ${e.message}")
                failed++
            }

            val total = existing.size + (index + 1)
            if (total % 50 == 0 || index == files.size - 1) {
                println("[ChuCoverEmbedding] 进度：${total}/${existing.size + files.size}（本次成功 $success，失败 $failed）")
            }
        }

        val outputFile = Paths.get(outputPath).toAbsolutePath().normalize().toFile()
        outputFile.parentFile.mkdirs()
        outputFile.writeText(json.encodeToString(result), Charsets.UTF_8)

        println("[ChuCoverEmbedding] 完成！本次成功 $success 张，失败 $failed 张，累计共 ${result.size} 张。")
        println("[ChuCoverEmbedding] 结果已保存至：$outputFile")
    }

    suspend fun generateDescriptions(
        client: LLMClient,
        coversDir: String,
        outputPath: String = "${coversDir}/../cover-descriptions.json",
    ) {
        val coverDir = Paths.get(coversDir).toAbsolutePath().normalize()
        val existing = loadDescriptions(outputPath)
        val existingIds = existing.keys.toSet()
        val allFiles = coverDir.toFile().listFiles()
        if (allFiles == null) {
            println("[ChuCoverDesc] 无法读取封面目录：$coverDir")
            return
        }
        val files = allFiles.filter { it.name.endsWith(".png") && it.isFile }
            .sortedBy { it.nameWithoutExtension.toIntOrNull() ?: Int.MAX_VALUE }
            .filter { file -> file.nameWithoutExtension.toIntOrNull() !in existingIds }

        if (files.isEmpty()) {
            println("[ChuCoverDesc] 所有封面已有描述，无需生成。")
            return
        }

        println("[ChuCoverDesc] 已有 ${existing.size} 张，还需生成 ${files.size} 张的描述并向量化...")

        val result = existing.toMutableMap()
        val success = AtomicInteger(0)
        val failed = AtomicInteger(0)
        val semaphore = Semaphore(32)

        coroutineScope {
            val deferred = files.mapIndexedNotNull { index, file ->
                val resourceId = file.nameWithoutExtension.toIntOrNull()
                if (resourceId == null) {
                    println("[ChuCoverDesc] 跳过非数字文件名：${file.name}")
                    return@mapIndexedNotNull null
                }
                async {
                    semaphore.withPermit {
                        try {
                            val bytes = compressImage(file)
                            val desc = client.chat(scene = "rhythm-game") {
                                system("你是一个中二节奏封面描述专家。请用100-150字详细描述这张封面的视觉特征：人物、动作、服装颜色、表情、背景场景、色调、构图风格、整体氛围。如果画面有文字也描述文字。用中文。只返回描述文本，不要任何前缀。")
                                user {
                                    image(bytes, ContentType.Image.JPEG)
                                }
                            }
                            if (desc.isBlank()) {
                                println("[ChuCoverDesc] 警告：${file.name} 返回空描述")
                                failed.incrementAndGet()
                                return@withPermit
                            }
                            val vec = client.embed(
                                scene = "embedding",
                                input = desc,
                            )
                            synchronized(result) {
                                if (vec.isNotEmpty()) {
                                    result[resourceId] = CoverDescData(desc = desc, vec = vec)
                                    success.incrementAndGet()
                                } else {
                                    println("[ChuCoverDesc] 警告：${file.name} 描述向量为空")
                                    failed.incrementAndGet()
                                }
                            }
                        } catch (e: Exception) {
                            println("[ChuCoverDesc] 失败：${file.name} - ${e.message}")
                            failed.incrementAndGet()
                        }
                        val total = existing.size + (index + 1)
                        if (total % 50 == 0 || index == files.size - 1) {
                            println("[ChuCoverDesc] 进度：${total}/${existing.size + files.size}（本次成功 ${success.get()}，失败 ${failed.get()}）")
                        }
                    }
                }
            }
            deferred.forEach { it.await() }
        }

        val outputFile = Paths.get(outputPath).toAbsolutePath().normalize().toFile()
        outputFile.parentFile.mkdirs()
        outputFile.writeText(json.encodeToString(result), Charsets.UTF_8)

        println("[ChuCoverDesc] 完成！本次成功 $success 张，失败 $failed 张，累计共 ${result.size} 张。")
        println("[ChuCoverDesc] 结果已保存至：$outputFile。")
    }

    fun load(path: String): Map<Int, List<Float>> {
        val file = File(path)
        if (!file.exists()) return emptyMap()
        return runCatching {
            val map = json.decodeFromString<Map<String, List<Float>>>(file.readText(Charsets.UTF_8))
            map.entries.associate { (key, value) -> key.toInt() to value }
        }.getOrDefault(emptyMap())
    }

    fun loadDescriptions(path: String): Map<Int, CoverDescData> {
        val file = File(path)
        if (!file.exists()) return emptyMap()
        return runCatching {
            val map = json.decodeFromString<Map<String, CoverDescData>>(file.readText(Charsets.UTF_8))
            map.entries.associate { (key, value) -> key.toInt() to value }
        }.getOrDefault(emptyMap())
    }
}
