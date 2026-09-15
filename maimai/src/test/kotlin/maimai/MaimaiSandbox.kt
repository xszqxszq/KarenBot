package xyz.xszq.bot.maimai

import kotlinx.coroutines.test.TestScope
import org.jetbrains.exposed.sql.Database
import xyz.xszq.bot.BotSandbox
import xyz.xszq.bot.maimai.api.MaimaiAPI
import xyz.xszq.bot.maimai.component.MaimaiData
import xyz.xszq.bot.mockTencentCOS
import xyz.xszq.bot.payload.AdminCheckRequest
import xyz.xszq.bot.subscribe.Channel

/**
 * 建立舞萌测试沙箱
 *
 * @param scope 测试作用域
 * @param database 测试数据库
 * @param backends 替换使用的查分器后端
 * @return 测试沙箱
 */
suspend fun setMaimai(
    scope: TestScope,
    database: Database,
    backends: ((MaimaiData) -> List<MaimaiAPI>) ?= null
): BotSandbox {
    val sandbox = BotSandbox(scope, mockTencentCOS(), database)
    sandbox.pluginLoader.subscribes.subscribe(
        "admin", Channel<AdminCheckRequest>("admin-check") { data ->
            data.deferred.complete(data.userId == "test-user")
        }
    )
    val maimai = Maimai().apply {
        plugin = "maimai"
        pluginLoader = sandbox.pluginLoader
        configPath = "./config/maimai.yml"
        dataPath = "./data/maimai"
        backends?.let { factory ->
            val defaults = createBackends
            createBackends = {
                val replaced = factory(maimaiData).associateBy { it.id }
                defaults().map { backend -> replaced[backend.id] ?: backend }
            }
        }
    }
    maimai.load()
    maimai.image.manager.init()
    sandbox.cleanup = { maimai.unload() }
    return sandbox
}