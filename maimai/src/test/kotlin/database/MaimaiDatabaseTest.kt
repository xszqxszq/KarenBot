package xyz.xszq.bot.database

import kotlin.test.BeforeTest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

abstract class MaimaiDatabaseTest {
    companion object {
        private var connected = false

        private fun connectIfNeeded() {
            if (connected)
                return
            Database.connect(
                url = "jdbc:h2:mem:maimai;MODE=MySQL;DB_CLOSE_DELAY=-1",
                driver = "org.h2.Driver",
            )
            connected = true
        }
    }

    @BeforeTest
    fun resetDatabase() {
        connectIfNeeded()
        transaction {
            SchemaUtils.drop(GuessGameTable, MusicAliasesVoteTable, ArcadeTable, ArcadeGroupBindTable, ArcadeGroupTable, QQBindTable, MaimaiSettingsTable, MusicAliasesTable)
            SchemaUtils.create(ArcadeGroupTable, ArcadeGroupBindTable, ArcadeTable, QQBindTable, MaimaiSettingsTable, MusicAliasesTable, MusicAliasesVoteTable, GuessGameTable)
        }
    }
}
