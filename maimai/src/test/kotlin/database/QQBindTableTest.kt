package xyz.xszq.bot.database

import kotlinx.coroutines.test.runTest
import xyz.xszq.bot.controller.ControllerTest.Companion.newOpenId
import kotlin.random.Random
import kotlin.random.nextLong
import kotlin.test.Test
import kotlin.test.assertEquals

class QQBindTableTest: MaimaiDatabaseTest() {
    @Test
    fun testBind() = runTest {
        val testUserId = newOpenId()
        QQBindTable.update(testUserId, 1234567890L)
        assertEquals(1234567890L, QQBindTable[testUserId])

        val testQQ = Random.nextLong(10000000L, 10000000000L)
        QQBindTable.update(testUserId, testQQ)
        assertEquals(testQQ, QQBindTable[testUserId])
    }
}
