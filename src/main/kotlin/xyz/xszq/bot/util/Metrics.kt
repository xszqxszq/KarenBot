package xyz.xszq.bot.util

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlin.coroutines.cancellation.CancellationException

/**
 * Bot 进程内指标
 */
object Metrics {
    internal val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    init {
        ClassLoaderMetrics().bindTo(registry)
        JvmGcMetrics().bindTo(registry)
        JvmMemoryMetrics().bindTo(registry)
        JvmThreadMetrics().bindTo(registry)
        FileDescriptorMetrics().bindTo(registry)
        ProcessorMetrics().bindTo(registry)
        UptimeMetrics().bindTo(registry)
    }

    /**
     * 记录计数值
     *
     * @param name 指标名
     * @param tags 固定取值标签
     */
    fun count(
        name: String,
        vararg tags: Pair<String, String>
    ) = add(name, 1.0, *tags)

    /**
     * 累加计数值
     *
     * @param name 指标名
     * @param amount 累加数量
     * @param tags 固定取值标签
     */
    fun add(
        name: String,
        amount: Double,
        vararg tags: Pair<String, String>
    ) {
        Counter.builder("$name.total").apply {
            tags.forEach { tag -> tag(tag.first, tag.second) }
        }.register(registry).increment(amount)
    }

    /**
     * 计时并记录异常
     *
     * @param name 指标名
     * @param tags 标签
     * @param outcome 异常标签
     * @param block 代码块
     * @return 结果
     */
    suspend fun <T> time(
        name: String,
        vararg tags: Pair<String, String>,
        outcome: (Throwable) -> String,
        block: suspend () -> T
    ): T {
        val sample = Timer.start(registry)
        return runCatching { block() }.fold(
            onSuccess = { result ->
                sample.stop(timer(name, tags, "success"))
                result
            },
            onFailure = { e ->
                val failure = if (e is CancellationException)
                    "cancelled"
                else
                    outcome(e)
                sample.stop(timer(name, tags, failure))
                throw e
            }
        )
    }

    /**
     * 计时并记录异常
     *
     * @param name 指标名
     * @param tags 标签
     * @param block 代码块
     * @return 结果
     */
    suspend fun <T> time(
        name: String,
        vararg tags: Pair<String, String>,
        block: suspend () -> T
    ): T = time(name, *tags, outcome = ::outcome, block = block)

    private fun timer(
        name: String,
        tags: Array<out Pair<String, String>>,
        outcome: String
    ) = Timer.builder(name)
        .tag("outcome", outcome)
        .apply {
            tags.forEach { tag -> tag(tag.first, tag.second) }
        }
        .publishPercentileHistogram(true)
        .register(registry)

    /**
     * 抓取 Prometheus 文本
     *
     * @return 当前进程的全部指标
     */
    fun scrape(): String = registry.scrape()

    private fun outcome(e: Throwable) =
        if (e is CancellationException) "cancelled" else "error"
}