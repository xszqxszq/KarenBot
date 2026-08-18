package xyz.xszq.bot

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.mockk.mockk
import korlibs.io.file.VfsFile
import kotlinx.coroutines.test.runTest
import kotlin.test.*

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