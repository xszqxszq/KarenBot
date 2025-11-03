package xyz.xszq.bot

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import xyz.xszq.bot.config.BotConfig
import xyz.xszq.bot.config.CosConfig
import xyz.xszq.bot.config.ForwardConfig
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.message.MessageChain
import xyz.xszq.bot.message.PlainText
import java.io.File

/**
 * Global json serializer.
 */
val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
}

/**
 * Entry point.
 */
@OptIn(ExperimentalHoplite::class)
@Suppress("OPT_IN_USAGE")
fun main() {
    /* Initialize */
    val botConfig = ConfigLoaderBuilder.default()
        .addFileSource("./config/bot.yml")
        .withExplicitSealedTypes()
        .build()
        .loadConfigOrThrow<BotConfig>()
    val forwardConfig = if (botConfig.forward)
        ConfigLoaderBuilder.default()
            .addFileSource("./config/forward.yml")
            .withExplicitSealedTypes()
            .build()
            .loadConfigOrThrow<ForwardConfig>()
    else
        null
    val cosConfig = ConfigLoaderBuilder.default()
        .addFileSource("./config/cos.yml")
        .withExplicitSealedTypes()
        .build()
        .loadConfigOrThrow<CosConfig>()
    val sensitiveWords = File("./config/sensitive.txt").let {
        if (it.exists()) it.readText().replace("\r", "").split("\n")
        else listOf()
    }

    val filter = WordFilter(sensitiveWords)
    val api = OpenAPI(botConfig, filter)
    val cos = TencentCos(cosConfig)
    val logger = KotlinLogging.logger {}

    val pluginLoader = PluginLoader(api, cos)
    pluginLoader.reloadAllPlugins()

    System.getProperty("cli") ?.let {
        GlobalScope.launch(Dispatchers.IO) {
            readInput(pluginLoader)
        }
    }

    /* Start Webhook Server */
    embeddedServer(
        Netty,
        port = botConfig.port,
        host = "0.0.0.0"
    ) {
        install(ContentNegotiation) {
            json(json)
        }
        configureRouting(logger, pluginLoader, filter, forwardConfig)
    }.start(wait = true)
}
@OptIn(DelicateCoroutinesApi::class)
fun readInput(pluginLoader: PluginLoader) {
    while (true) {
        val input = readln()
        GlobalScope.launch(Dispatchers.IO) {
            pluginLoader.manualTrigger(
                MessageEvent(pluginLoader.bot, "", "",
                    MessageChain(PlainText(input)), User(pluginLoader.bot, "BD11EC5ADAE7A0CA792984F3EC63A165")
                ))
        }
    }
}