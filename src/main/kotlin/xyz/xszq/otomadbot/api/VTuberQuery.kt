package xyz.xszq.otomadbot.api

import com.soywiz.korim.bitmap.NativeImageOrBitmap32
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.bitmap.resized
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.font.FolderBasedNativeSystemFontProvider
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.encode
import com.soywiz.korim.format.readNativeImage
import com.soywiz.korim.text.TextAlignment
import com.soywiz.korio.file.std.toVfs
import com.soywiz.korio.util.toStringDecimal
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.ScaleMode
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import xyz.xszq.otomadbot.EventHandler
import xyz.xszq.otomadbot.NetworkUtils
import xyz.xszq.otomadbot.image.circle
import xyz.xszq.otomadbot.image.fillBg
import xyz.xszq.otomadbot.image.text
import xyz.xszq.otomadbot.image.textWidth
import xyz.xszq.otomadbot.quoteReply
import xyz.xszq.otomadbot.startsWithSimple


object VTuberQuery: EventHandler("查成分", "vtuber") {
    private var vtbs = mapOf<Long, VTBsMoeUserShort>()
    private val fonts = FolderBasedNativeSystemFontProvider()
    private val nameFont = fonts.loadFontByName("Source Han Sans CN Bold Bold")!!
    private val infoFont = fonts.loadFontByName("Source Han Sans CN Bold Bold")!!
    private val thinFont = fonts.loadFontByName("Source Han Sans CN Bold Bold")!!
    private val client = HttpClient {
        install(JsonFeature) {
            serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        expectSuccess = false
    }
    override fun register() {
        println(fonts.listFontNames())
        GlobalEventChannel.subscribeMessages {
            startsWithSimple("查成分") { _, raw ->
                when {
                    raw.isBlank() -> null
                    raw.all { it.isDigit() } -> raw.toLong()
                    else -> BilibiliApi.getMidByName(raw)
                } ?.let { mid ->
                    handle(mid, this)
                } ?: run {
                    quoteReply("使用方法：查成分 uid/昵称\n例：查成分 陈睿")
                }
            }
        }
        runBlocking {
            vtbs = getVTBs().associateBy { it.mid }
        }
        super.register()
    }
    private suspend fun getVTBs() = client.get<List<VTBsMoeUserShort>>("https://api.tokyo.vtbs.moe/v1/short")
    private suspend fun handle(mid: Long, event: MessageEvent) = event.run {
        val user = kotlin.runCatching {
            BilibiliApi.getCardByMid(mid)
        }.onFailure {
            quoteReply("请求被B站拦截了，请稍后再试")
            return@run
        }.getOrThrow()

        val rawAvatar = NetworkUtils.downloadTempFile(user.face)!!.toVfs()
        val avatar = rawAvatar.readNativeImage()
        val subscribe = user.attentions.filter { vtbs.containsKey(it) }
        NativeImageOrBitmap32(480, 170 + subscribe.size * 40).context2d {
            fillBg(Colors.WHITE)
            text(user.name, 310, 50, 28.0, align = TextAlignment.TOP_CENTER, font = nameFont)
            text(user.mid, 310, 73, 15.0, Colors.DIMGRAY, align = TextAlignment.TOP_CENTER,
                font = infoFont)
            text("粉丝：${user.fans}   关注：${user.attention}", 310, 100, 20.0,
                align = TextAlignment.TOP_CENTER, font = infoFont)
            text((100.0 * subscribe.size / user.attention).toStringDecimal(2) +
                    "% (${subscribe.size}/${user.attention})", 310, 130, 20.0,
                align = TextAlignment.TOP_CENTER, font = infoFont)
            drawImage(avatar.resized(130, 130, ScaleMode.EXACT, Anchor.CENTER).circle(), 20, 13)
            strokeStyle = RGBA(230, 230, 230)
            lineWidth = 1.0
            strokeRect(10, 155, 460, subscribe.size * 40)
            subscribe.forEachIndexed { i, mid ->
                if (i % 2 == 1) {
                    fillStyle = RGBA(240, 240, 240)
                    fillRect(10, 155 + i * 40, 460, 40)
                }
                val now = vtbs[mid]!!
                text(now.uname, 20, 190 + i * 40, 20.0, align = TextAlignment.MIDDLE_LEFT,
                    font = thinFont)
                val offsetX = textWidth(now.uname, 20.0, thinFont)
                text(now.mid.toString(), 30 + offsetX, 185 + i * 40, 15.0, Colors.DIMGRAY,
                    align = TextAlignment.MIDDLE_LEFT, font = thinFont)
            }
        }.encode(PNG).toExternalResource().use {
            quoteReply(it.uploadAsImage(subject))
        }
        rawAvatar.delete()
    }
}


@Serializable
class VTBsMoeUserShort(val mid: Long, val uname: String, val roomid: Long)