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
import xyz.xszq.bot.config.BotConfig
import xyz.xszq.bot.config.COSConfig
import xyz.xszq.bot.config.ForwardConfig
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.llm.LLMClient
import xyz.xszq.bot.llm.LLMConfig
import xyz.xszq.bot.message.MessageChain
import xyz.xszq.bot.message.PlainText
import xyz.xszq.bot.webhook.WebhookRouter
import java.io.File
import xyz.xszq.bot.service.OpenAPI
import xyz.xszq.bot.service.TencentCOS
import xyz.xszq.bot.service.WordFilter

/**
 * Bot 运行装配时
 *
 * 负责配置加载、数据库连接和组件组装
 */
class BotRuntime : RuntimeControl {
    private val appLogger = org.slf4j.LoggerFactory.getLogger("xyz.xszq") as ch.qos.logback.classic.Logger
    override var debugLog: Boolean
        get() = appLogger.level == ch.qos.logback.classic.Level.DEBUG
        set(value) {
            appLogger.level = if (value)
                ch.qos.logback.classic.Level.DEBUG
            else
                ch.qos.logback.classic.Level.INFO
        }

    private val logger = KotlinLogging.logger {}
    private lateinit var pluginLoader: PluginLoader

    var forwardConfig: ForwardConfig ?= null

    /**
     * 重载 Bot 相关配置
     */
    override fun reloadConfig() {
        val botConfig = loadBotConfig()
        val forwardConfig = loadForwardConfig(botConfig)

        pluginLoader.api.reloadConfig(botConfig)
        this.forwardConfig = forwardConfig
    }

    /**
     * 启动 Bot
     */
    @OptIn(ExperimentalHoplite::class, DelicateCoroutinesApi::class)
    suspend fun start() {
        // 加载配置
        val botConfig = loadBotConfig()
        forwardConfig = loadForwardConfig(botConfig)
        val cosConfig = loadCOSConfig()
        val sensitiveWords = File("./config/sensitive.txt").let {
            if (it.exists()) it.readText().replace("\r", "").split("\n")
            else listOf()
        }
        val llmConfig = loadLLMConfig()

        // 组装组件
        val database = Database.connect(
            url = botConfig.database.url,
            driver = botConfig.database.driver,
            user = botConfig.database.username,
            password = botConfig.database.password
        )
        val filter = WordFilter(sensitiveWords)
        val api = OpenAPI(botConfig, filter)
        val cos = TencentCOS(cosConfig)
        val llmClient = llmConfig?.let { LLMClient(it) }

        // 拉取 Bot 自身详情
        val me = runCatching { api.getMe() }.onFailure { e ->
            logger.error { "获取机器人信息失败: ${e.message}" }
        }.getOrNull()
        if (me != null)
            logger.info { "机器人 ${me.username}（${me.id}）已就绪" }

        pluginLoader = PluginLoader(
            api = api,
            cos = cos,
            database = database,
            llmClient = llmClient,
            control = this,
            botInfo = me
        )
        pluginLoader.files.start()
        pluginLoader.reloadAllPlugins()

        // 命令行入口
        if (System.getProperty("cli") != null) {
            GlobalScope.launch(Dispatchers.IO) {
                readInput()
            }
        }

        // 启动 Webhook 服务
        embeddedServer(
            Netty,
            port = botConfig.port,
            host = "0.0.0.0"
        ) {
            install(ContentNegotiation) {
                json(json)
            }
            WebhookRouter(logger, pluginLoader, filter) { forwardConfig }.configure(this)
        }.start(wait = true)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun readInput() {
        while (true) {
            val input = readln()
            val event = MessageEvent(
                pluginLoader.bot, "", "",
                MessageChain(PlainText(input)),
                User(pluginLoader.bot, "BD11EC5ADAE7A0CA792984F3EC63A165")
            )
            GlobalScope.launch(Dispatchers.IO) {
                pluginLoader.manualTrigger(event)
            }
        }
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

    @OptIn(ExperimentalHoplite::class)
    private fun loadCOSConfig() = ConfigLoaderBuilder.default()
        .addFileSource("./config/cos.yml")
        .withExplicitSealedTypes()
        .build()
        .loadConfigOrThrow<COSConfig>()

    @OptIn(ExperimentalHoplite::class)
    private fun loadLLMConfig() = runCatching {
        ConfigLoaderBuilder.default()
            .addFileSource("./config/llm.yml")
            .withExplicitSealedTypes()
            .build()
            .loadConfigOrThrow<LLMConfig>()
    }.getOrNull()
}