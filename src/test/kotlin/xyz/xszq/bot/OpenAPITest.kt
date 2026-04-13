package xyz.xszq.bot

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.readRemaining
import xyz.xszq.bot.config.BotConfig
import xyz.xszq.bot.config.DatabaseConfig
import xyz.xszq.bot.payload.MsgType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpenAPITest {
    @Test
    fun shouldReuseAccessTokenBeforeExpiry() = kotlinx.coroutines.test.runTest {
        var tokenRequests = 0
        val authHeaders = mutableListOf<String>()
        val engine = MockEngine { request ->
            when (request.url.toString()) {
                OpenAPI.DEFAULT_ACCESS_TOKEN_URL -> {
                    tokenRequests += 1
                    respond(
                        content = """{"access_token":"token-1","expires_in":3600}""",
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders
                    )
                }

                "${OpenAPI.DEFAULT_SERVER}/v2/groups/group/messages" -> {
                    authHeaders += request.headers[HttpHeaders.Authorization].orEmpty()
                    respond(
                        content = """{"id":"message","timestamp":"2025-01-01T00:00:00Z"}""",
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders
                    )
                }

                else -> error("Unexpected url ${request.url}")
            }
        }
        val api = createApi(engine)

        assertTrue(api.sendGroupMessage("group", "hello", MsgType.TEXT, eventId = "event", msgId = "1"))
        assertTrue(api.sendGroupMessage("group", "hello again", MsgType.TEXT, eventId = "event", msgId = "2"))

        assertEquals(1, tokenRequests)
        assertEquals(listOf("QQBot token-1", "QQBot token-1"), authHeaders)
    }

    @Test
    fun shouldRefreshExpiredAccessToken() = kotlinx.coroutines.test.runTest {
        var tokenRequests = 0
        var now = 0L
        val authHeaders = mutableListOf<String>()
        val engine = MockEngine { request ->
            when (request.url.toString()) {
                OpenAPI.DEFAULT_ACCESS_TOKEN_URL -> {
                    tokenRequests += 1
                    respond(
                        content = """{"access_token":"token-$tokenRequests","expires_in":1}""",
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders
                    )
                }

                "${OpenAPI.DEFAULT_SERVER}/v2/groups/group/messages" -> {
                    authHeaders += request.headers[HttpHeaders.Authorization].orEmpty()
                    respond(
                        content = """{"id":"message","timestamp":"2025-01-01T00:00:00Z"}""",
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders
                    )
                }

                else -> error("Unexpected url ${request.url}")
            }
        }
        val api = createApi(engine, now = { now })

        assertTrue(api.sendGroupMessage("group", "hello", MsgType.TEXT, eventId = "event", msgId = "1"))
        now = 2_000L
        assertTrue(api.sendGroupMessage("group", "hello again", MsgType.TEXT, eventId = "event", msgId = "2"))

        assertEquals(2, tokenRequests)
        assertEquals(listOf("QQBot token-1", "QQBot token-2"), authHeaders)
    }

    @Test
    fun shouldFilterContentBeforeSending() = kotlinx.coroutines.test.runTest {
        var requestBody = ""
        val engine = MockEngine { request ->
            when (request.url.toString()) {
                OpenAPI.DEFAULT_ACCESS_TOKEN_URL -> respond(
                    content = """{"access_token":"token","expires_in":3600}""",
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders
                )

                "${OpenAPI.DEFAULT_SERVER}/v2/groups/group/messages" -> {
                    requestBody = request.bodyText()
                    respond(
                        content = """{"id":"message","timestamp":"2025-01-01T00:00:00Z"}""",
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders
                    )
                }

                else -> error("Unexpected url ${request.url}")
            }
        }
        val api = createApi(engine, filter = WordFilter(listOf("bad")))

        assertTrue(api.sendGroupMessage("group", "bad content", MsgType.TEXT, eventId = "event", msgId = "1"))
        assertTrue("\"content\":\"*** content\"" in requestBody)
    }

    private fun createApi(
        engine: MockEngine,
        filter: WordFilter = WordFilter(emptyList()),
        now: () -> Long = { 0L }
    ) = OpenAPI(
        config = BotConfig(
            appId = "app-id",
            clientSecret = "secret",
            database = DatabaseConfig("jdbc:h2:mem:test", "org.h2.Driver", "", "")
        ),
        filter = filter,
        client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }
        },
        now = now
    )

    private suspend fun HttpRequestData.bodyText(): String = when (val body = this.body) {
        is OutgoingContent.ByteArrayContent -> body.bytes().decodeToString()
        is OutgoingContent.ReadChannelContent -> body.readFrom().readRemaining().readText()
        is OutgoingContent.WriteChannelContent -> {
            val channel = ByteChannel(autoFlush = true)
            body.writeTo(channel)
            channel.close()
            channel.readRemaining().readText()
        }

        is OutgoingContent.NoContent -> ""
        else -> error("Unsupported request body type: ${body::class}")
    }

    private companion object {
        val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }
}