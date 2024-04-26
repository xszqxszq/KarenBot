package xyz.xszq.karenbot.image

import com.soywiz.korio.file.std.toVfs
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.decodeFromString
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.message.data.anyIsInstance
import org.jsoup.Jsoup
import xyz.xszq.KarenBot
import xyz.xszq.events
import xyz.xszq.karenbot.CommandModule
import xyz.xszq.karenbot.GroupCommand
import xyz.xszq.karenbot.api.ApiSettings
import xyz.xszq.karenbot.kotlin.pass
import xyz.xszq.karenbot.mirai.nextMessageEvent
import xyz.xszq.karenbot.mirai.quoteReply
import xyz.xszq.karenbot.mirai.reply
import xyz.xszq.karenbot.mirai.startsWithSimple
import java.text.DecimalFormat
import kotlin.math.roundToInt


object SearchHandler: CommandModule("图像搜索", "image.search") {
    override suspend fun subscribe() {
        events.subscribeGroupMessages {
//            startsWithSimple("搜图", true) { _, _ ->
//                sauceNao.checkAndRun(this)
//            }
            startsWithSimple("搜番", true) { _, _ ->
                traceMoe.checkAndRun(this)
            }
        }
    }
    val client = HttpClient {
        install(HttpTimeout) {
            socketTimeoutMillis = 10000
            requestTimeoutMillis = 10000
            connectTimeoutMillis = 10000
        }
    }
//    val sauceNao = GroupCommand("搜图", "saucenao") {
//        val target = if (message.anyIsInstance<Image>()) {
//            this
//        } else {
//            quoteReply("请发送想要搜索的图片（仅限二次元图片）：")
//            nextMessageEvent()
//        }
//        var tempCounter = 0
//        target.message.forEach { pic ->
//            if (pic is Image) {
//                tempCounter += 1
//                subject.sendMessage(
//                    target.message.quote() + (if (tempCounter > 1) "【图$tempCounter】" else "")
//                            + getImageSearchByUrl(pic.queryUrl())
//                )
//            }
//        }
//    }
    val traceMoe = GroupCommand("搜番", "tracemoe") {
        val target = if (message.anyIsInstance<Image>()) {
            this
        } else {
            quoteReply("请发送想要搜索的番剧截图：")
            nextMessageEvent()
        }
        target.message.forEach { pic ->
            if (pic is Image) {
                val file = pic.cacheOrDownload()!!.toVfs()
                reply(AnimeDB.handle(file))
                file.delete()
                return@GroupCommand
            }
        }
    }
}