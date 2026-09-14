package xyz.xszq.bot.database

import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GroupMemberTableTest {
    private val database = Database.connect(
        url = "jdbc:h2:mem:group-members;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver",
        user = "sa",
        password = ""
    )

    @BeforeTest
    fun setUp() {
        transaction(database) {
            SchemaUtils.drop(GroupMemberTable)
            SchemaUtils.create(GroupMemberTable)
        }
    }

    @Test
    fun shouldAddRelation() = runTest {
        GroupMemberTable.add(database, "group-1", "member-1")

        assertEquals(listOf("group-1" to "member-1"), relations())
    }

    @Test
    fun shouldIgnoreDuplicateRelation() = runTest {
        GroupMemberTable.add(database, "group-1", "member-1")
        GroupMemberTable.add(database, "group-1", "member-1")

        assertEquals(listOf("group-1" to "member-1"), relations())
    }

    @Test
    fun shouldRemoveRelation() = runTest {
        GroupMemberTable.add(database, "group-1", "member-1")
        GroupMemberTable.add(database, "group-1", "member-2")
        GroupMemberTable.remove(database, "group-1", "member-1")

        assertEquals(listOf("group-1" to "member-2"), relations())
    }

    @Test
    fun shouldAllowSameMemberInManyGroups() = runTest {
        GroupMemberTable.add(database, "group-1", "member-1")
        GroupMemberTable.add(database, "group-2", "member-1")
        GroupMemberTable.remove(database, "group-1", "member-1")

        assertEquals(listOf("group-2" to "member-1"), relations())
    }

    private suspend fun relations(): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        GroupMemberTable.forEach(database) { group, member ->
            result += group to member
        }
        return result.sortedBy { it.first + it.second }
    }
}