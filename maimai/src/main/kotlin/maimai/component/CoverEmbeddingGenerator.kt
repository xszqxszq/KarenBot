package xyz.xszq.bot.maimai.component

import io.github.oshai.kotlinlogging.KotlinLogging
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

/**
 * 封面嵌入向量与描述
 *
 * 已有结果的封面自动跳过
 */
object CoverEmbeddingGenerator {
    private val logger = KotlinLogging.logger {}
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

        val stream = ByteArrayOutputStream()
        ImageIO.write(scaled, "jpeg", stream)
        return stream.toByteArray()
    }

    /**
     * 生成封面嵌入向量
     *
     * @param client LLM 客户端
     * @param coversDir 封面目录
     * @param outputPath 向量保存路径
     */
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
            logger.error { "[CoverEmbedding] 无法读取封面目录：$coverDir" }
            return
        }
        val files = allFiles.filter { it.name.endsWith(".png") && it.isFile }
            .sortedBy { it.nameWithoutExtension.toIntOrNull() ?: Int.MAX_VALUE }
            .filter { it.nameWithoutExtension.toIntOrNull() !in existingIds }

        if (files.isEmpty()) {
            logger.info { "[CoverEmbedding] 所有封面已有向量，无需生成。" }
            return
        }

        logger.info { "[CoverEmbedding] 已有 ${existing.size} 张，还需生成 ${files.size} 张的向量..." }

        val result = existing.toMutableMap()
        var success = 0
        var failed = 0

        files.forEachIndexed { index, file ->
            val resourceId = file.nameWithoutExtension.toIntOrNull()
            if (resourceId == null) {
                logger.warn { "[CoverEmbedding] 跳过非数字文件名：${file.name}" }
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
                    result[resourceId] = vector.toFloatArray()
                    success++
                } else {
                    logger.warn { "[CoverEmbedding] 警告：${file.name} 返回空向量" }
                    failed++
                }
            } catch (e: Exception) {
                logger.warn { "[CoverEmbedding] 失败：${file.name} - ${e.message}" }
                failed++
            }

            val total = existing.size + (index + 1)
            if (total % 50 == 0 || index == files.size - 1) {
                logger.debug { "[CoverEmbedding] 进度：${total}/${existing.size + files.size}（本次成功 $success，失败 $failed）" }
            }
        }

        val outputFile = Paths.get(outputPath).toAbsolutePath().normalize().toFile()
        outputFile.parentFile.mkdirs()
        outputFile.writeText(json.encodeToString(result), Charsets.UTF_8)

        logger.info { "[CoverEmbedding] 完成！本次成功 $success 张，失败 $failed 张，累计共 ${result.size} 张。" }
        logger.info { "[CoverEmbedding] 结果已保存至：$outputFile" }
    }

    /**
     * 生成封面描述与描述向量
     *
     * @param client LLM 客户端
     * @param coversDir 封面目录
     * @param outputPath 描述保存路径
     */
    suspend fun generateDescriptions(
        client: LLMClient,
        coversDir: String,
        outputPath: String = "${coversDir}/../cover-descriptions.json",
    ) {
        val coverDir = Paths.get(coversDir).toAbsolutePath().normalize()
        val allFiles = coverDir.toFile().listFiles()
        if (allFiles == null) {
            logger.error { "[CoverDesc] 无法读取封面目录：$coverDir" }
            return
        }
        val existing = loadDescriptions(outputPath)
        val existingIds = existing.keys.toSet()
        var files = allFiles.filter { it.name.endsWith(".png") && it.isFile }
            .sortedBy { it.nameWithoutExtension.toIntOrNull() ?: Int.MAX_VALUE }
        files = files.filter { file -> file.nameWithoutExtension.toIntOrNull() !in existingIds }
        if (files.isEmpty()) {
            logger.info { "[CoverDesc] 所有封面已有描述，无需生成。" }
            return
        }

        logger.info { "[CoverDesc] 已有 ${existing.size} 张，还需生成 ${files.size} 张的描述并向量化..." }

        val result = existing.toMutableMap()
        val success = AtomicInteger(0)
        val failed = AtomicInteger(0)
        val semaphore = Semaphore(32)

        coroutineScope {
            val deferred = files.mapIndexedNotNull { index, file ->
                val resourceId = file.nameWithoutExtension.toIntOrNull()
                if (resourceId == null) {
                    logger.warn { "[CoverDesc] 跳过非数字文件名：${file.name}" }
                    return@mapIndexedNotNull null
                }
                async {
                    semaphore.withPermit {
                        try {
                            val bytes = compressImage(file)
                            val desc = client.chat(scene = "rhythm-game") {
                                system("你是一个舞萌DX封面描述专家。请用100-150字详细描述这张封面的视觉特征：人物、动作、服装颜色、表情、背景场景、色调、构图风格、整体氛围。如果画面有文字也描述文字。用中文。只返回描述文本，不要任何前缀。")
                                user {
                                    image(bytes, ContentType.Image.JPEG)
                                }
                            }
                            if (desc.isBlank()) {
                                logger.warn { "[CoverDesc] 警告：${file.name} 返回空描述" }
                                failed.incrementAndGet()
                                return@withPermit
                            }
                            val vec = client.embed(
                                scene = "embedding",
                                input = desc,
                            )
                            synchronized(result) {
                                if (vec.isNotEmpty()) {
                                    result[resourceId] = CoverDescData(desc = desc, vec = vec.toFloatArray())
                                    success.incrementAndGet()
                                } else {
                                    logger.warn { "[CoverDesc] 警告：${file.name} 描述向量为空" }
                                    failed.incrementAndGet()
                                }
                            }
                        } catch (e: Exception) {
                            logger.warn { "[CoverDesc] 失败：${file.name} - ${e.message}" }
                            failed.incrementAndGet()
                        }
                        val total = existing.size + (index + 1)
                        if (total % 50 == 0 || index == files.size - 1) {
                            logger.debug { "[CoverDesc] 进度：${total}/${existing.size + files.size}（本次成功 ${success.get()}，失败 ${failed.get()}）" }
                        }
                    }
                }
            }
            deferred.forEach { it.await() }
        }

        val outputFile = Paths.get(outputPath).toAbsolutePath().normalize().toFile()
        outputFile.parentFile.mkdirs()
        outputFile.writeText(json.encodeToString(result), Charsets.UTF_8)

        logger.info { "[CoverDesc] 完成！本次成功 $success 张，失败 $failed 张，累计共 ${result.size} 张。" }
        logger.info { "[CoverDesc] 结果已保存至：$outputFile。" }
    }

    /**
     * 读取封面嵌入向量文件
     *
     * @param path 向量保存路径
     * @return 向量映射表
     */
    fun load(path: String): Map<Int, FloatArray> {
        val file = File(path)
        if (!file.exists()) return emptyMap()
        return runCatching {
            val map = json.decodeFromString<Map<String, FloatArray>>(file.readText(Charsets.UTF_8))
            map.entries.associate { (key, value) -> key.toInt() to value }
        }.getOrDefault(emptyMap())
    }

    /**
     * 读取封面描述文件
     *
     * @param path 描述保存路径
     * @return 向量映射表
     */
    fun loadDescriptions(path: String): Map<Int, CoverDescData> {
        val file = File(path)
        if (!file.exists()) return emptyMap()
        return runCatching {
            val map = json.decodeFromString<Map<String, CoverDescData>>(file.readText(Charsets.UTF_8))
            map.entries.associate { (key, value) -> key.toInt() to value }
        }.getOrDefault(emptyMap())
    }
}