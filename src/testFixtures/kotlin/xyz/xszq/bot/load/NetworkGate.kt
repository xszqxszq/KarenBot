package xyz.xszq.bot.load

import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.security.Permission
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 外部网络请求拦截
 */
@Suppress("DEPRECATION", "removal")
class NetworkGate {
    private val attempts = ConcurrentLinkedQueue<String>()

    /**
     * 被拦截的外部地址
     */
    val blocked: List<String> get() = attempts.toList()

    /**
     * 启用拦截
     */
    fun install() {
        System.setSecurityManager(manager)
    }

    /**
     * 卸载拦截
     */
    fun uninstall() {
        System.setSecurityManager(null)
    }

    /**
     * 探测外部地址的连接是否被拦截
     *
     * @param host 目标主机
     * @param port 目标端口
     * @return 是否抛出 SecurityException
     */
    fun probeConnect(host: String, port: Int): Boolean = runCatching {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), PROBE_TIMEOUT_MS)
        }
    }.exceptionOrNull() is SecurityException

    /**
     * 探测外部域名解析是否被拦截
     *
     * @param host 目标主机
     * @return 是否抛出 SecurityException
     */
    fun probeResolve(host: String): Boolean = runCatching {
        InetAddress.getByName(host)
    }.exceptionOrNull() is SecurityException

    private val manager = object : SecurityManager() {
        override fun checkPermission(perm: Permission) {}

        override fun checkPermission(perm: Permission, context: Any?) {}

        override fun checkConnect(host: String ?, port: Int) = denyExternal(host, port)

        override fun checkConnect(host: String ?, port: Int, context: Any?) =
            denyExternal(host, port)

        private fun denyExternal(host: String ?, port: Int) {
            if (host == null || isLocal(host, port))
                return
            val target = if (port < 0) "解析 $host" else "连接 $host:$port"
            attempts += target
            throw SecurityException("压测期间禁止访问外部地址，$target")
        }
    }

    private fun isLocal(host: String, port: Int) =
        isLoopback(host) || port < 0 && isUnspecified(host)

    private fun isLoopback(host: String) =
        host == "localhost" || host == "::1" || host == "0:0:0:0:0:0:0:1" ||
            host.startsWith("127.")

    private fun isUnspecified(host: String) =
        host == "0.0.0.0" || host == "::" || host == "0:0:0:0:0:0:0:0"

    companion object {
        /**
         * 连接对照使用的保留地址
         */
        const val CONNECT_PROBE_HOST = "192.0.2.1"

        const val CONNECT_PROBE_PORT = 443

        /**
         * 解析对照使用的保留域名
         */
        const val RESOLVE_PROBE_HOST = "probe.invalid"

        private const val PROBE_TIMEOUT_MS = 3000
    }
}
