package xyz.xszq.bot.maimai

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.junit.jupiter.api.Tag
import xyz.xszq.bot.load.FakeQQServer
import xyz.xszq.bot.load.LoadHarness
import xyz.xszq.bot.load.LoadScenario
import xyz.xszq.bot.load.NetworkGate
import xyz.xszq.bot.maimai.component.MaimaiData
import xyz.xszq.bot.maimai.database.MaimaiSettingsTable
import xyz.xszq.bot.maimai.database.ProberBindTable
import xyz.xszq.bot.payload.AdminCheckRequest
import xyz.xszq.bot.subscribe.Channel
import kotlin.test.Test

/**
 * 并发压测
 */
@Tag("load")
class MaimaiLoadTest : MaimaiDatabaseTest() {
    private val logger = KotlinLogging.logger {}

    private companion object {
        const val PROBER_LATENCY_MS = 25L
        const val QQ_LATENCY_MS = 20L
        const val CONCURRENCY = 16
        const val SATURATION = 64
        const val ROUNDS = 4
        const val EVENT_TIMEOUT_MS = 30_000L
        const val DATA_PATH = "./data/maimai"
        val USERS = listOf(
            "load-user-0" to "lxns",
            "load-user-1" to "lxns",
            "load-user-2" to "diving-fish",
            "load-user-3" to "diving-fish"
        )
    }

    @Test
    fun runAll() = runBlocking {
        // 拦截一切外部网络请求
        val gate = NetworkGate()
        gate.install()
        try {
            check(gate.probeConnect(NetworkGate.CONNECT_PROBE_HOST, NetworkGate.CONNECT_PROBE_PORT)) {
                "外部连接拦截未生效"
            }
            check(gate.probeResolve(NetworkGate.RESOLVE_PROBE_HOST)) {
                "外部域名解析拦截未生效"
            }
            val controlCount = gate.blocked.size
            measure(
                subscribeThreads = null,
                concurrency = CONCURRENCY,
                label = "Dispatchers.IO 并发 $CONCURRENCY"
            )
            measure(
                subscribeThreads = 4,
                concurrency = CONCURRENCY,
                label = "订阅线程池 4 并发 $CONCURRENCY"
            )
            measure(
                subscribeThreads = null,
                concurrency = SATURATION,
                label = "Dispatchers.IO 并发 $SATURATION"
            )
            val leaked = gate.blocked.drop(controlCount)
            check(leaked.isEmpty()) { "压测期间出现外部网络访问: $leaked" }
            logger.info { "[压测] 外部网络拦截生效，对照已触发，全程零外部连接与零外部解析" }
        } finally {
            gate.uninstall()
        }
    }

    /**
     * 多种命令请求混合，模拟真实流量
     */
    private fun plan(): List<LoadScenario> = listOf(
        LoadScenario("落雪b50", "/b50", weight = 6, user = 0),
        LoadScenario("水鱼b50", "/b50", weight = 6, user = 2),
        LoadScenario("落雪b40", "/b40", weight = 2, user = 1),
        LoadScenario("单曲成绩", "歌50 852", weight = 5, user = 0),
        LoadScenario("分数列表", "13分数列表", weight = 6, user = 1),
        LoadScenario("定数表", "13定数表", weight = 4, user = 1),
        LoadScenario("查歌", "查歌 TiamaT", weight = 8, user = 3),
        LoadScenario("定数查歌", "定数查歌 12.0", weight = 6, user = 2),
        LoadScenario("帮助", "/mai", weight = 4, user = 3),
        LoadScenario("分数线", "分数线 紫852 100.5", weight = 3, user = 3)
    )

    private suspend fun measure(
        subscribeThreads: Int ?,
        concurrency: Int,
        label: String
    ) {
        val fakeQQ = FakeQQServer(latencyMs = QQ_LATENCY_MS)
        val harness = LoadHarness(database, fakeQQ, subscribeThreads, label)
        harness.start()
        var maimai: Maimai ?= null
        try {
            harness.pluginLoader.subscribes.subscribe(
                "admin",
                Channel<AdminCheckRequest>("admin-check") { data ->
                    data.deferred.complete(true)
                }
            )
            val data = MaimaiData(dataPath = DATA_PATH).apply { load() }
            val lxns = MockLxnsProber(data, latencyMs = PROBER_LATENCY_MS)
            val divingFish = MockDivingFish(data, latencyMs = PROBER_LATENCY_MS)
            bindUsers(lxns, divingFish)

            maimai = Maimai().apply {
                plugin = "maimai"
                pluginLoader = harness.pluginLoader
                configPath = "./config/maimai.yml"
                dataPath = DATA_PATH
                createBackends = { listOf(divingFish.backend(), lxns.backend()) }
            }
            maimai.load()
            maimai.image.manager.init()

            // 预热
            val missed = harness.warmup(plan())
            check(missed.isEmpty()) { "预热未收到回复的场景: $missed，压测数据不可信" }
            harness.reset()

            val rounds = List(ROUNDS) { plan() }.flatten()
            harness.drive(rounds, concurrency, timeoutMs = EVENT_TIMEOUT_MS, report = false)
            harness.reset()
            harness.drive(rounds, concurrency, timeoutMs = EVENT_TIMEOUT_MS)
        } finally {
            runCatching { maimai ?.unload() }
            harness.close()
        }
    }

    private suspend fun bindUsers(
        lxns: MockLxnsProber,
        divingFish: MockDivingFish
    ) = newSuspendedTransaction(db = database) {
        USERS.forEach { (openid, prober) ->
            MaimaiSettingsTable[openid, "prober"] = prober
            when (prober) {
                "lxns" -> ProberBindTable[openid, "lxns", "friend-code"] =
                    lxns.friendCode.toString()
                else -> ProberBindTable[openid, "diving-fish", "id"] = divingFish.bind(openid)
            }
        }
    }
}
