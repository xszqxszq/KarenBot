@file:Suppress("unused")

package com.benjaminwan.ocrlibrary

class OcrEngine {
    init {
        try {
            System.loadLibrary("OcrLiteOnnx")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var padding: Int = 50
    var boxScoreThresh: Float = 0.6f
    var boxThresh: Float = 0.3f
    var unClipRatio: Float = 2.0f
    var doAngle: Boolean = true
    var mostAngle: Boolean = true

    fun detect(input: String, maxSideLen: Int) = detect(
        input, padding, maxSideLen,
        boxScoreThresh, boxThresh,
        unClipRatio, doAngle, mostAngle
    )

    external fun setNumThread(numThread: Int): Boolean

    external fun initLogger(
        isConsole: Boolean,
        isPartImg: Boolean,
        isResultImg: Boolean
    )

    external fun enableResultText(imagePath: String)

    external fun initModels(
        modelsDir: String,
        detName: String,
        clsName: String,
        recName: String,
        keysName: String
    ): Boolean

    external fun getVersion(): String

    external fun detect(
        input: String, padding: Int, maxSideLen: Int,
        boxScoreThresh: Float, boxThresh: Float,
        unClipRatio: Float, doAngle: Boolean, mostAngle: Boolean
    ): OcrResult

}

sealed class OcrOutput

object OcrStop : OcrOutput()
object OcrFailed : OcrOutput()

data class OcrResult(
    val dbNetTime: Double,
    val textBlocks: ArrayList<TextBlock>,
    var detectTime: Double,
    var strRes: String
) : OcrOutput()

data class Point(var x: Int, var y: Int)

data class TextBlock(
    val boxPoint: ArrayList<Point>, var boxScore: Float,
    val angleIndex: Int, val angleScore: Float, val angleTime: Double,
    val text: String, val charScores: FloatArray, val crnnTime: Double,
    val blockTime: Double
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TextBlock

        if (boxPoint != other.boxPoint) return false
        if (boxScore != other.boxScore) return false
        if (angleIndex != other.angleIndex) return false
        if (angleScore != other.angleScore) return false
        if (angleTime != other.angleTime) return false
        if (text != other.text) return false
        if (!charScores.contentEquals(other.charScores)) return false
        if (crnnTime != other.crnnTime) return false
        if (blockTime != other.blockTime) return false

        return true
    }

    override fun hashCode(): Int {
        var result = boxPoint.hashCode()
        result = 31 * result + boxScore.hashCode()
        result = 31 * result + angleIndex
        result = 31 * result + angleScore.hashCode()
        result = 31 * result + angleTime.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + charScores.contentHashCode()
        result = 31 * result + crnnTime.hashCode()
        result = 31 * result + blockTime.hashCode()
        return result
    }
}