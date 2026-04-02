package xyz.xszq.bot

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import korlibs.io.util.UUID
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula
import xyz.xszq.bot.config.LLMConfig
import xyz.xszq.bot.event.GroupMessageEvent
import xyz.xszq.bot.message.File
import xyz.xszq.bot.message.Image
import xyz.xszq.bot.message.PlainText
import xyz.xszq.bot.payload.*
import xyz.xszq.bot.payload.markdown.*
import java.awt.Color
import kotlin.random.Random

@Suppress("unused")
class Text: Plugin() {
    val templateBrief = "102112100_1761189409"
    val templateImage = "102112100_1761189134"
    val presets = buildMap {
        put("在", "bot在")
        put("有人吗", "有bot在哦")
        put("在？", "BIG BOT IS WATCHING YOU")
        put("在吗", "bot一直都在哦")
        put("有美少女吗", "(づ￣3￣)づ")
        put("？", "问我干嘛")
    }
    lateinit var stereotypes: StereotypesPresets
    lateinit var llmConfig: LLMConfig
    val randomImage = RandomImage()

    val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    @OptIn(ExperimentalHoplite::class)
    override suspend fun load() {
        stereotypes = ConfigLoaderBuilder.default()
            .addFileSource("./data/random/stereotypes.yml")
            .withExplicitSealedTypes()
            .build()
            .loadConfigOrThrow<StereotypesPresets>()


        llmConfig = ConfigLoaderBuilder.default()
            .addFileSource("./config/llm.yml")
            .withExplicitSealedTypes()
            .build()
            .loadConfigOrThrow<LLMConfig>()

        randomImage.init()

        setRoute()
        logger.info { "[文本] 插件加载完成。" }
    }
    suspend fun setRoute() = route {
        equalsTo(listOf("帮助", "help")) {
            reply(MarkdownData.create(templateBrief) {
                "title" {
                    "可怜BOT"
                }
                "content" {
                    "请点击下方查看帮助："
                }
            }.toMessage(Keyboard.create {
                row {
                    button(
                        id = "1",
                        action = Action(
                            type = Action.LINK,
                            data = "https://otmdb.cn/bot/features",
                            permission = Permission(Permission.EVERYONE),
                        ),
                        renderData = RenderData(
                            label = "查看帮助",
                            visitedLabel = "查看帮助",
                            style = RenderData.BLUE
                        )
                    )
                }
            }))
        }
        always {
            presets[message.text.trim()]?.let { text ->
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
            if (message.text.isBlank() && message.filter { it !is PlainText }.isEmpty()) {
                reply(Image(randomImage.random()))
            }
            message.firstOrNull { this is File }?.let {
                reply(it)
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
            reply(MarkdownData.create(templateImage) {
                "title" {
                    video.title
                }
                "img" {
                    video.pic + "@672w_378h_1c.webp"
                }
                "img_size" {
                    "img #672px #378px"
                }
                "description" {
                    buildString {
                        appendLine("UP主：${video.owner.name}")
                        appendLine("数据来自otaMAD⋅top")
                    }.trim().replace("\n", "\r")
                }
            }.toMessage(Keyboard.create {
                row {
                    button(
                        id = "1",
                        action = Action(
                            type = Action.LINK,
                            data = "https://bilibili.com/video/${video.bvid}",
                            permission = Permission(Permission.EVERYONE),
                        ),
                        renderData = RenderData(
                            label = "查看音MAD",
                            visitedLabel = "已观看",
                            style = RenderData.FILLED_BLUE
                        )
                    )
                }
                row {

                    button(
                        id = "2",
                        action = Action(
                            type = Action.AT,
                            data = "随机音mad",
                            permission = Permission(Permission.EVERYONE),
                            enter = true
                        ),
                        renderData = RenderData(
                            label = "再抽一个",
                            visitedLabel = "再抽一个",
                            style = RenderData.BLUE
                        )
                    )
                    button(
                        id = "3",
                        action = Action(
                            type = Action.LINK,
                            data = "https://otmdb.cn/jump/otamad_top",
                            permission = Permission(Permission.EVERYONE),
                        ),
                        renderData = RenderData(
                            label = "otaMAD⋅top",
                            visitedLabel = "otaMAD⋅top",
                            style = RenderData.BLUE
                        )
                    )
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
        val endpoint = "${llmConfig.url}/chat/completions"
        val messages = listOf(
            LLMMessage(role = "system", content = llmConfig.system),
            LLMMessage(role = "user", content = text)
        )
        val httpResponse = client.post(endpoint) {
            contentType(ContentType.Application.Json)
            headers {
                append(HttpHeaders.Authorization, "Bearer ${llmConfig.apikey}")
            }
            setBody(LLMRequest(
                model = llmConfig.model,
                messages = messages,
                stream = false,
                temperature = llmConfig.temperature,
                thinking = LLMThinking("disabled")
            ))
        }
        return when (httpResponse.status) {
            HttpStatusCode.OK -> {
                val response = httpResponse.body<LLMResponse>()
                response.choices.firstOrNull() ?.message ?.content ?.toBooleanStrictOrNull() ?: true
            }
            HttpStatusCode.BadRequest -> false
            else -> true
        }
    }
}