package xyz.xszq.bot

import ai.onnxruntime.*
import korlibs.io.file.VfsFile
import korlibs.io.file.std.localCurrentDirVfs
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.*
import java.nio.FloatBuffer
import kotlin.properties.Delegates

/**
 * 金发检测
 * 模型来自 https://huggingface.co/SmilingWolf/wd-v1-4-moat-tagger-v2
 */
class BlondeHairDetector(
    private val modelPath: String,
    private val tagsPath: String
) : AutoCloseable {
    private val size = 448
    lateinit var env: OrtEnvironment
    lateinit var session: OrtSession
    lateinit var tags: List<List<String>>
    lateinit var names: List<String>
    var girlIndex by Delegates.notNull<Int>()
    var blondeIndex by Delegates.notNull<Int>()

    suspend fun init() {
        env = OrtEnvironment.getEnvironment()
        session = env.createSession(localCurrentDirVfs[modelPath].readAll(),
            OrtSession.SessionOptions().apply { setIntraOpNumThreads(2) })
        tags = localCurrentDirVfs[tagsPath].readAll().decodeToString().lineSequence().drop(1).filter {
            it.isNotBlank()
        }.map {
            it.split(",", limit = 4)
        }.toList()
        names = tags.map { it[1] }
        girlIndex = names.indexOf("1girl")
        blondeIndex = names.indexOf("blonde_hair")
    }

    fun recognize(imagePath: VfsFile): Boolean {
        val img = Image.makeFromEncoded(runBlocking { imagePath.readAll() })
        val maxDim = maxOf(img.width, img.height)
        val scale = size.toFloat() / maxDim.toFloat()
        val sw = (img.width * scale).toInt().coerceAtMost(size)
        val sh = (img.height * scale).toInt().coerceAtMost(size)
        val ox = (size - sw) / 2f
        val oy = (size - sh) / 2f

        val bmp = Bitmap().apply { allocN32Pixels(size, size) }
        Canvas(bmp).apply {
            clear(-1)
            drawImageRect(img, Rect(0f, 0f, img.width.toFloat(), img.height.toFloat()),
                Rect(ox, oy, ox + sw, oy + sh))
            close()
        }

        val pixels = bmp.readPixels(
            ImageInfo.makeN32(size, size, ColorAlphaType.OPAQUE),
            size * 4, 0, 0
        )!!
        val inp = FloatArray(size * size * 3)
        var j = 0
        (0 until pixels.size step 4).forEach { i ->
            inp[j++] = (pixels[i    ].toInt() and 0xFF).toFloat()
            inp[j++] = (pixels[i + 1].toInt() and 0xFF).toFloat()
            inp[j++] = (pixels[i + 2].toInt() and 0xFF).toFloat()
        }

        val tensor = OnnxTensor.createTensor(
            env,
            FloatBuffer.wrap(inp),
            longArrayOf(1, size.toLong(), size.toLong(), 3)
        )
        val output = session.run(mapOf(session.inputNames.iterator().next() to tensor))
        val logits = FloatArray(names.size).apply { (output[0] as OnnxTensor).floatBuffer.get(this) }
        output.close()
        tensor.close()

        logits.indices.forEach { logits[it] = 1f / (1f + kotlin.math.exp(-logits[it])) }
        return logits[girlIndex] >= 0.5f && logits[blondeIndex] >= 0.6f
    }

    override fun close() = session.close()
}