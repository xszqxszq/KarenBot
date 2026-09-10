package xyz.xszq.bot.load

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * 压测指标
 *
 * @property name 指标名称
 */
class LoadMetrics(
    private val name: String
) {
    private val logger = KotlinLogging.logger {}
    private val started = ConcurrentHashMap<String, Sample>()
    private val latencies = ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>>()
    private val acks = ConcurrentLinkedQueue<Long>()
    private val failures = ConcurrentHashMap<String, String>()
    private val inFlight = AtomicInteger(0)
    private val peakInFlight = AtomicInteger(0)

    /**
     * 记录事件提交
     *
     * @param eventId 事件 ID
     * @param scenario 场景名
     */
    fun submit(eventId: String, scenario: String) {
        started[eventId] = Sample(scenario, System.nanoTime())
        val current = inFlight.incrementAndGet()
        peakInFlight.updateAndGet { peak -> maxOf(peak, current) }
    }

    /**
     * 记录 Webhook 请求的往返耗时
     *
     * @param nanos 往返耗时（纳秒）
     */
    fun ack(nanos: Long) {
        acks += nanos
    }

    /**
     * 记录事件处理完成
     *
     * @param eventId 事件 ID
     */
    fun complete(eventId: String) {
        val sample = started.remove(eventId) ?: return
        inFlight.decrementAndGet()
        latencies.computeIfAbsent(sample.scenario) { ConcurrentLinkedQueue() } +=
            System.nanoTime() - sample.atNanos
    }

    /**
     * 记录事件失败
     *
     * @param eventId 事件 ID
     * @param reason 失败原因
     */
    fun fail(eventId: String, reason: String) {
        started.remove(eventId) ?.let { inFlight.decrementAndGet() }
        failures[eventId] = reason
    }

    /**
     * 清空全部指标
     */
    fun reset() {
        started.clear()
        latencies.clear()
        acks.clear()
        failures.clear()
        inFlight.set(0)
        peakInFlight.set(0)
    }

    /**
     * 输出压测报告
     *
     * @param elapsedMs 压测总耗时（毫秒）
     */
    fun report(elapsedMs: Long) {
        val elapsed = elapsedMs.coerceAtLeast(1)
        val total = latencies.values.sumOf { it.size }
        logger.info { "[压测] $name 完成 $total 个事件，耗时 ${elapsed}ms" }
        logger.info { "[压测] 吞吐 ${total * 1000 / elapsed} 事件每秒，峰值在途 ${peakInFlight.get()}，失败 ${failures.size}" }
        logger.info { "[压测] ACK ${percentiles(acks.toList())}" }
        latencies.entries.sortedByDescending { it.value.size }.forEach { (scenario, values) ->
            logger.info { "[压测] $scenario ${percentiles(values.toList())}" }
        }
        failures.values.groupingBy { it }.eachCount().forEach { (reason, count) ->
            logger.warn { "[压测] 失败 $count 次，$reason" }
        }
    }

    private fun percentiles(values: List<Long>): String {
        if (values.isEmpty())
            return "无样本"
        val sorted = values.sorted()
        return buildString {
            append("n=${sorted.size}")
            listOf(50, 95, 99).forEach { percentile ->
                append(" p$percentile=${sorted[percentile(sorted, percentile)] / 1_000_000}ms")
            }
            append(" max=${sorted.last() / 1_000_000}ms")
        }
    }

    private fun percentile(sorted: List<Long>, percentile: Int) =
        (sorted.size * percentile / 100).coerceAtMost(sorted.size - 1)

    /**
     * 待统计的单个事件
     */
    private class Sample(
        val scenario: String,
        val atNanos: Long
    )
}
