package xyz.xszq.bot.chunithm

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.junit.jupiter.api.Tag
import xyz.xszq.bot.chunithm.component.ChunithmData
import xyz.xszq.bot.chunithm.database.MaimaiSettingsTable
import xyz.xszq.bot.chunithm.database.ProberBindTable
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.load.FakeQQServer
import xyz.xszq.bot.load.LoadHarness
import xyz.xszq.bot.load.LoadScenario
import xyz.xszq.bot.load.NetworkGate
import xyz.xszq.bot.payload.AdminCheckRequest
import xyz.xszq.bot.reply
import xyz.xszq.bot.subscribe.Channel
import kotlin.test.Test

/**
 * 并发压测
 */
@Tag("load")
class ChunithmLoadTest : ChunithmDatabaseTest() {
    private val logger = KotlinLogging.logger {}

    private companion object {
        const val PROBER_LATENCY_MS = 25L
        const val QQ_LATENCY_MS = 20L
        const val CONCURRENCY = 16
        const val SATURATION = 64
        const val ROUNDS = 4
        const val EVENT_TIMEOUT_MS = 30_000L
        const val REFRESH_TOKEN = "test-refresh"
        const val DATA_PATH = "./data/chunithm"
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
        LoadScenario("最高分b50", "/b50 maxscore", weight = 3, user = 1),
        LoadScenario("定数表", "14定数表", weight = 4, user = 1),
        LoadScenario("分数列表", "14分数列表", weight = 6, user = 1),
        LoadScenario("查歌", "查歌 B.B.K.K.B.K.K.", weight = 8, user = 3),
        LoadScenario("定数查歌", "定数查歌 14.0", weight = 6, user = 2),
        LoadScenario("谱师查歌", "谱师查歌 Jack", weight = 4, user = 3),
        LoadScenario("是什么歌", "3是什么歌", weight = 4, user = 0),
        LoadScenario("帮助", "/chu", weight = 3, user = 3)
    )

    private suspend fun measure(
        subscribeThreads: Int ?,
        concurrency: Int,
        label: String
    ) {
        val fakeQQ = FakeQQServer(latencyMs = QQ_LATENCY_MS)
        val harness = LoadHarness(database, fakeQQ, subscribeThreads, label)
        harness.start()
        var chunithm: Chunithm ?= null
        try {
            harness.pluginLoader.subscribes.subscribe(
                "admin",
                Channel<AdminCheckRequest>("admin-check") { data ->
                    data.deferred.complete(true)
                }
            )
            harness.pluginLoader.subscribes.subscribe(
                "maimai",
                Channel<MessageEvent>("rhythm-game-bind") { target ->
                    target.reply("请先绑定查分器")
                }
            )
            val data = ChunithmData(dataPath = DATA_PATH)
            val lxnsProber = MockLxnsProber(data, latencyMs = PROBER_LATENCY_MS)
            val lxns = lxnsProber.backend()
            val divingFishProber = MockDivingFish(data, latencyMs = PROBER_LATENCY_MS)
            val divingFish = divingFishProber.backend()
            data.load(lxns)
            bindUsers(lxnsProber, divingFishProber)

            chunithm = Chunithm().apply {
                plugin = "chunithm"
                pluginLoader = harness.pluginLoader
                configPath = "./config/chunithm.yml"
                dataPath = DATA_PATH
                createLxns = { lxns }
                createBackends = { listOf(divingFish, lxns) }
            }
            chunithm.load()
            chunithm.image.manager.init()

            // 预热
            val missed = harness.warmup(plan())
            check(missed.isEmpty()) { "预热未收到回复的场景: $missed，压测数据不可信" }
            harness.reset()

            val rounds = List(ROUNDS) { plan() }.flatten()
            harness.drive(rounds, concurrency, timeoutMs = EVENT_TIMEOUT_MS, report = false)
            harness.reset()
            harness.drive(rounds, concurrency, timeoutMs = EVENT_TIMEOUT_MS)
        } finally {
            runCatching { chunithm ?.unload() }
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
                "lxns" -> {
                    ProberBindTable[openid, "lxns", "chunithm-friend-code"] =
                        lxns.friendCode.toString()
                    ProberBindTable[openid, "lxns", "refresh"] = REFRESH_TOKEN
                }
                else -> ProberBindTable[openid, "diving-fish", "id"] = divingFish.bind(openid)
            }
        }
    }
}
