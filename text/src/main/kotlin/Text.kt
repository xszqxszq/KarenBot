package xyz.xszq.bot

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import korlibs.io.util.UUID
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula
import xyz.xszq.bot.config.TextConfig
import xyz.xszq.bot.event.GroupMessageEvent
import xyz.xszq.bot.message.At
import xyz.xszq.bot.message.Image
import xyz.xszq.bot.message.Markdown
import xyz.xszq.bot.message.PlainText
import xyz.xszq.bot.payload.BilibiliResponse
import xyz.xszq.bot.payload.BilibiliVideoInfo
import xyz.xszq.bot.payload.RandomOtomads
import xyz.xszq.bot.payload.markdown.Keyboard
import xyz.xszq.bot.payload.markdown.MarkdownData
import xyz.xszq.bot.payload.markdown.RenderData
import java.awt.Color
import kotlin.random.Random

@Suppress("unused")
class Text: Plugin() {
    lateinit var textConfig: TextConfig
    lateinit var stereotypes: StereotypesPresets
    val randomImage = RandomImage()
    var blondeHairDetector: BlondeHairDetector? = null
    private var apiServer: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null

    internal var client = createHttpClient()

    companion object {
        fun createHttpClient() = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }
    }

    @OptIn(ExperimentalHoplite::class)
    override suspend fun load() {
        stereotypes = ConfigLoaderBuilder.default()
            .addFileSource("./data/random/stereotypes.yml")
            .withExplicitSealedTypes()
            .build()
            .loadConfigOrThrow<StereotypesPresets>()


        textConfig = ConfigLoaderBuilder.default()
            .addFileSource("./config/text.yml")
            .withExplicitSealedTypes()
            .build()
            .loadConfigOrThrow<TextConfig>()

        randomImage.init()

        textConfig.remoteApi ?.let {
            logger.info { "[文本] 使用远程API: ${textConfig.remoteApi}" }
        } ?: run {
            BlondeHairDetector(
                "models/wd-v1-4-moat-tagger-v2/wd-v1-4-moat-tagger-v2.onnx",
                "models/wd-v1-4-moat-tagger-v2/selected_tags.csv"
            ).also { detector ->
                detector.init()
                blondeHairDetector = detector
            }
            startApiServer()
        }

        setRoute()
        logger.info { "[文本] 插件加载完成。" }
    }
    suspend fun setRoute() = route {
        equalsTo(listOf("帮助", "help")) {
            reply(Markdown(MarkdownData(buildString {
                appendLine("**可怜BOT**")
                appendLine()
                appendLine("请点击下方查看帮助：")
            }), Keyboard.create {
                row {
                    link("查看帮助", "https://otmdb.cn/bot/features", id = "1")
                }
            }))
        }
        equalsTo("在") {
            reply("bot在")
        }
        equalsTo(listOf("？", "?")) {
            if (this !is GroupMessageEvent || mentions.any { it.isSelf })
                reply("问我干嘛")
        }
        always {
            textConfig.presets[message.text.trim()]?.let { text ->
                reply(text)
            }
        }
        startsWith("发病") { target ->
            if (target.isBlank()) {
                reply("使用方法：/发病 名字\n例：/发病 小冰")
                return@startsWith
            }
            val result = stereotypes.texts.random(Random(System.currentTimeMillis())).replace("{target_name}", target)
            if (audit(result))
                reply(result)
            else
                reply("检测到疑似违规内容，请检查输入")
        }
        always {
            val isBlank = message.text.isBlank() && message.filter { it !is PlainText && it !is At }.isEmpty()
            val hasAt = this !is GroupMessageEvent || (mentions.isNotEmpty() && mentions.all { it.isSelf })
            if (isBlank && hasAt) {
                reply(Image(randomImage.random()))
                return@always
            }
            if (message.any { it is Image }) {
                val detected = message.filterIsInstance<Image>().any { img ->
                    blondeHairDetector ?.recognize(img.file)
                        ?: remoteDetect(img.url)
                }
                if (detected)
                    reply(Image(randomImage.random()))
            }
        }
        startsWith(listOf("来点金发", "来点金毛", "来点黄毛", "随机金发", "随机黄毛")) {
            reply(Image(randomImage.random()))
        }
        equalsTo("随机uuid") {
            reply(UUID.randomUUID().toString())
        }
        startsWith("随机数字") { raw ->
            val args = raw.split(" ").filter { it.isNotBlank() }
            reply(
                when (args.size) {
                    0 -> Random.nextInt().toString()
                    1 -> Random.nextLong(args.first().toLong()).toString()
                    2 -> Random.nextLong(args.first().toLong(), args.last().toLong()).toString()
                    else -> buildString {
                        appendLine("随机数字将生成 {x|下界<=x<上界} 内的数字。使用方法：")
                        appendLine("\t随机数字")
                        appendLine("\t随机数字 上界")
                        appendLine("\t随机数字 下界 上界")
                    }.trim().newLine()
                }
            )
        }
        startsWith(listOf("随机音mad", "随机音骂", "otamad")) {
            val list = client.get("https://otmdb.cn/otomad/otamad_random.json").body<RandomOtomads>()

            var video: BilibiliVideoInfo? = null
            repeat(10) {
                val url = list.randomSites.random()
                val bvid = url.substringAfter("video/")
                val info = client.get("https://api.bilibili.com/x/web-interface/view?bvid=$bvid") {
                    headers[HttpHeaders.UserAgent] =
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Safari/537.36"
                }.body<BilibiliResponse<BilibiliVideoInfo>>()
                info.data?.let { data ->
                    video = data
                    return@repeat
                }
                delay(500L)
            }
            video ?: run {
                reply("咦，获取失败了(ó﹏ò｡)")
                return@startsWith
            }
            reply(Markdown(MarkdownData(buildString {
                appendLine("**${video.title}**")
                appendLine()
                appendLine("![img #672px #378px](${video.pic + "@672w_378h_1c.webp"})")
                appendLine()
                appendLine(buildString {
                    appendLine("UP主：${video.owner.name}")
                    appendLine("数据来自otaMAD⋅top")
                }.trim().replace("\n", "\r"))
            }), Keyboard.create {
                row {
                    link("查看音MAD", "https://bilibili.com/video/${video.bvid}", style = RenderData.FILLED_BLUE, id = "1")
                }
                row {
                    at("再抽一个", "随机音mad", enter = true, id = "2")
                    link("otaMAD⋅top", "https://otmdb.cn/jump/otamad_top", id = "3")
                }
            }))
        }
        startsWith("latex") { latex ->
            useTempFile { result ->
                TeXFormula(latex).createPNG(
                    TeXConstants.STYLE_DISPLAY, 22.0F, result.absolutePath,
                    Color.WHITE, Color.BLACK
                )
                reply(Image(result))
            }
        }
        startsWith("debug") {
            reply(buildString {
                appendLine("用户ID: ${sender.id}")
                if (this@startsWith is GroupMessageEvent)
                    appendLine("群组ID: ${group.id}")
            }.trim().newLine())
        }
    }
    @Suppress("unused")
    suspend fun <T> timer(comment: String = "", block: suspend () -> T): T {
        val start = System.currentTimeMillis()
        return block().also {
            print("$comment: ")
            println(System.currentTimeMillis() - start)
        }
    }
    suspend fun audit(text: String): Boolean {
        val client = pluginLoader.llmClient ?: return true
        return try {
            val content = client.chat {
                system(textConfig.system)
                user(text)
            }
            content.toBooleanStrictOrNull() ?: true
        } catch (e: ClientRequestException) {
            false
        } catch (e: Exception) {
            true
        }
    }

    private fun startApiServer() {
        val key = textConfig.remoteKey
        apiServer = embeddedServer(Netty, host = "0.0.0.0", port = 18101) {
            routing {
                post("/blonde") {
                    val auth = call.request.header(HttpHeaders.Authorization)
                    if (auth != "Bearer $key") {
                        call.respond(HttpStatusCode.Unauthorized)
                        return@post
                    }
                    val imageUrl = call.receiveParameters()["url"] ?: run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@post
                    }
                    val detector = blondeHairDetector ?: run {
                        call.respond(HttpStatusCode.InternalServerError)
                        return@post
                    }
                    val imageBytes = client.get(imageUrl).bodyAsBytes()
                    useTempFile(suffix = ".jpg") { file ->
                        file.writeBytes(imageBytes)
                        val result = detector.recognize(file)
                        call.respondText(if (result) "true" else "false")
                    }
                }
            }
        }.start(wait = false)
    }

    private suspend fun remoteDetect(imageUrl: String): Boolean {
        val api = textConfig.remoteApi ?: return false
        val key = textConfig.remoteKey
        val response = client.submitForm(
            url = "$api/blonde",
            formParameters = Parameters.build {
                append("url", imageUrl)
            }
        ) {
            header(HttpHeaders.Authorization, "Bearer $key")
        }
        return response.bodyAsText().toBooleanStrictOrNull() ?: false
    }
}