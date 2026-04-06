package xyz.xszq.bot

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import xyz.xszq.bot.config.LLMConfig

class TextTest {
    @Test
    fun shouldReturnResultAfterAudit() = runTest {
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
    fun shouldReturnFalseWhenBadRequestOccurred() = runTest {
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
    fun shouldReturnTrueWhenServerGlitches() = runTest {
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
        return Text().also {
            it.client = HttpClient(engine) {
                install(ContentNegotiation) {
                    json(json)
                }
            }
            it.llmConfig = LLMConfig(
                apikey = "apikey",
                url = "https://example.com",
                model = "test",
                system = "prompt",
                temperature = 0.1,
            )
        }
    }

    private companion object {
        val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }
}
