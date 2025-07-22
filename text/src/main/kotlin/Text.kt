package xyz.xszq.bot

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import xyz.xszq.bot.message.File
import xyz.xszq.bot.message.Image
import xyz.xszq.bot.message.PlainText
import kotlin.collections.random
import kotlin.random.Random

@Suppress("unused")
class Text: Plugin() {
    val presets = buildMap {
        put("在", "bot在")
        put("有人吗", "有bot在哦")
        put("在？", "BIG BOT IS WATCHING YOU")
        put("在吗", "bot一直都在哦")
        put("有美少女吗", "(づ￣3￣)づ")
        put("？", "问我干嘛")
    }
    lateinit var stereotypes: StereotypesConfig
    val randomImage = RandomImage()

    @OptIn(ExperimentalHoplite::class)
    override fun load() {
        stereotypes = ConfigLoaderBuilder.default()
            .addFileSource("./config/stereotypes.yml")
            .withExplicitSealedTypes()
            .build()
            .loadConfigOrThrow<StereotypesConfig>()

        setRoute()
        logger.info { "[文本] 插件加载完成。" }
    }
    fun setRoute() = route {
        always {
            presets[message.text.trim()] ?.let { text ->
                reply(text)
            }
        }
        startsWith("发病") { target ->
            if (target.isBlank()) {
                reply("使用方法：/发病 名字\n例：/发病 小冰")
                return@startsWith
            }
            reply(stereotypes.texts.random(Random(System.currentTimeMillis())).replace("{target_name}", target))
        }
        always {
            if (message.text.isBlank() && message.filter { it !is PlainText }.isEmpty()) {
                reply(Image(randomImage.random()))
            }
            message.firstOrNull { this is File } ?.let {
                reply(it)
            }
        }
        startsWith(listOf("来点金发", "来点金毛", "来点黄毛", "随机金发", "随机黄毛")) {
            reply(Image(randomImage.random()))
        }
    }
}