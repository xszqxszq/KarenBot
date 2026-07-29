package xyz.xszq.bot

import org.jetbrains.skia.EncodedImageFormat
import xyz.xszq.shinobu.dom.Element
import xyz.xszq.shinobu.template.Template
import xyz.xszq.shinobu.template.TemplateManager
import java.io.File

fun Template.get(): Element {
    val main = this["main"]!!

    val musics = listOf("患部で止まってすぐ溶ける～狂気の優曇華院", "患部で止まってすぐ溶ける～狂気の優曇華院", "患部で止まってすぐ溶ける～狂気の優曇華院", "患部で止まってすぐ溶ける～狂気の優曇華院", "患部で止まってすぐ溶ける～狂気の優曇華院")

    repeat(7) {
        musics.forEach { songName ->
            val now = this["music"]!!.modify {
                text("info/title") {
                    text = songName
                }
            }
            main["best-35"]?.add(now)
        }
    }
    repeat(3) {
        musics.forEach { songName ->
            val now = this["music"]!!.modify {
                text("info/title") {
                    text = songName
                }
            }
            main["best-15"]?.add(now)
        }
    }

    main.modify {
        div("upper/header") {
            image("avatar") {
                src = "avatars/301.png"
            }
            div("info/rating") {
                background = "rating_base_10.png"
            }
        }
    }
    return main
}

suspend fun main() {
    val manager = TemplateManager("./data/maimai")
    manager.init()

//    val template = manager["rating"] ?: return

//    repeat(1000) {
//        val startTime = System.currentTimeMillis()
//        val main = template.get()
//        val data = template.render(main)
//        val endTime = System.currentTimeMillis()
//        println("${endTime - startTime} ms")
//        File("output.jpg").writeBytes(data)
//    }
//    var template = manager["score"]!!
//    val main = template.get()
//    val main = template["main"]!!
//    File("E:/Temp/output.jpg").writeBytes(template.render(main).encodeToData(EncodedImageFormat.JPEG, 85)!!.bytes)
    var template = manager["course"]!!
//    val main = template.get()
    val main = template["main"]!!
    File("E:/Temp/output.jpg").writeBytes(template.render(main).encodeToData(EncodedImageFormat.JPEG, 85)!!.bytes)
}