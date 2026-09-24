package xyz.xszq.bot.chunithm

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import xyz.xszq.bot.*
import xyz.xszq.bot.chunithm.api.ChunithmAPI
import xyz.xszq.bot.chunithm.component.ChunithmData
import xyz.xszq.bot.chunithm.database.MaimaiSettingsTable
import xyz.xszq.bot.chunithm.database.ProberBindTable
import xyz.xszq.bot.database.newSuspendedTransaction
import xyz.xszq.bot.payload.AdminCheckRequest
import xyz.xszq.bot.subscribe.Channel
import kotlin.test.Test
import kotlin.test.assertEquals

class ChunithmTest : ChunithmDatabaseTest() {

    private val username = "xszqxszq"

    @Test
    fun runAll() = runTest {
        lateinit var divingFish: MockDivingFish
        val sandbox = setChunithm(
            scope = this,
            database = database,
            backends = { data ->
                val prober = MockDivingFish(data)
                divingFish = prober
                listOf(prober.backend())
            }
        )
        try {
            newSuspendedTransaction(db = database) {
                ProberBindTable["test-user", "diving-fish", "username"] = username
                ProberBindTable["test-user", "diving-fish", "id"] = divingFish.bind("test-user")
                MaimaiSettingsTable["test-user", "prober"] = "diving-fish"
            }
            testB50Maxscore(sandbox)
            testB50(sandbox)
            testLevelList(sandbox)
            testScoreList(sandbox)
            testMusic(sandbox)
            testSearchFamily(sandbox)
            testAliases(sandbox)
            testDeleteAlias(sandbox)
            testButtons(sandbox)
            testHelp(sandbox)
            testDefault(sandbox)
            testCollections(sandbox)
            testInvalidCollections(sandbox)
            testPreview(sandbox)
        } finally {
            sandbox.cleanup()
        }
    }

    private suspend fun testB50Maxscore(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedWithImage(sandbox, sandbox.user() says "/b50 maxscore")
    }

