package xyz.xszq.bot

import io.mockk.mockk
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
import korlibs.io.file.VfsFile
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileTest {
    @Test
    fun shouldDeleteFileAfterUse() = runTest {
        val file = newTempFile(suffix = ".txt")
        file.writeString("Test")

        val result = file.use {
            assertTrue(it.exists())
            it.readString()
        }

        assertEquals("Test", result)
        assertFalse(file.exists())
    }

    @Test
    fun shouldDeleteTempFileAfterUseTempFile() = runTest {
        lateinit var tempFile: VfsFile

        val result = useTempFile(suffix = ".txt") { file ->
            tempFile = file
            file.writeString("Test")
            file.readString()
        }

        assertEquals("Test", result)
        assertFalse(tempFile.exists())
    }

    @Test
    fun shouldDownloadFileWithInjectedClient() = runTest {
        val client = HttpClient(MockEngine {
            respond(
                content = ByteReadChannel("Test"),
                status = HttpStatusCode.OK
            )
        })

        val file = downloadFile("https://test.com/file.txt", "file.txt", mockk(relaxed = true), client)

        assertEquals("Test", file?.readString())
        assertTrue(file?.exists() == true)
        file.delete()
        client.close()
    }

    @Test
    fun shouldReturnNullWhenDownloadFails() = runTest {
        val client = HttpClient(MockEngine {
            throw IllegalStateException("boom")
        })

        val file = downloadFile("https://test.com/file.txt", "file.txt", mockk(relaxed = true), client)

        assertNull(file)
        client.close()
    }
}
