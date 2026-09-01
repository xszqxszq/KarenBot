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
import xyz.xszq.bot.chunithm.database.ProberBindTable
import xyz.xszq.bot.chunithm.query.ComboQuery
import xyz.xszq.bot.event.Event
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.reply
import xyz.xszq.bot.subscribe.SubscribeBuilder
import kotlin.reflect.full.primaryConstructor

@Suppress("unused")
class Chunithm: Plugin() {
    var configPath = "./config/chunithm.yml"
    var dataPath = "./data/chunithm"

    lateinit var config: ChunithmConfig
    lateinit var backends: List<ChunithmAPI>
    lateinit var chunithmData: ChunithmData
    lateinit var image: ChunithmImage
    lateinit var query: ChunithmQuery
    lateinit var aliases: AliasesSearch
    private val controllers = mutableListOf<Controller>()
    private lateinit var lxns: LXNS

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun backend(
        name: String
    ) = backends.first { it.id == name }

    @OptIn(ExperimentalHoplite::class)
    override suspend fun load() {
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

        backends.forEach { backend ->
            logger.info { "[中二] 正在加载数据源 ${backend.id}……" }
            backend.load()
            logger.info { "[中二] 数据源 ${backend.id}加载完毕。" }
        }

        transaction(database) {
            listOf(
                MaimaiSettingsTable, ChunithmMusicAliasesTable, ChunithmMusicAliasesVoteTable,
                ProberBindTable
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

        Controller::class.sealedSubclasses.forEach {
            val controller = it.primaryConstructor!!.call(this@Chunithm)
            controller.setRoute()
            controllers.add(controller)
        }

        setRoute()
    }

    override suspend fun unload() {
        aliases.close()
        controllers.forEach { controller ->
            controller.unload()
        }
    }

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

    suspend fun setRoute() {
        route("/chu", true) {
            startsWith(listOf("默认", "设为默认")) {
                MaimaiSettingsTable.setDefaultGame(sender.id, "chunithm")
                reply("设置成功，在不带“/mai”“/chu”命令前缀时，将默认选择使用中二节奏的相关功能")
            }
        }
    }

    fun musics() = chunithmData.musics.values
    fun music(
        id: Int
    ) = chunithmData.musics[id]
    fun charts() = musics().flatMap { it.charts }

    companion object {
        suspend fun Event.textMode() = if (this is MessageEvent)
            MaimaiSettingsTable[sender.id, "text-mode"] == "1" else false
    }
}