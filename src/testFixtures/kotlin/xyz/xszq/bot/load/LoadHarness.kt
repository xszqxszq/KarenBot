package xyz.xszq.bot.load

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.Database
import xyz.xszq.bot.PluginLoader
import xyz.xszq.bot.RuntimeControl
import xyz.xszq.bot.mockTencentCOS
import xyz.xszq.bot.service.WordFilter
import xyz.xszq.bot.subscribe.SubscribeManager
import xyz.xszq.bot.util.json
import xyz.xszq.bot.webhook.WebhookRouter
import java.net.ServerSocket
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * 并发压测
 *
 * @property fakeQQ 假 QQ 服务器
 * @property subscribeThreads 订阅处理线程数
 * @property label 指标名称
 */
class LoadHarness(
    database: Database,
    val fakeQQ: FakeQQServer = FakeQQServer(),
    subscribeThreads: Int ?= null,
    label: String = "压测"
) {
    private val logger = KotlinLogging.logger {}
    private val port: Int = freePort()
    private val executor: ExecutorService ?= subscribeThreads ?.let { threads ->
        Executors.newFixedThreadPool(threads)
    }
    private val ownedDispatcher: ExecutorCoroutineDispatcher? = executor?.asCoroutineDispatcher()
    private val server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>
    private val webhook = LoadWebhookClient(port, fakeQQ.appId, fakeQQ.clientSecret)
    private val sequence = AtomicInteger(0)

    /**
     * 压测指标
     */
    val metrics = LoadMetrics(label)

    /**
     * 订阅处理使用的调度器
     */
    val dispatcher: CoroutineDispatcher = ownedDispatcher ?: Dispatchers.IO

    /**
     * 插件加载器
     */
    val pluginLoader: PluginLoader = PluginLoader(
        api = fakeQQ.api,
        cos = mockTencentCOS(),
        database = database,
        subscribes = SubscribeManager(dispatcher),
        control = object : RuntimeControl {
            override var debugLog: Boolean = false
            override fun reloadConfig() {}
        }
    )

    init {
        server = embeddedServer(Netty, host = "127.0.0.1", port = port) {
            install(ContentNegotiation) {
                json(json)
            }
            WebhookRouter(logger, pluginLoader, WordFilter(emptyList())) { null }.configure(this)
        }.start(wait = false)
    }

    /**
     * 等待 Webhook 服务
     */
    suspend fun start() = webhook.awaitReady()

    /**
     * 执行压测
     *
     * @param plan 场景
     * @param concurrency 并发工作者数量
     * @param timeoutMs 单个事件的回复等待上限（毫秒）
     * @param report 是否输出报告
     * @return 压测总耗时（毫秒）
     */
    suspend fun drive(
        plan: List<LoadScenario>,
        concurrency: Int,
        timeoutMs: Long = 60_000,
        report: Boolean = true
    ): Long {
        val queue = ConcurrentLinkedQueue(expand(plan))
        val startedAt = System.nanoTime()
        coroutineScope {
            (0 until concurrency).map {
                launch(Dispatchers.IO) {
                    while (true) {
                        val request = queue.poll() ?: break
                        runScenario(request, timeoutMs)
                    }
                }
            }.joinAll()
        }
        val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000
        if (report)
            metrics.report(elapsedMs)
        return elapsedMs
    }

    /**
     * 清空已记录的指标与回复
     */
    fun reset() {
        metrics.reset()
        fakeQQ.reset()
    }

    /**
     * 关闭服务并释放线程池
     */
    fun close() {
        webhook.close()
        server.stop(500, 1_000)
        ownedDispatcher?.close()
    }

    /**
     * 预热压测
     *
     * @param scenario 场景
     * @param timeoutMs 回复等待上限（毫秒）
     * @return 回复文本
     */
    suspend fun warmup(scenario: LoadScenario, timeoutMs: Long = 30_000): String? {
        val eventId = "warmup-${scenario.name}-${sequence.getAndIncrement()}"
        webhook.post(bodyOf(eventId, scenario))
        return fakeQQ.awaitReply(eventId, timeoutMs) ?.text
    }

    /**
     * 按命令预热
     *
     * @param plan 场景
     * @param rounds 轮数
     * @return 没有收到回复的场景名
     */
    suspend fun warmup(plan: List<LoadScenario>, rounds: Int = WARMUP_ROUNDS): List<String> =
        buildList {
            repeat(rounds) {
                plan.distinctBy { it.content }.forEach { scenario ->
                    if (warmup(scenario) == null)
                        add(scenario.name)
                }
            }
        }.distinct()

    private suspend fun runScenario(request: Request, timeoutMs: Long) {
        val scenario = request.scenario
        val eventId = "${scenario.name}-${sequence.getAndIncrement()}"
        metrics.submit(eventId, scenario.name)
        val ack = runCatching {
            webhook.post(bodyOf(eventId, scenario))
        }.getOrElse { e ->
            metrics.fail(eventId, "提交失败 ${e.message}")
            return
        }
        metrics.ack(ack)
        val reply = fakeQQ.awaitReply(eventId, timeoutMs)
        if (reply == null)
            metrics.fail(eventId, "超时未回复")
        else
            metrics.complete(eventId)
    }

    private fun bodyOf(eventId: String, scenario: LoadScenario) = webhook.groupMessage(
        eventId = eventId,
        group = "load-group-${scenario.group}",
        user = "load-user-${scenario.user}",
        content = scenario.content
    )

    private fun expand(plan: List<LoadScenario>): List<Request> = plan.flatMap { scenario ->
        (0 until scenario.weight).map { Request(scenario) }
    }.shuffled()

    private fun freePort(): Int = ServerSocket(0).use { socket -> socket.localPort }

    /**
     * 计划中的一次事件
     */
    private class Request(
        val scenario: LoadScenario
    )

    private companion object {
        const val WARMUP_ROUNDS = 2
    }
}
