package xyz.xszq.bot.maimai

import kotlinx.coroutines.test.TestScope
import org.jetbrains.exposed.sql.Database
import xyz.xszq.bot.*

suspend fun setMaimai(scope: TestScope, database: Database): BotSandbox {
    val sandbox = BotSandbox(scope, mockTencentCos(), database)
    val maimai = Maimai().apply {
        plugin = "maimai"
        pluginLoader = sandbox.pluginLoader
        configPath = "../config/maimai.yml"
        dataPath = "../data/maimai"
    }
    maimai.load()
    maimai.image.manager.init()
    sandbox.cleanup = { maimai.unload() }
    return sandbox
}
