package xyz.xszq.bot

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import korlibs.io.file.std.localCurrentDirVfs
import korlibs.math.roundDecimalPlaces
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.xszq.bot.event.Event
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.maimai.api.DivingFish
import xyz.xszq.bot.maimai.api.LXNS
import xyz.xszq.bot.maimai.api.MaimaiAPI
import xyz.xszq.bot.maimai.component.AliasesSearch
import xyz.xszq.bot.maimai.component.MaimaiData
import xyz.xszq.bot.maimai.component.MaimaiQuery
import xyz.xszq.bot.maimai.component.MarkdownTemplates
import xyz.xszq.bot.maimai.component.image.MaimaiImage
import xyz.xszq.bot.maimai.config.MaimaiConfig
import xyz.xszq.bot.maimai.controller.ApiController
import xyz.xszq.bot.maimai.controller.Controller
import xyz.xszq.bot.maimai.database.*
import xyz.xszq.bot.maimai.payload.DivingFishStats
import xyz.xszq.bot.maimai.query.ComboQuery
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.full.primaryConstructor

class Maimai: Plugin() {
    // 配置文件
    lateinit var config: MaimaiConfig
    // 后端
    lateinit var backends: List<MaimaiAPI>
    // 组件
    lateinit var maimaiData: MaimaiData
    lateinit var image: MaimaiImage
    lateinit var query: MaimaiQuery
    lateinit var aliases: AliasesSearch
    lateinit var api: ApiController
    private val controllers = mutableListOf<Controller>()
    // 数据库
    lateinit var database: Database

    // 其他
    var pluginStopped: Boolean = false
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val messageToReplay = ConcurrentHashMap<String, String>()

    fun backend(name: String) = backends.first { it.id == name }

    /**
     * 初始化插件
     */
    @OptIn(ExperimentalHoplite::class, DelicateCoroutinesApi::class)
    override suspend fun load() {
        // 载入配置
        config = ConfigLoaderBuilder.default()
            .addFileSource("./config/maimai.yml")
            .withExplicitSealedTypes()
            .build()
            .loadConfigOrThrow<MaimaiConfig>()

        maimaiData = MaimaiData()
        maimaiData.load()

        backends = listOf(
            DivingFish(config.tokens["diving-fish"].toString(), maimaiData),
            LXNS(
                config.tokens["lxns"].toString(),
                config.tokens["lxns-oa-id"].toString(),
                config.tokens["lxns-oa-secret"].toString(),
                config.tokens["lxns-oa-callback"].toString(),
                maimaiData
            )
        )

        // 各组件初始化
        image = MaimaiImage(maimaiData)
        query = MaimaiQuery(this)
        aliases = AliasesSearch(this)
        api = ApiController(this)

        image.init()
        ComboQuery.init(maimaiData)
        MarkdownTemplates.init(this)

        // 数据库初始化
        database = Database.connect(
            config.database.url, config.database.driver,
            config.database.username, config.database.password
        )
        transaction {
            listOf(
                QQBindTable, MusicAliasesTable, MusicAliasesVoteTable,
                MaimaiSettingsTable, ArcadeTable, ArcadeGroupTable, ArcadeGroupBindTable, GuessGameTable
            ).forEach { table ->
                if (!table.exists())
                    SchemaUtils.create(table)
            }
        }
        scope.launch(Dispatchers.IO) {
            logger.info { "[舞萌] 正在加载图片中……" }
            image.load(scope)
            logger.info { "[舞萌] 图片载入完毕。" }
        }
        scope.launch(Dispatchers.IO) {
            logger.info { "[舞萌] 别名初始化中……" }
            aliases.init()
            logger.info { "[舞萌] 别名初始化完毕。" }
        }
        scope.launch(Dispatchers.IO) {
            logger.info { "[舞萌] 载入拟合定数中……" }
            loadFitLevelValues()
            logger.info { "[舞萌] 拟合定数载入完毕。" }
        }
        scope.launch(Dispatchers.IO) {
            // Controller初始化
            Controller::class.sealedSubclasses.forEach {
                val controller = it.primaryConstructor!!.call(this@Maimai)
                controller.setRoute()
                controllers.add(controller)
            }
        }

        // 各API初始化
        api.listen()
        backends.forEach { backend ->
            scope.launch {
                logger.info { "[舞萌] 正在加载数据源 ${backend.id}……" }
                backend.load()
                logger.info { "[舞萌] 数据源 ${backend.id}加载完毕。" }
            }
        }

        // 配置路由
        setRoute()

        logger.info { "[舞萌] 插件加载完成。" }
    }

    fun musics() = maimaiData.musics.values
    fun music(id: Int) = maimaiData.musics[id]
    fun charts() = musics().flatMap { it.charts }

    suspend fun getGamePreference(
        senderId: String,
        defaultGame: String
    ): String {
        if (!this::database.isInitialized)
            return defaultGame
        return MaimaiSettingsTable[senderId, "game-prior"] ?: defaultGame
    }

    suspend fun setGamePreference(
        senderId: String,
        game: String
    ) {
        if (!this::database.isInitialized)
            return
        MaimaiSettingsTable[senderId, "game-prior"] = game
    }

    /**
     * 配置路由
     */
    suspend fun setRoute() = route("/mai") {
    }

    override suspend fun unload() {
        aliases.close()
        api.close()
        pluginStopped = true
        scope.cancel()
        controllers.forEach { controller ->
            controller.unload()
        }
        TransactionManager.closeAndUnregister(database)
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
                music(id.toInt()) ?.charts ?.getOrNull(difficulty)
                    ?.fitLevelValue = stat.fitLevelValue ?.roundDecimalPlaces(1) ?: return@forEachIndexed
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