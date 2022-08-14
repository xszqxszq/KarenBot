package xyz.xszq.otomadbot.image

import ai.djl.Device
import ai.djl.modality.Classifications
import ai.djl.modality.cv.Image
import ai.djl.modality.cv.ImageFactory
import ai.djl.modality.cv.output.BoundingBox
import ai.djl.modality.cv.output.DetectedObjects
import ai.djl.modality.cv.output.DetectedObjects.DetectedObject
import ai.djl.modality.cv.output.Rectangle
import ai.djl.modality.cv.transform.Resize
import ai.djl.modality.cv.transform.ToTensor
import ai.djl.modality.cv.translator.YoloV5Translator
import ai.djl.repository.zoo.Criteria
import ai.djl.repository.zoo.ZooModel
import ai.djl.training.util.ProgressBar
import ai.djl.translate.Pipeline
import ai.djl.translate.Translator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import xyz.xszq.OtomadBotCore
import xyz.xszq.otomadbot.SafeYamlConfig
import java.io.File

class YOLOv5(val name: String) {
    lateinit var translator: Translator<Image, DetectedObjects>
    lateinit var model: ZooModel<Image, DetectedObjects>
    val imageSize = 640
    fun init() {
        val config = YOLOv5Config.data.model[name]!!
        val pipeline = Pipeline()
        pipeline.add(Resize(imageSize))
        pipeline.add(ToTensor())
        translator = YoloV5Translator
            .builder()
            .setPipeline(pipeline)
            .optSynset(config.classes)
            .build()
        model = Criteria.builder()
            .setTypes(Image::class.java, DetectedObjects::class.java)
            .optDevice(Device.cpu())
            .optModelUrls(YOLOv5Config.data.modelPath)
            .optModelName(config.filename)
            .optTranslator(translator)
            .optProgress(ProgressBar())
            .optEngine("PyTorch")
            .build().loadModel()
    }
    suspend fun detect(file: File) = withContext(Dispatchers.IO) {
        model.newPredictor().use { predictor ->
            val input = ImageFactory.getInstance().fromFile(file.toPath())
            val objects = predictor.predict(input)
            val boxes = mutableListOf<BoundingBox>()
            val names = mutableListOf<String>()
            val prob = mutableListOf<Double>()
            objects.items<Classifications.Classification>().forEach {
                val bounds = (it as DetectedObject).boundingBox.bounds
                boxes.add(Rectangle(bounds.x / imageSize * input.width,
                    bounds.y / imageSize * input.height,
                    bounds.width / imageSize * input.width,
                    bounds.height / imageSize * input.height))
                names.add(it.className)
                prob.add(it.probability)
            }
            DetectedObjects(names, prob, boxes)
        }
    }
}

@Serializable
class YOLOv5ConfigEntry(val classes: List<String>, val filename: String)

@Serializable
class YOLOv5ConfigData(val modelPath: String = "yolo/", val model: MutableMap<String, YOLOv5ConfigEntry>)

object YOLOv5Config: SafeYamlConfig<YOLOv5ConfigData>(OtomadBotCore, "yolov5",
    YOLOv5ConfigData("yolo/", buildMap {
        put("common", YOLOv5ConfigEntry(listOf("longyutao_face", "maimai_dxcn_score", "maimai_dxcn_webscore"),
            "common.torchscript"))
        put("anime_face", YOLOv5ConfigEntry(listOf("face"), "yolov5s_anime.torchscript"))
    }.toMutableMap())
)
