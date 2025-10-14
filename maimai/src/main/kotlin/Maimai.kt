package xyz.xszq.bot

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.xszq.bot.api.DivingFish
import xyz.xszq.bot.api.LXNS
import xyz.xszq.bot.api.MaimaiAPI
import xyz.xszq.bot.api.Local
import xyz.xszq.bot.component.*
import xyz.xszq.bot.config.DatabaseConfig
import xyz.xszq.bot.config.TokenConfig
import xyz.xszq.bot.controller.ApiController
import xyz.xszq.bot.controller.Controller
import xyz.xszq.bot.database.*
import xyz.xszq.bot.event.Event
import xyz.xszq.bot.event.GroupEvent
import xyz.xszq.bot.event.MessageEvent
import kotlin.reflect.full.primaryConstructor

@Suppress("unused")
class Maimai: Plugin() {
    /**
     * Tokens.
     */
    @OptIn(ExperimentalHoplite::class)
    val tokens = ConfigLoaderBuilder.Companion.default()
        .addFileSource("./config/maimai-tokens.yml")
        .withExplicitSealedTypes()
        .build()
        .loadConfigOrThrow<TokenConfig>()
    /**
     * Prober Backends.
     */
    val localConnector = LocalConnector()
    val local = Local(localConnector)
    val backends = listOf(
        local,
        DivingFish(tokens.tokens["diving-fish"].toString(), local),
        LXNS(
            tokens.tokens["lxns"].toString(),
            tokens.tokens["lxns-oa-id"].toString(),
            tokens.tokens["lxns-oa-secret"].toString(),
            tokens.tokens["lxns-oa-callback"].toString(),
            local
        )
    )
    fun backend(name: String) = backends.first { it.name == name }
    suspend fun backendsWithPriority(
        event: MessageEvent,
        args: String
    ): List<MaimaiAPI> {
        var backends = listOf(
            backend("diving-fish"),
            backend("lxns"),
        ).toMutableList()
        MaimaiSettingsTable[event.sender.id, "prober"] ?.let { prefer ->
            backends = (backends.filter { it.name == prefer })
                .toMutableList()
        }
        if (event.sender.id in localConnector.config.allowed ||
            (event is GroupEvent && event.group.id in localConnector.config.allowed))
            backends.add(backend("local"))
        return backends
    }
    /**
     * Modules.
     */
    val image = MaimaiImage()
    val query = MaimaiQuery(this)
    val aliases = AliasesSearch(this)
    val api = ApiController(this)
    /**
     * Database.
     */
    lateinit var database: Database
    /**
     * Config.
     */
    @OptIn(ExperimentalHoplite::class)
    val databaseConfig = ConfigLoaderBuilder.Companion.default()
        .addFileSource("./config/database.yml")
        .withExplicitSealedTypes()
        .build()
        .loadConfigOrThrow<DatabaseConfig>()

    /**
     * Init Maimai Plugin.
     */
    @OptIn(ExperimentalHoplite::class)
    override fun load() {
        localConnector.load()

        // Database Init
        database = Database.connect(
            databaseConfig.url, databaseConfig.driver,
            databaseConfig.username, databaseConfig.password
        )
        transaction {
            listOf(
                MaimaiBindTable, DivingFishBindTable, QQBindTable, MusicAliasesTable, MusicAliasesVoteTable,
                MaimaiSettingsTable, ArcadeTable, ArcadeGroupTable, ArcadeGroupBindTable
            ).forEach { table ->
                if (!table.exists())
                    SchemaUtils.create(table)
            }
        }

        // Backends & Modules init
        runBlocking {
            api.listen()
            coroutineScope {
                launch {
                    logger.info { "[舞萌] 正在加载图片中……" }
                    image.loadImage()
                }
                launch {
                    backends.forEach { backend ->
                        logger.info { "[舞萌] 正在加载数据源 ${backend.name}……" }
                        backend.load()
                        if (backend.name == "local") {
                            launch {
                                logger.info { "[舞萌] 别名初始化中……" }
                                aliases.init()
                            }
                        }
                    }
                }
            }
        }

        // Load Controllers
        Controller::class.sealedSubclasses.forEach {
            val controller = it.primaryConstructor!!.call(this)
            controller.setRoute()
        }
        // Set route
        setRoute()

        logger.info { "[舞萌] 插件加载完成。" }
    }

    fun musics() = local.musics.values
    fun music(id: Int) = local.musics[id]
    fun charts() = musics().flatMap { it.charts }

    /**
     * Command Routes.
     */
    fun setRoute() = route("/mai") {
    }

    override fun unload() {
        aliases.close()
        api.close()
        logger.info { "[舞萌] 插件已卸载。" }
    }

    companion object {
        suspend fun Event.textMode() = if (this is MessageEvent)
            MaimaiSettingsTable[sender.id, "text-mode"] == "1" else false
    }
}