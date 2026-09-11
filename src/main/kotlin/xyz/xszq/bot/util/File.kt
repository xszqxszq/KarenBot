package xyz.xszq.bot.util

import io.github.oshai.kotlinlogging.KLogger
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import korlibs.io.file.VfsFile
import korlibs.io.file.std.tempVfs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Container
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.awt.HeadlessException
import java.awt.RenderingHints
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.*
import java.util.concurrent.CountDownLatch
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

private val downloadClient by lazy { createDownloadClient() }

/**
 * 创建临时文件
 *
 * @param prefix 文件名前缀
 * @param suffix 文件名后缀
 */
fun newTempFile(
    prefix: String = "",
    suffix: String = ""
) = tempVfs[prefix + UUID.randomUUID().toString() + suffix]

/**
 * 使用文件并自动删除
 *
 * @param block 使用文件的代码块
 */
suspend fun <T> VfsFile.use(block: suspend (VfsFile) -> T): T {
    return try {
        block.invoke(this)
    } finally {
        delete()
    }
}

/**
 * 创建临时文件并在使用后删除
 *
 * @param prefix 文件名前缀
 * @param suffix 文件名后缀
 * @param block 使用代码块
 */
suspend fun <R> useTempFile(
    prefix: String = "",
    suffix: String = "",
    block: suspend (VfsFile) -> R
): R = newTempFile(prefix, suffix).use(block)

fun createDownloadClient() = HttpClient(OkHttp)

/**
 * 在弹出的窗口中显示图片
 */
suspend fun VfsFile.showImageAndWait() = withContext(Dispatchers.IO) {
    if (GraphicsEnvironment.isHeadless())
        throw HeadlessException("当前环境没有图形界面")
    val source = ImageIO.read(ByteArrayInputStream(readBytes()))
        ?: return@withContext
    val imageType = if (source.colorModel.hasAlpha())
        BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB
    val image = BufferedImage(source.width, source.height, imageType)
    image.createGraphics().apply {
        drawImage(source, 0, 0, null)
        dispose()
    }
    val closed = CountDownLatch(1)
    SwingUtilities.invokeLater {
        val pane = object : Container() {
            override fun paint(g: Graphics) {
                val graphics = g as? Graphics2D
                graphics ?.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC
                )
                graphics ?.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
                )
                g.drawImage(image, 0, 0, width, height, null)
            }
        }
        val bounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
            .maximumWindowBounds
        val maxWidth = (bounds.width * 0.9).toInt()
        val maxHeight = bounds.height
        fun scaledPane(limitWidth: Int, limitHeight: Int): Dimension {
            val scale = minOf(
                1.0,
                limitWidth.toDouble() / image.width,
                limitHeight.toDouble() / image.height
            )
            return Dimension(
                (image.width * scale).toInt().coerceAtLeast(1),
                (image.height * scale).toInt().coerceAtLeast(1)
            )
        }
        pane.minimumSize = Dimension(128, 128)
        pane.preferredSize = scaledPane(maxWidth, maxHeight)
        JFrame("图片预览").apply {
            defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
            addWindowListener(object : WindowAdapter() {
                override fun windowClosed(e: WindowEvent) {
                    closed.countDown()
                }
            })
            contentPane = pane
            pack()
            val decorWidth = width - pane.width
            val decorHeight = height - pane.height
            if (height > bounds.height || width > bounds.width) {
                pane.preferredSize = scaledPane(
                    maxWidth - decorWidth,
                    maxHeight - decorHeight
                )
                pack()
            }
            setLocationRelativeTo(null)
            isVisible = true
        }
    }
    closed.await()
}


/**
 * 从 URL 下载文件
 *
 * @param url 下载地址
 * @param filename 文件名
 * @param logger 日志器
 */
suspend fun downloadFile(url: String, filename: String, logger: KLogger): VfsFile? =
    downloadFile(url, filename, logger, downloadClient)

/**
 * 从 URL 下载文件
 *
 * @param url 下载地址
 * @param filename 文件名
 * @param logger 日志器
 * @param client 客户端
 */
suspend fun downloadFile(url: String, filename: String, logger: KLogger, client: HttpClient): VfsFile? =
    withContext(Dispatchers.IO) {
        val file = tempVfs[filename]
        runCatching {
            val response = client.get(url)
            file.write(response.bodyAsBytes())
            file
        }.onFailure { e ->
            logger.error { "下载文件失败: ${e.message}" }
        }.getOrNull()
    }