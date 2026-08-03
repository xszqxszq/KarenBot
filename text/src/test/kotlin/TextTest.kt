package xyz.xszq.bot

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import xyz.xszq.bot.config.TextConfig
import xyz.xszq.bot.llm.LLMClient
import xyz.xszq.bot.llm.LLMConfig
import xyz.xszq.bot.llm.LLMModelConfig
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextTest {
    @Test
    fun testAudit() = runTest {
        val text = textWithClient(
            MockEngine { request ->
                respond(
                    content = """{"id":"1","created":1,"model":"test","choices":[{"index":0,"message":{"role":"assistant","content":"false"},"finish_reason":"stop"}]}""",
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders
                )
            }
        )

        val result = text.audit("Test")
        assertFalse(result)
    }

    @Test
    fun testHardReject() = runTest {
        val text = textWithClient(
            MockEngine {
                respond(
                    content = "",
                    status = HttpStatusCode.BadRequest,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Plain.toString())
                )
            }
        )

        assertFalse(text.audit("Test"))
    }

    @Test
    fun testAuditServerGlitches() = runTest {
        val text = textWithClient(
            MockEngine {
                respond(
                    content = """{"error":"unknown"}""",
                    status = HttpStatusCode.ServiceUnavailable,
                    headers = jsonHeaders
                )
            }
        )

        assertTrue(text.audit("Test"))
    }

    private fun textWithClient(engine: MockEngine): Text {
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
        return Text().also {
            it.textConfig = TextConfig(system = "prompt", presets = emptyMap())
            it.pluginLoader = mockPluginLoader
        }
    }

    private companion object {
        val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }
}
