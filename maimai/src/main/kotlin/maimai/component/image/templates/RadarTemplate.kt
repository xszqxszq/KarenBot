package xyz.xszq.bot.maimai.component.image.templates

import korlibs.io.file.VfsFile
import korlibs.io.file.std.localCurrentDirVfs
import kotlinx.serialization.json.Json
import org.jetbrains.skia.*
import xyz.xszq.bot.maimai.component.RadarValue
import xyz.xszq.bot.maimai.music.ChartInfo
import xyz.xszq.bot.maimai.payload.LocalMusicInfo
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class RadarTemplate(val basePath: String) {
    lateinit var dataDir: VfsFile
    lateinit var typeface: Typeface
    lateinit var data: Map<String, List<RadarValue?>>

    fun init() {
        dataDir = localCurrentDirVfs[basePath]

        typeface = findTypeface("阿里巴巴普惠体-H")
        data = Json.decodeFromString(
            File(dataDir.absolutePath + "/radar.json").readText(Charsets.UTF_8)
        )
    }

    private fun findTypeface(targetAlias: String): Typeface {
        val fontMgr = FontMgr.default
        for (i in 0 until fontMgr.familiesCount) {
            val familyName = fontMgr.getFamilyName(i)
            val styleSet = fontMgr.makeStyleSet(i) ?: continue
            for (j in 0 until styleSet.count()) {
                val typeface = styleSet.getTypeface(j) ?: continue
                val styleName = styleSet.getStyleName(j)

                val styleIdentifier = when {
                    styleName.contains("Heavy", ignoreCase = true) -> "H"
                    styleName.contains("Bold", ignoreCase = true) -> "B"
                    styleName.contains("Regular", ignoreCase = true) -> "R"
                    styleName.contains("Medium", ignoreCase = true) -> "M"
                    styleName.contains("Light", ignoreCase = true) -> "L"
                    else -> ""
                }
                if (styleIdentifier.isNotEmpty()) {
                    val alias = "$familyName-$styleIdentifier"
                    if (alias == targetAlias) {
                        return typeface
                    }
                }
            }
        }
        return fontMgr.matchFamilyStyle(null, FontStyle.NORMAL)
            ?: fontMgr.makeStyleSet(0)?.getTypeface(0)
            ?: throw IllegalStateException()
    }

    fun generate(
        charts: List<ChartInfo>,
        size: Int = 500,
        transparent: Boolean = true
    ): Image? {
        val data = charts.mapNotNull { chart -> data[chart.music.id.toString()]?.get(chart.difficulty.value) }
        return generate(RadarValue(
            notes = data.sumOf { it.notes } / data.size,
            peak = data.sumOf { it.peak } / data.size,
            stamina = data.sumOf { it.stamina } / data.size,
            slide = data.sumOf { it.slide } / data.size,
            handTrip = data.sumOf { it.handTrip } / data.size
        ), size, transparent)
    }

    fun generate(
        chart: ChartInfo,
        size: Int = 500,
        transparent: Boolean = true
    ): Image? {
        val data = data[chart.music.id.toString()]?.get(chart.difficulty.value) ?: return null
        return generate(data, size, transparent)
    }

    fun generate(
        data: RadarValue,
        size: Int = 500,
        transparent: Boolean = true
    ): Image {
        val values = floatArrayOf(
            data.notes.toFloat(),
            data.peak.toFloat(),
            data.stamina.toFloat(),
            data.slide.toFloat(),
            data.handTrip.toFloat()
        )

        val dimensionNames = arrayOf("键盘", "爆发", "耐力", "星星", "出张")

        val surface = Surface.makeRasterN32Premul(size, size)
        val canvas = surface.canvas

        val bgAlpha = if (transparent) 0 else 255
        canvas.clear(Color.makeARGB(bgAlpha, 0, 0, 0))

        val centerX = size / 2f
        val centerY = size / 2f
        val scale = size / 500f
        val maxRadius = 150f * scale
        val maxValue = 10.0f

        val angles = FloatArray(5) { i -> (-PI / 2 + 2 * PI * i / 5).toFloat() }

        val bgPath = Path()
        val bgPaint = Paint().apply {
            color = Color.makeARGB(100, 200, 200, 200)
            mode = PaintMode.STROKE
            strokeWidth = 2f * scale
            isAntiAlias = true
        }

        (0 until 5).forEach { index ->
            val x = centerX + maxRadius * cos(angles[index])
            val y = centerY + maxRadius * sin(angles[index])
            if (index == 0)
                bgPath.moveTo(x, y)
            else
                bgPath.lineTo(x, y)
            canvas.drawLine(centerX, centerY, x, y, bgPaint)
        }
        bgPath.closePath()
        canvas.drawPath(bgPath, bgPaint)

        val dataPath = Path()
        (0 until 5).forEach { index ->
            val ratio = (values[index] / maxValue).coerceAtMost(1f)
            val r = ratio * maxRadius
            val x = centerX + r * cos(angles[index])
            val y = centerY + r * sin(angles[index])

            if (index == 0)
                dataPath.moveTo(x, y)
            else
                dataPath.lineTo(x, y)
        }
        dataPath.closePath()

        val fillPaint = Paint().apply {
            color = Color.makeARGB(150, 159, 81, 220)
            mode = PaintMode.FILL
            isAntiAlias = true
        }
        canvas.drawPath(dataPath, fillPaint)

        val strokePaint = Paint().apply {
            color = Color.makeARGB(255, 159, 81, 220)
            mode = PaintMode.STROKE
            strokeWidth = 3f * scale
            isAntiAlias = true
        }
        canvas.drawPath(dataPath, strokePaint)

        val nameFont = Font(typeface, 18f * scale)
        val valueFont = Font(typeface, 16f * scale)

        val textPaint = Paint().apply {
            color = Color.makeARGB(255, 255, 255, 255)
            isAntiAlias = true
        }

        (0 until 5).forEach { index ->
            val dimensionName = dimensionNames[index]
            val valueStr = String.format("%.2f", values[index])

            val textRadius = maxRadius + 35f * scale
            val tx = centerX + textRadius * cos(angles[index])
            val ty = centerY + textRadius * sin(angles[index])

            val nameWidth = nameFont.measureTextWidth(dimensionName)
            val valueWidth = valueFont.measureTextWidth(valueStr)

            canvas.drawString(
                dimensionName,
                tx - (nameWidth / 2f),
                ty - 4f * scale,
                nameFont,
                textPaint
            )
            canvas.drawString(
                valueStr,
                tx - (valueWidth / 2f),
                ty + 16f * scale,
                valueFont,
                textPaint
            )
        }

        return surface.makeImageSnapshot()
    }
}
