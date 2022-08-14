package xyz.xszq.otomadbot.image

import com.benjaminwan.ocrlibrary.OcrEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ChineseOCRLite {
    private val ocrEngine = OcrEngine()
    fun init() {
        ocrEngine.setNumThread(16)

        ocrEngine.initLogger(
            isConsole = false,
            isPartImg = false,
            isResultImg = false
        )

        val initModelsRet = ocrEngine.initModels("chineseocr_lite/models", "dbnet.onnx",
            "angle_net.onnx", "crnn_lite_lstm.onnx", "keys.txt")
        if (!initModelsRet) {
            println("Error in models initialization, please check the models/keys path!")
            return
        }

        ocrEngine.padding = 50
        ocrEngine.boxScoreThresh = 0.6f
        ocrEngine.boxThresh = 0.3f
        ocrEngine.unClipRatio = 2.5f
        ocrEngine.doAngle = true
        ocrEngine.mostAngle = true
    }
    suspend fun ocr(imgPath: String): String {
        return withContext(Dispatchers.IO) {
            ocrEngine.detect(imgPath, maxSideLen = 0).textBlocks.joinToString(" ") { it.text }
        }
    }
}