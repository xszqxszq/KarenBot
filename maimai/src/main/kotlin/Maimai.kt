package xyz.xszq.bot

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import korlibs.io.file.VfsFile
import korlibs.io.file.std.localCurrentDirVfs
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.xszq.bot.api.DivingFish
import xyz.xszq.bot.api.LXNS
import xyz.xszq.bot.api.Local
import xyz.xszq.bot.api.MaimaiAPI
import xyz.xszq.bot.component.AliasesSearch
import xyz.xszq.bot.component.LocalConnector
import xyz.xszq.bot.component.MaimaiImage
import xyz.xszq.bot.component.MaimaiQuery
import xyz.xszq.bot.config.DatabaseConfig
import xyz.xszq.bot.config.TokenConfig
import xyz.xszq.bot.controller.ApiController
import xyz.xszq.bot.controller.Controller
import xyz.xszq.bot.database.*
import xyz.xszq.bot.event.Event
import xyz.xszq.bot.event.GroupEvent
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.payload.DivingFishStats
import kotlin.collections.listOf
import kotlin.reflect.full.primaryConstructor

@Suppress("unused")
class Maimai: Plugin() {
    /**
     * Tokens.
     */
    lateinit var tokens: TokenConfig
    lateinit var databaseConfig: DatabaseConfig
    /**
     * Prober Backends.
     */
    lateinit var localConnector: LocalConnector
    lateinit var local: Local
    lateinit var backends: List<MaimaiAPI>
    /**
     * Modules.
     */
    lateinit var image: MaimaiImage
    lateinit var query: MaimaiQuery
    lateinit var aliases: AliasesSearch
    lateinit var api: ApiController
    /**
     * Database.
     */
    lateinit var database: Database

    var pluginStopped: Boolean = false

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
            if (prefer.isBlank())
                return@let
            backends = (backends.filter { it.name == prefer })
                .toMutableList()
        }
        if (event.sender.id in localConnector.config.allowed ||
            (event is GroupEvent && event.group.id in localConnector.config.allowed))
            backends.add(backend("local"))
        return backends
    }

    @OptIn(ExperimentalHoplite::class)
    suspend fun init() {
        tokens = ConfigLoaderBuilder.Companion.default()
            .addFileSource("./config/maimai-tokens.yml")
            .withExplicitSealedTypes()
            .build()
            .loadConfigOrThrow<TokenConfig>()

        databaseConfig = ConfigLoaderBuilder.Companion.default()
            .addFileSource("./config/database.yml")
            .withExplicitSealedTypes()
            .build()
            .loadConfigOrThrow<DatabaseConfig>()

        localConnector = LocalConnector()
        local = Local(localConnector)
        backends = listOf(
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

        image = MaimaiImage(this)
        query = MaimaiQuery(this)
        aliases = AliasesSearch(this)
        api = ApiController(this)
    }

    /**
     * Init Maimai Plugin.
     */
    @OptIn(ExperimentalHoplite::class, DelicateCoroutinesApi::class)
    override suspend fun load() {
        init()

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

        image.init()

        // Backends & Modules init

        api.listen()
        backends.forEach { backend ->
            scope.launch {
                logger.info { "[舞萌] 正在加载数据源 ${backend.name}……" }
                backend.load()
                logger.info { "[舞萌] 数据源 ${backend.name}加载完毕。" }
                if (backend.name == "local") {
                    launch(Dispatchers.IO) {
                        logger.info { "[舞萌] 正在加载图片中……" }
                        image.loadImage()
                        logger.info { "[舞萌] 图片载入完毕。" }
                    }
                    launch(Dispatchers.IO) {
                        logger.info { "[舞萌] 别名初始化中……" }
                        aliases.init()
                        logger.info { "[舞萌] 别名初始化完毕。" }
                    }
                    launch(Dispatchers.IO) {
                        logger.info { "[舞萌] 载入拟合定数中……" }
                        loadFitLevelValues()
                        logger.info { "[舞萌] 拟合定数载入完毕。" }
                    }
                    launch(Dispatchers.IO) {
                        // Load Controllers
                        Controller::class.sealedSubclasses.forEach {
                            val controller = it.primaryConstructor!!.call(this@Maimai)
                            controller.setRoute()
                        }
                    }
                }
            }
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
    suspend fun setRoute() = route("/mai") {
    }

    override suspend fun unload() {
        aliases.close()
        api.close()
        pluginStopped = true
        logger.info { "[舞萌] 插件已卸载。" }
    }

    suspend fun loadFitLevelValues() {
        val local = localCurrentDirVfs["./data/maimai/diving-fish-stats.json"]
        val stats = runCatching {
            (backend("diving-fish") as DivingFish).getStats()
        }.onFailure {
            logger.warn { "[舞萌] 拟合定数拉取失败" }
            if (local.exists())
                runCatching {
                    json.decodeFromString<DivingFishStats>(local.readString())
                }.getOrNull()
        }.getOrNull()

        stats ?.charts ?.forEach { (id, chartStats) ->
            chartStats.forEachIndexed { difficulty, stat ->
                music(id.toInt())
                    ?.charts
                    ?.getOrNull(difficulty)
                    ?.fitLevelValue = stat.fitLevelValue ?: return@forEachIndexed
            }
        }
        stats ?.let {
            local.writeString(json.encodeToString(stats))
        }
    }

    companion object {
        suspend fun Event.textMode() = if (this is MessageEvent)
            MaimaiSettingsTable[sender.id, "text-mode"] == "1" else false
    }
}