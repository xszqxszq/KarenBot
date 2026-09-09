package xyz.xszq.bot.chunithm

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.xszq.bot.Plugin
import xyz.xszq.bot.chunithm.api.ChunithmAPI
import xyz.xszq.bot.chunithm.api.DivingFish
import xyz.xszq.bot.chunithm.api.LXNS
import xyz.xszq.bot.chunithm.component.*
import xyz.xszq.bot.chunithm.config.ChunithmConfig
import xyz.xszq.bot.chunithm.controller.Controller
import xyz.xszq.bot.chunithm.database.ChunithmMusicAliasesTable
import xyz.xszq.bot.chunithm.database.ChunithmMusicAliasesVoteTable
import xyz.xszq.bot.chunithm.database.MaimaiSettingsTable
import xyz.xszq.bot.chunithm.query.ComboQuery
import xyz.xszq.bot.event.Event
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.reply
import xyz.xszq.bot.subscribe.SubscribeBuilder
import kotlin.reflect.full.primaryConstructor

/**
 * 中二节奏插件
 */
@Suppress("unused")
class Chunithm: Plugin() {
    var configPath = "./config/chunithm.yml"
    var dataPath = "./data/chunithm"

    // 配置文件
    lateinit var config: ChunithmConfig
    // 后端
    lateinit var backends: List<ChunithmAPI>
    // 组件
    lateinit var chunithmData: ChunithmData
    lateinit var image: ChunithmImage
    lateinit var query: ChunithmQuery
    lateinit var aliases: AliasesSearch
    private val controllers = mutableListOf<Controller>()
    private lateinit var lxns: LXNS

    // 其他
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 按 ID 获取查分器后端
     *
     * @param name ID，`diving-fish`/`lxns`
     * @return 后端
     */
    fun backend(
        name: String
    ) = backends.first { it.id == name }

    /**
     * 初始化插件
     */
    @OptIn(ExperimentalHoplite::class)
    override suspend fun load() {
        // 载入配置
        config = ConfigLoaderBuilder.default()
            .addFileSource(configPath)
            .withExplicitSealedTypes()
            .build()
            .loadConfigOrThrow<ChunithmConfig>()

        chunithmData = ChunithmData(dataPath = dataPath)
        lxns = LXNS(
            config.tokens["lxns"].toString(),
            config.tokens["lxns-oa-id"].toString(),
            config.tokens["lxns-oa-secret"].toString(),
            chunithmData
        )
        backends = listOf(
            DivingFish(
                config.tokens["diving-fish-oa-id"].toString(),
                config.tokens["diving-fish-oa-secret"].toString(),
                chunithmData
            ),
            lxns
        )

        // 各API初始化
        backends.forEach { backend ->
            logger.info { "[中二] 正在加载数据源 ${backend.id}……" }
            backend.load()
            logger.info { "[中二] 数据源 ${backend.id}加载完毕。" }
        }

        // 数据库初始化
        transaction(database) {
            listOf(
                ChunithmMusicAliasesTable, ChunithmMusicAliasesVoteTable
            ).forEach { table ->
                if (!table.exists())
                    SchemaUtils.create(table)
            }
        }

        MarkdownTemplates.init(this)

        chunithmData.load(lxns)
        logger.info { "[中二] 中二数据加载完成。" }

        image = ChunithmImage(chunithmData, dataPath = dataPath)
        image.init()
        ComboQuery.init(chunithmData)

        scope.launch(Dispatchers.IO) {
            logger.info { "[中二] 正在加载图片中……" }
            image.load(scope)
            logger.info { "[中二] 图片载入完毕。" }
        }

        query = ChunithmQuery(this)
        aliases = AliasesSearch(this)
        aliases.init()

        // Controller初始化
        Controller::class.sealedSubclasses.forEach {
            val controller = it.primaryConstructor!!.call(this@Chunithm)
            controller.setRoute()
            controllers.add(controller)
        }

        // 配置路由
        setRoute()

        logger.info { "[中二] 插件加载完成。" }
    }

    override suspend fun unload() {
        aliases.close()
        controllers.forEach { controller ->
            controller.unload()
        }
    }

    /**
     * 注册命令域
     *
     * 命令域下根据用户偏好选择唯一的插件处理其命令
     *
     * @param block 命令域路由代码块
     */
    suspend fun rhythm(
        block: suspend SubscribeBuilder.() -> Unit
    ) {
        route("/chu") {
            domain(
                name = "rhythm",
                value = "chunithm",
                defaultHandler = {
                    MaimaiSettingsTable.defaultGame(sender.id)
                },
                block = block
            )
        }
    }

    /**
     * 注册路由
     */
    suspend fun setRoute() {
        route("/chu", true) {
            startsWith(listOf("默认", "设为默认")) {
                MaimaiSettingsTable.setDefaultGame(sender.id, "chunithm")
                reply("设置成功，在不带“/mai”“/chu”命令前缀时，将默认选择使用中二节奏的相关功能")
            }
        }
    }

    /**
     * 获取全部歌曲
     */
    fun musics() = chunithmData.musics.values
    /**
     * 按 ID 获取歌曲
     *
     * @param id 歌曲 ID
     * @return 歌曲
     */
    fun music(
        id: Int
    ) = chunithmData.musics[id]
    /**
     * 获取全部谱面
     */
    fun charts() = musics().flatMap { it.charts }

    companion object {
        /**
         * 判断是否开启纯文本模式
         */
        suspend fun Event.textMode() = if (this is MessageEvent)
            MaimaiSettingsTable[sender.id, "text-mode"] == "1" else false
    }
}