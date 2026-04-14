package xyz.xszq.bot

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
import xyz.xszq.bot.chunithm.api.ChunithmAPI
import xyz.xszq.bot.chunithm.api.DivingFish
import xyz.xszq.bot.chunithm.api.LXNS
import xyz.xszq.bot.chunithm.component.AliasesSearch
import xyz.xszq.bot.chunithm.component.ChunithmData
import xyz.xszq.bot.chunithm.component.ChunithmQuery
import xyz.xszq.bot.chunithm.component.MarkdownTemplates
import xyz.xszq.bot.chunithm.config.ChunithmConfig
import xyz.xszq.bot.chunithm.controller.Controller
import xyz.xszq.bot.chunithm.database.MaimaiSettingsTable
import xyz.xszq.bot.chunithm.database.MusicAliasesTable
import xyz.xszq.bot.chunithm.database.MusicAliasesVoteTable
import xyz.xszq.bot.event.Event
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.subscribe.SubscribeBuilder
import kotlin.reflect.full.primaryConstructor

@Suppress("unused")
class Chunithm: Plugin() {
    lateinit var config: ChunithmConfig
    lateinit var backends: List<ChunithmAPI>
    lateinit var chunithmData: ChunithmData
    lateinit var query: ChunithmQuery
    lateinit var aliases: AliasesSearch
    private val controllers = mutableListOf<Controller>()
    private lateinit var lxns: LXNS

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun backend(
        name: String
    ) = backends.first { it.id == name }

    @OptIn(ExperimentalHoplite::class)
    override suspend fun load() {
        config = ConfigLoaderBuilder.default()
            .addFileSource("./config/chunithm.yml")
            .withExplicitSealedTypes()
            .build()
            .loadConfigOrThrow<ChunithmConfig>()

        chunithmData = ChunithmData()
        lxns = LXNS(
            config.tokens["lxns"].toString(),
            config.tokens["lxns-oa-id"].toString(),
            config.tokens["lxns-oa-secret"].toString(),
            config.tokens["lxns-oa-callback"].toString(),
            chunithmData
        )
        backends = listOf(
            DivingFish(config.tokens["diving-fish"].toString(), chunithmData),
            lxns
        )

        MarkdownTemplates.init(this)

        transaction(database) {
            listOf(
                MaimaiSettingsTable, MusicAliasesTable, MusicAliasesVoteTable
            ).forEach { table ->
                if (!table.exists())
                    SchemaUtils.create(table)
            }
        }

        backends.forEach { backend ->
            logger.info { "[中二] 正在加载数据源 ${backend.id}……" }
            backend.load()
            logger.info { "[中二] 数据源 ${backend.id}加载完毕。" }
        }

        scope.launch {
            chunithmData.load(lxns)
            logger.info { "[中二] 中二数据加载完成。" }
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