    private suspend fun testB50(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "/b50")
    }

    private suspend fun testLevelList(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedWithImage(sandbox, sandbox.user() says "14定数表")
    }

    private suspend fun testScoreList(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "14分数列表")
    }

    private suspend fun testMusic(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "id 3")
        assertRepliedAny(sandbox, sandbox.user() says "查歌 B.B.K.K.B.K.K.")
        assertRepliedAny(sandbox, sandbox.user() says "定数查歌 14.0")
        assertRepliedAny(sandbox, sandbox.user() says "随个")
        assertRepliedAny(sandbox, sandbox.user() says "3是什么歌")
    }

    private suspend fun testSearchFamily(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "谱师查歌 Jack")
        assertRepliedAny(sandbox, sandbox.user() says "版本查歌 CHUNITHM")
        assertRepliedAny(sandbox, sandbox.user() says "曲师查歌 nora2r")
        assertRepliedAny(sandbox, sandbox.user() says "正则查歌 B\\.B\\.K\\.K")
        assertRepliedAny(sandbox, sandbox.user() says "BPM查歌 170")
        assertRepliedAny(sandbox, sandbox.user() says "搜索 其他游戏")
        assertRepliedAny(sandbox, sandbox.tapButton("chunithm-search-designer", "Jack\n1"))
        assertRepliedAny(sandbox, sandbox.tapButton("chunithm-search-version", "CHUNITHM\n1"))
        assertRepliedAny(sandbox, sandbox.tapButton("chunithm-search-artist", "nora2r\n1"))
        assertRepliedAny(sandbox, sandbox.tapButton("chunithm-search-bpm", "170\n1"))
        assertRepliedAny(sandbox, sandbox.tapButton("chunithm-search-combo", "其他游戏\n1"))
    }

    private suspend fun testAliases(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "3有什么别名")
        assertRepliedAny(sandbox, sandbox.user() says "添加别名 3 测试别名")
    }

    private suspend fun testDeleteAlias(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "添加别名 3 test-del")
        assertReplied(sandbox, sandbox.user() says "删除别名 3 test-del", "别名已删除")
        assertReplied(sandbox, sandbox.user() says "删除别名", "使用方法")
        assertReplied(sandbox, sandbox.user() says "删除别名 3 不存在的别名", "该别名不存在")
        assertReplied(sandbox, sandbox.user() says "删除别名 不存在的歌曲 任意", "未找到该歌曲")
        sandbox.clear()
        sandbox.user("not-admin") says "删除别名 3 test-del"
        assertEquals(0, sandbox.replies.size)
    }

    private suspend fun testPreview(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "预览 B.B.K.K.B.K.K.")
    }

    private suspend fun testButtons(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.tapButton("chunithm-search-word", "B.B.K.K.B.K.K.\n1"))
        assertRepliedAny(sandbox, sandbox.tapButton("chunithm-search-level", "14.0:14.0\n1"))
    }

    private suspend fun testHelp(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "/chu")
    }

    private suspend fun testDefault(sandbox: BotSandbox) {
        sandbox.clear()
        assertReplied(sandbox, sandbox.user() says "/chu 默认", "设置成功")
    }

    private suspend fun testCollections(sandbox: BotSandbox) {
        sandbox.clear()
        assertReplied(sandbox, sandbox.user() says "设置chu", "支持以下设定")
        assertReplied(sandbox, sandbox.user() says "设置头像 500", "设置头像成功")
        assertEquals(500, MaimaiSettingsTable.settings("test-user").avatar)
        assertReplied(sandbox, sandbox.user() says "设置头像 天王洲 なずな", "设置头像成功")
        assertReplied(sandbox, sandbox.user() says "设置牌子 10129", "设置牌子成功")
        assertEquals(10129, MaimaiSettingsTable.settings("test-user").plate)
        assertReplied(sandbox, sandbox.user() says "设置牌子 41011", "设置牌子成功")
        assertEquals(41011, MaimaiSettingsTable.settings("test-user").plate)
        assertReplied(sandbox, sandbox.user() says "设置头像 不存在的头像", "使用方法")
        assertReplied(sandbox, sandbox.user() says "设置牌子 不存在的牌子", "使用方法")
    }

    private suspend fun testInvalidCollections(sandbox: BotSandbox) {
        newSuspendedTransaction(db = database) {
            MaimaiSettingsTable["test-user", MaimaiSettingsTable.ICON_KEY] = "999999"
            MaimaiSettingsTable["test-user", MaimaiSettingsTable.PLATE_KEY] = "999999"
        }
        sandbox.clear()
        assertRepliedWithImage(sandbox, sandbox.user() says "/b50")
    }
}

/**
 * 建立中二测试沙箱
 *
 * @param scope 测试作用域
 * @param database 测试数据库
 * @param backends 替换使用的查分器后端
 * @return 测试沙箱
 */
suspend fun setChunithm(
    scope: TestScope,
    database: org.jetbrains.exposed.sql.Database,
    backends: ((ChunithmData) -> List<ChunithmAPI>) ?= null
): BotSandbox {
    val sandbox = BotSandbox(scope, mockTencentCOS(), database)
    sandbox.pluginLoader.subscribes.subscribe(
        "admin", Channel<AdminCheckRequest>("admin-check") { data ->
            data.deferred.complete(data.userId == "test-user")
        }
    )
    val chunithm = Chunithm().apply {
        plugin = "chunithm"
        pluginLoader = sandbox.pluginLoader
        configPath = "./config/chunithm.yml"
        dataPath = "./data/chunithm"
        backends?.let { factory ->
            val defaults = createBackends
            createBackends = {
                val replaced = factory(chunithmData).associateBy { it.id }
                defaults().map { backend -> replaced[backend.id] ?: backend }
            }
        }
    }
    chunithm.load()
    chunithm.image.manager.init()
    sandbox.cleanup = { chunithm.unload() }
    return sandbox
}