package xyz.xszq.otomadbot

import kotlinx.serialization.Serializable
import xyz.xszq.OtomadBotCore

@Serializable
class DatabaseConfigData(
    val url: String,
    val username: String,
    val password: String
)

object DatabaseConfig: SafeYamlConfig<DatabaseConfigData>(
    OtomadBotCore,
    "database",
    DatabaseConfigData(
        "jdbc:mariadb://localhost:3306/karenbot",
        "",
        ""
    )
)