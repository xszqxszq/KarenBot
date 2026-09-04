package xyz.xszq.bot.message

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import xyz.xszq.bot.util.newTempFile

class FileManagerTest {
    @Test
    fun shouldDeleteExpiredFiles() = runTest {
        var now = 0L
        val manager = FileManager(expiresAfter = 10L, now = { now })

        val file1 = newTempFile(suffix = ".txt")
        val file2 = newTempFile(suffix = ".txt")
        file1.writeString("old")
        file2.writeString("new")

        manager.addFile(file1)
        assertTrue(file1.exists())

        now = 11L
        manager.addFile(file2)

        assertFalse(file1.exists())
        assertTrue(file2.exists())

        file2.delete()
    }
}