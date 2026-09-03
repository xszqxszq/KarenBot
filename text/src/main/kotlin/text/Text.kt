package xyz.xszq.bot.text

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import io.ktor.client.plugins.*
import xyz.xszq.bot.Plugin
import xyz.xszq.bot.event.GroupMessageEvent
import xyz.xszq.bot.message.Image
import xyz.xszq.bot.message.Markdown
import xyz.xszq.bot.newLine
import xyz.xszq.bot.reply
import xyz.xszq.bot.text.config.TextConfig
import xyz.xszq.bot.useTempFile
import java.awt.Color
import kotlin.random.Random
import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula

/**
 * 文本插件
 *
 * 词条回复、发病文案、LaTeX 渲染与调试等文本功能
 */
@Suppress("unused")
class Text: Plugin() {
    lateinit var textConfig: TextConfig
    lateinit var stereotypes: StereotypesPresets

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

        setRoute()
        logger.info { "[文本] 插件加载完成。" }
    }
    suspend fun setRoute() = route {
        // 获取帮助
        equalsTo(listOf("帮助", "help")) {
            reply(Markdown.create {
                line(bold("可怜BOT"))
                line()
                line("请点击下方查看帮助：")
                keyboard {
                    row {
                        link("查看帮助", "https://otmdb.cn/bot/features", id = "1")
                    }
                }
            })
        }
        // 内置文本回复预设
        equalsTo("在") {
            reply("bot在")
        }
        equalsTo(listOf("？", "?")) {
            if (this !is GroupMessageEvent || mentions.any { it.isSelf })
                reply("问我干嘛")
        }
        // 配置文本回复预设
        always {
            textConfig.presets[message.text.trim()] ?.let { text ->
                reply(text)
            }
            textConfig.userSpecifiedPresets.firstOrNull { preset ->
                sender.id == preset.openId && message.text.trim() == preset.match
            } ?.let {
                reply(it.reply)
            }
        }
        // 发病小作文
        startsWith("发病") { target ->
            if (target.isBlank()) {
                reply(buildString {
                    appendLine("使用方法：/发病 名字")
                    appendLine("例：/发病 小冰")
                }.trim())
                return@startsWith
            }
            val result = stereotypes.texts.random(
                Random(System.currentTimeMillis())
            ).replace("{target_name}", target)
            if (audit(result))
                reply(result)
            else
                reply("检测到疑似违规内容，请检查输入")
        }
        // 渲染 LaTeX 图片
        startsWith("latex") { latex ->
            useTempFile { result ->
                TeXFormula(latex).createPNG(
                    TeXConstants.STYLE_DISPLAY, 22.0F, result.absolutePath,
                    Color.WHITE, Color.BLACK
                )
                reply(Image(result))
            }
        }
        // 获取 ID
        startsWith("debug") {
            reply(buildString {
                appendLine("用户ID: ${sender.id}")
                if (this@startsWith is GroupMessageEvent)
                    appendLine("群组ID: ${group.id}")
            }.trim().newLine())
        }
    }
    suspend fun audit(text: String): Boolean {
        val client = pluginLoader.llmClient ?: return true
        return try {
            val content = client.chat(scene = "audit") {
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