package xyz.xszq.bot.util

/**
 * 配额限流
 *
 * @property perMinute 每分钟允许的次数
 * @property now 当前时间（毫秒）
 */
class RateLimiter(
    private val perMinute: Int,
    private val now: () -> Long = System::currentTimeMillis
) {
    private val stamps = ArrayDeque<Long>()

    /**
     * 尝试取得一次调用配额
     *
     * @return 是否取得配额
     */
    @Synchronized
    fun tryAcquire(): Boolean {
        val current = now()
        while (stamps.isNotEmpty() && current - stamps.first() >= WINDOW)
            stamps.removeFirst()
        if (stamps.size >= perMinute)
            return false
        stamps.addLast(current)
        return true
    }

    private companion object {
        const val WINDOW = 60_000L
    }
}