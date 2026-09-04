package xyz.xszq.bot.chunithm

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.xszq.bot.chunithm.database.ChunithmMusicAliasesTable
import xyz.xszq.bot.chunithm.database.ChunithmMusicAliasesVoteTable
import xyz.xszq.bot.chunithm.database.MaimaiSettingsTable
import xyz.xszq.bot.chunithm.database.ProberBindTable
import xyz.xszq.bot.chunithm.database.QQBindTable
import kotlin.test.BeforeTest

abstract class ChunithmDatabaseTest {
    companion object {
        private var connected = false
        private lateinit var connectedDatabase: Database

        private fun connectIfNeeded(): Database {
            if (connected)
                return connectedDatabase
            connectedDatabase = Database.connect(
                url = "jdbc:h2:mem:chunithm;MODE=MySQL;DB_CLOSE_DELAY=-1",
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
                QQBindTable, ProberBindTable, MaimaiSettingsTable,
                ChunithmMusicAliasesTable, ChunithmMusicAliasesVoteTable
            )
            SchemaUtils.create(
                QQBindTable, ProberBindTable, MaimaiSettingsTable,
                ChunithmMusicAliasesTable, ChunithmMusicAliasesVoteTable
            )
        }
    }
}