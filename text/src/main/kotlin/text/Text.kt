package xyz.xszq.bot.text

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import io.ktor.client.plugins.*
import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula
import xyz.xszq.bot.Plugin
import xyz.xszq.bot.event.GroupMessageEvent
import xyz.xszq.bot.message.Image
import xyz.xszq.bot.message.Markdown
import xyz.xszq.bot.newLine
import xyz.xszq.bot.reply
import xyz.xszq.bot.text.config.TextConfig
import xyz.xszq.bot.util.useTempFile
import java.awt.Color
import kotlin.random.Random

/**
 * 文本功能插件
 */
@Suppress("unused")
class Text: Plugin() {
    lateinit var textConfig: TextConfig
    lateinit var stereotypes: StereotypesPresets

    private companion object {
        val auditPrompt = buildString {
            appendLine("审核发病小作文的目标名称，只审核用户输入。")
            appendLine("仅两类内容返回 false：")
            appendLine("1. 政治敏感，现实政治人物、重大政治事件、政治组织、口号、机构、")
            appendLine("政策与意识形态争议，以及明显代称、谐音、缩写或影射。")
            appendLine("2. 明显露骨色情，明确描述性行为、性器官、未成年性或强制性内容，")
            appendLine("或名称本身是明显色情用语。")
            appendLine("脏话辱骂、普通玩梗、暧昧表达、性暗示双关和拿不准的内容都返回 true。")
            appendLine("只输出 true 或 false。")
        }.trim()
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

        setRoute()
        logger.info { "[文本] 插件加载完成。" }
    }
    /**
     * 注册路由
     */
    suspend fun setRoute() = route {
        // 获取帮助
        equalsTo(listOf("帮助", "help")) {
            reply(Markdown.create {
                line(bold("可怜BOT"))
                line()
                line("请点击下方查看帮助：")
                keyboard {
                    row {
                        link("查看帮助", "https://docs.karenbot.cn/features", id = "1")
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
            if (!audit(target)) {
                reply("检测到疑似违规内容，请检查输入")
                return@startsWith
            }
            val result = stereotypes.texts.random(
                Random(System.currentTimeMillis())
            ).replace("{target_name}", target)
            reply(result)
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
    /**
     * 用 LLM 审核发病小作文的用户输入
     *
     * @param input 用户输入
     * @return 是否合规
     */
    suspend fun audit(input: String): Boolean {
        val client = pluginLoader.llmClient ?: return true
        return runCatching {
            val content = client.chat(scene = "audit") {
                system(auditPrompt)
                user(input)
            }
            content.trim() == "true"
        }.getOrElse { e ->
            e !is ClientRequestException
        }
    }
}