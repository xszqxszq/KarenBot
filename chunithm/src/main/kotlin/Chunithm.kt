package xyz.xszq.bot

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import kotlinx.coroutines.DelicateCoroutinesApi
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.xszq.bot.chunithm.config.ChunithmConfig
import xyz.xszq.bot.chunithm.database.MaimaiSettingsTable

@Suppress("unused")
class Chunithm: Plugin() {
    lateinit var config: ChunithmConfig
    lateinit var database: Database

    @OptIn(ExperimentalHoplite::class, DelicateCoroutinesApi::class)
    override suspend fun load() {
        config = ConfigLoaderBuilder.default()
            .addFileSource("./config/chunithm.yml")
            .withExplicitSealedTypes()
            .build()
            .loadConfigOrThrow<ChunithmConfig>()

        database = Database.connect(
            config.database.url, config.database.driver,
            config.database.username, config.database.password
        )
        transaction {
            if (!MaimaiSettingsTable.exists())
                SchemaUtils.create(MaimaiSettingsTable)
        }

        setRoute()
        super.load()
    }

    override suspend fun unload() {
    }

    suspend fun rhythm(block: suspend xyz.xszq.bot.subscribe.SubscribeBuilder.() -> Unit) {
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
                MaimaiSettingsTable.setDefaultGame(sender.id, "maimai")
                reply("设置成功，在不带“/mai”“/chu”命令前缀时，将默认选择使用中二节奏的相关功能")
            }
        }
    }

}