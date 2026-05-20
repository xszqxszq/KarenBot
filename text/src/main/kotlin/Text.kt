package xyz.xszq.bot

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import korlibs.io.util.UUID
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula
import xyz.xszq.bot.config.TextConfig
import xyz.xszq.bot.event.GroupMessageEvent
import xyz.xszq.bot.message.File
import xyz.xszq.bot.message.Image
import xyz.xszq.bot.message.Markdown
import xyz.xszq.bot.message.PlainText
import xyz.xszq.bot.payload.*
import xyz.xszq.bot.payload.markdown.*
import java.awt.Color
import kotlin.random.Random

@Suppress("unused")
class Text: Plugin() {
    lateinit var textConfig: TextConfig
    lateinit var stereotypes: StereotypesPresets
    val randomImage = RandomImage()

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
            if (message.text.isBlank() && message.filter { it !is PlainText }.isEmpty() && (this !is GroupMessageEvent || mentions.any { it.isSelf })) {
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
}