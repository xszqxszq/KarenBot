package xyz.xszq.bot

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.Database
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
object KarenBotApplication {
    var forwardConfig: ForwardConfig? = null

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        RuntimePaths.relaunchIfNeeded(KarenBotApplication::class.java.name, args)
        start()
    }

    @OptIn(ExperimentalHoplite::class)
    private fun loadBotConfig() = ConfigLoaderBuilder.default()
        .addFileSource("./config/bot.yml")
        .withExplicitSealedTypes()
        .build()
        .loadConfigOrThrow<BotConfig>()

    @OptIn(ExperimentalHoplite::class)
    private fun loadForwardConfig(botConfig: BotConfig) = if (botConfig.forward)
        ConfigLoaderBuilder.default()
            .addFileSource("./config/forward.yml")
            .withExplicitSealedTypes()
            .build()
            .loadConfigOrThrow<ForwardConfig>()
    else
        null

    fun reloadConfig(pluginLoader: PluginLoader) {
        val botConfig = loadBotConfig()
        val forwardConfig = loadForwardConfig(botConfig)

        pluginLoader.api.reloadConfig(botConfig)
        this.forwardConfig = forwardConfig
    }

    @OptIn(ExperimentalHoplite::class)
    @Suppress("OPT_IN_USAGE")
    suspend fun start() {
        /* Initialize */
        val botConfig = loadBotConfig()
        forwardConfig = loadForwardConfig(botConfig)
        val cosConfig = ConfigLoaderBuilder.default()
            .addFileSource("./config/cos.yml")
            .withExplicitSealedTypes()
            .build()
            .loadConfigOrThrow<CosConfig>()
        val sensitiveWords = File("./config/sensitive.txt").let {
            if (it.exists()) it.readText().replace("\r", "").split("\n")
            else listOf()
        }

        val database = Database.connect(
            url = botConfig.database.url,
            driver = botConfig.database.driver,
            user = botConfig.database.username,
            password = botConfig.database.password
        )

        val filter = WordFilter(sensitiveWords)
        val api = OpenAPI(botConfig, filter)
        val cos = TencentCos(cosConfig)
        val logger = KotlinLogging.logger {}

        val pluginLoader = PluginLoader(api, cos, database)
        pluginLoader.reloadAllPlugins()

        System.getProperty("cli")?.let {
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
            configureRouting(logger, pluginLoader, filter)
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
}
