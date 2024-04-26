package xyz.xszq.karenbot

import kotlinx.serialization.Serializable
import xyz.xszq.KarenBot

@Serializable
class DatabaseConfigData(
    val url: String,
    val username: String,
    val password: String
)

object DatabaseConfig: SafeYamlConfig<DatabaseConfigData>(
    KarenBot,
    "database",
    DatabaseConfigData(
        "jdbc:mariadb://localhost:3306/karenbot",
        "",
        ""
    )
)