package xyz.xszq.bot

import kotlinx.serialization.json.*
import org.jetbrains.skia.*
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// 简单的字体查找函数，直接在测试脚本里转换你的逻辑
fun findTypeface(targetAlias: String): Typeface {
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
    println("警告: 未找到字体 [$targetAlias]，将使用系统默认字体。")
    return fontMgr.matchFamilyStyle(null, FontStyle.NORMAL)
        ?: fontMgr.makeStyleSet(0)?.getTypeface(0)
        ?: throw IllegalStateException("系统中没有找到任何可用字体！")
}

fun main() {
    // 1. 读取 JSON 文件
    val jsonFile = File("E:/Temp/radar.json")
    if (!jsonFile.exists()) {
        println("错误: 未找到 radar.json")
        return
    }

    val jsonString = jsonFile.readText()

    // 2. 使用 kotlinx.serialization 解析 JSON
    val root = Json.parseToJsonElement(jsonString).jsonObject
    val key = "295"
    val array = root[key]?.jsonArray ?: return

    // 找到数组中第一个有效（非 Null）的数据对象
    val dataObj = array.last { it != JsonNull }.jsonObject

    // 提取五个维度的值
    val values = floatArrayOf(
        dataObj["notes"]!!.jsonPrimitive.float,
        dataObj["peak"]!!.jsonPrimitive.float,
        dataObj["stamina"]!!.jsonPrimitive.float,
        dataObj["star"]!!.jsonPrimitive.float,
        dataObj["handTrip"]!!.jsonPrimitive.float
    )

    val dimensionNames = arrayOf("键盘", "爆发", "耐力", "星星", "出张")

    // 3. 初始化 Skia 画布 (扩大至 500x500 防止文字被裁剪)
    val width = 500
    val height = 500
    val surface = Surface.makeRasterN32Premul(width, height)
    val canvas = surface.canvas

    canvas.clear(Color.makeARGB(0, 0, 0, 0))

    val centerX = width / 2f
    val centerY = height / 2f
    val maxRadius = 150f
    val maxValue = 10.0f

    val angles = FloatArray(5) { i -> (-PI / 2 + 2 * PI * i / 5).toFloat() }

    // 4. 绘制底板网格与轴线
    val bgPath = Path()
    val bgPaint = Paint().apply {
        color = Color.makeARGB(100, 200, 200, 200)
        mode = PaintMode.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }

    for (i in 0 until 5) {
        val x = centerX + maxRadius * cos(angles[i])
        val y = centerY + maxRadius * sin(angles[i])
        if (i == 0) bgPath.moveTo(x, y) else bgPath.lineTo(x, y)
        canvas.drawLine(centerX, centerY, x, y, bgPaint)
    }
    bgPath.closePath()
    canvas.drawPath(bgPath, bgPaint)

    // 5. 绘制多边形数据层
    val dataPath = Path()
    for (i in 0 until 5) {
        val ratio = (values[i] / maxValue).coerceAtMost(1f)
        val r = ratio * maxRadius
        val x = centerX + r * cos(angles[i])
        val y = centerY + r * sin(angles[i])

        if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
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
        strokeWidth = 3f
        isAntiAlias = true
    }
    canvas.drawPath(dataPath, strokePaint)

    // 6. 绘制文字 (调用提取出的找字函数)
    val typeface = findTypeface("阿里巴巴普惠体-H")
    val nameFont = Font(typeface, 18f)
    val valueFont = Font(typeface, 16f)

    val textPaint = Paint().apply {
        color = Color.makeARGB(255, 255, 255, 255)
        isAntiAlias = true
    }

    for (i in 0 until 5) {
        val dimensionName = dimensionNames[i]
        val valueStr = String.format("%.2f", values[i])

        // 锚点往外扩展 35 像素放字
        val textRadius = maxRadius + 35f
        val tx = centerX + textRadius * cos(angles[i])
        val ty = centerY + textRadius * sin(angles[i])

        val nameWidth = nameFont.measureTextWidth(dimensionName)
        val valueWidth = valueFont.measureTextWidth(valueStr)

        val nameX = tx - (nameWidth / 2f)
        val nameY = ty - 4f

        val valueX = tx - (valueWidth / 2f)
        val valueY = ty + 16f

        canvas.drawString(dimensionName, nameX, nameY, nameFont, textPaint)
        canvas.drawString(valueStr, valueX, valueY, valueFont, textPaint)
    }

    // 7. 导出图像至 E:/Temp/
    val image = surface.makeImageSnapshot()
    val data = image.encodeToData(EncodedImageFormat.PNG)
        ?: throw Exception("图像编码失败")

    val outputDir = File("E:/Temp")
    if (!outputDir.exists()) {
        outputDir.mkdirs()
    }

    val outputFile = File(outputDir, "radar_output.png")
    outputFile.writeBytes(data.bytes)

    println("写入完成！路径: ${outputFile.absolutePath}")
}