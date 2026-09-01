package xyz.xszq.bot.maimai

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.xszq.bot.maimai.database.*
import kotlin.test.BeforeTest

abstract class MaimaiDatabaseTest {
    companion object {
        private var connected = false
        private lateinit var connectedDatabase: Database

        private fun connectIfNeeded(): Database {
            if (connected)
                return connectedDatabase
            connectedDatabase = Database.connect(
                url = "jdbc:h2:mem:maimai;MODE=MySQL;DB_CLOSE_DELAY=-1",
                driver = "org.h2.Driver",
            )
            connected = true
            return connectedDatabase
        }
    }

    protected val database: Database
        get() = connectIfNeeded()

    @BeforeTest
    fun resetDatabase() {
        connectIfNeeded()
        transaction {
            SchemaUtils.drop(
                GuessGameTable,
                MaimaiMusicAliasesVoteTable,
                ArcadeTable,
                ArcadeGroupBindTable,
                ArcadeGroupTable,
                QQBindTable,
                ProberBindTable,
                MaimaiSettingsTable,
                MaimaiMusicAliasesTable,
                DivingFishBindTable
            )
            SchemaUtils.create(
                ArcadeGroupTable,
                ArcadeGroupBindTable,
                ArcadeTable,
                QQBindTable,
                ProberBindTable,
                MaimaiSettingsTable,
                MaimaiMusicAliasesTable,
                MaimaiMusicAliasesVoteTable,
                GuessGameTable,
                DivingFishBindTable
            )
        }
    }
}