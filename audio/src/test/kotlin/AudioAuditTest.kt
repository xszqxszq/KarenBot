package xyz.xszq.bot

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.mockk.every
import io.mockk.mockk
import korlibs.io.file.VfsFile
import kotlinx.coroutines.test.runTest
import xyz.xszq.bot.audio.Audio
import xyz.xszq.bot.llm.LLMClient
import xyz.xszq.bot.llm.LLMConfig
import xyz.xszq.bot.llm.LLMModelConfig
import xyz.xszq.bot.payload.llm.LLMRequest
import xyz.xszq.bot.payload.llm.MessageContentMulti
import xyz.xszq.bot.util.useTempFile
import xyz.xszq.bot.util.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AudioAuditTest {
    @Test
    fun testAuditUsesInputAsUserMessage() = runTest {
        var requestBody: String ?= null
        val audio = audioWithClient(
            MockEngine {
                requestBody = (it.body as? TextContent)?.text
                respond(
                    content = """
                        {"id":"1","created":1,"model":"test","choices":[{"index":0,"message":{"role":"assistant","content":"true"},"finish_reason":"stop"}]}
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders
                )
            }
        )

        assertTrue(audio.audit("大家好"))
        val request = json.decodeFromString<LLMRequest>(requestBody ?: "")
        val system = request.messages.first { it.role == "system" }.contentAsText() ?: ""
        val user = request.messages.first { it.role == "user" }.contentAsText()

        assertTrue(system.contains("仅政治敏感"))
        assertFalse(system.contains("色情"))
        assertEquals("大家好", user)
    }

    @Test
    fun testAuditRejectsGuardrailAnswer() = runTest {
        val audio = audioWithClient(
            MockEngine {
                respond(
                    content = """
                        {"id":"1","created":1,"model":"test","choices":[{"index":0,"message":{"role":"assistant","content":"您的问题我无法回答"},"finish_reason":"stop"}]}
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders
                )
            }
        )

        assertFalse(audio.audit("大家好"))
    }

    @Test
    fun testAudioAuditSendsWavContentPart() = runTest {
        var requestBody: String ?= null
        val audio = audioWithClient(
            MockEngine {
                requestBody = (it.body as? TextContent)?.text
                respond(
                    content = """
                        {"id":"1","created":1,"model":"test","choices":[{"index":0,"message":{"role":"assistant","content":"true"},"finish_reason":"stop"}]}
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders
                )
            }
        )

        useTempFile(suffix = ".wav") { wav: VfsFile ->
            wav.writeBytes(byteArrayOf(1, 2, 3, 4))
            assertTrue(audio.audit(wav))
        }

        val request = json.decodeFromString<LLMRequest>(requestBody ?: "")
        val system = request.messages.first { it.role == "system" }.contentAsText() ?: ""
        val user = request.messages.first { it.role == "user" }.content
        val parts = (user as MessageContentMulti).parts
        val audioPart = parts.first { it.type == "input_audio" }.inputAudio

        assertEquals("AQIDBA==", audioPart ?.data)
        assertEquals("wav", audioPart ?.format)
    }

    private fun audioWithClient(engine: MockEngine): Audio {
        val llmClient = LLMClient(
            LLMConfig(
                models = mapOf(
                    "audit" to LLMModelConfig(
                        apikey = "apikey",
                        url = "https://example.com",
                        model = "test",
                        temperature = 0.1,
                    )
                )
            ),
            HttpClient(engine) {
                install(ContentNegotiation) {
                    json(json)
                }
            }
        )
        val mockPluginLoader = mockk<PluginLoader>()
        every { mockPluginLoader.llmClient } returns llmClient
        return Audio().also {
            it.pluginLoader = mockPluginLoader
        }
    }

    private companion object {
        val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }
}