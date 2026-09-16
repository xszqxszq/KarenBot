package xyz.xszq.bot.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import xyz.xszq.bot.config.DatabaseConfig
import xyz.xszq.bot.util.DB_PARALLELISM

/**
 * 数据库连接池
 */
object DatabasePool {
    private const val MARIADB_PREFIX = "jdbc:mariadb:"
    private const val CONNECT_TIMEOUT_MS = 10_000L

    /**
     * 连接数据库
     *
     * @param config 数据库连接配置
     * @return 数据库连接
     */
    fun connect(config: DatabaseConfig): Database {
        if (!config.url.startsWith(MARIADB_PREFIX))
            return Database.connect(
                url = config.url,
                driver = config.driver,
                user = config.username,
                password = config.password
            )
        return Database.connect(pool(config))
    }

    private fun pool(config: DatabaseConfig) = HikariDataSource(HikariConfig().apply {
        jdbcUrl = config.url
        driverClassName = config.driver
        username = config.username
        password = config.password
        poolName = "KarenBot"
        maximumPoolSize = DB_PARALLELISM
        minimumIdle = 1
        connectionTimeout = CONNECT_TIMEOUT_MS
    })
}