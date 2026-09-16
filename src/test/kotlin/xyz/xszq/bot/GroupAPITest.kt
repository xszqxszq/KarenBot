package xyz.xszq.bot

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import xyz.xszq.bot.config.BotConfig
import xyz.xszq.bot.config.DatabaseConfig
import xyz.xszq.bot.service.OpenAPI
import xyz.xszq.bot.service.WordFilter
import xyz.xszq.bot.util.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GroupAPITest {
    @Test
    fun shouldFetchGroupInfo() = runTest {
        val paths = mutableListOf<String>()
        val api = createApi(
            response = GROUP_INFO,
            onRequest = { paths += it }
        )

        val info = api.getGroupInfo("group")

        assertEquals(listOf("/v2/groups/group/info"), paths)
        assertEquals("读书分享会", info ?.name)
        assertEquals("每周共读一本好书", info ?.description)
        assertEquals(listOf("阅读", "文学"), info ?.tags)
        assertEquals(256, info ?.memberCount)
    }

    @Test
    fun shouldFetchBotState() = runTest {
        val paths = mutableListOf<String>()
        val api = createApi(
            response = BOT_STATE,
            onRequest = { paths += it }
        )

        val state = api.getBotState("group")

        assertEquals(listOf("/v2/groups/group/bot_state"), paths)
        assertEquals("all", state ?.recvMsgSetting)
        assertEquals(true, state ?.allowPush)
        assertEquals("admin", state ?.role)
    }

    @Test
    fun shouldListJoinRequests() = runTest {
        val api = createApi(
            response = """
                {
                  "list": [
                    {
                      "join_request_id": "request-1",
                      "member_openid": "member-1",
                      "username": "小光",
                      "apply_source": "self_apply",
                      "bot": false,
                      "verify_info": {
                        "method": "verify_message",
                        "verify_message": "几款看看",
                        "review_qa_list": []
                      }
                    }
                  ],
                  "next_cursor": "1785767153250497"
                }
            """.trimIndent()
        )

        val result = api.getJoinRequestList("group")

        val request = result ?.list ?.single()
        assertEquals("request-1", request ?.id)
        assertEquals("小光", request ?.username)
        assertEquals("几款看看", request ?.verifyInfo ?.verifyMessage)
        assertEquals("1785767153250497", result ?.nextCursor)
    }

    @Test
    fun shouldFetchRestrictChatSetting() = runTest {
        val api = createApi(
            response = """
                {
                  "global_rule": {
                    "mode": "recurring",
                    "schedule_rules": [],
                    "recurring_rules": [
                      {
                        "task_id": "task-1",
                        "weekdays": [1, 2],
                        "start_time": "13:05",
                        "end_time": "14:05",
                        "enabled": true
                      }
                    ]
                  },
                  "members": [
                    {
                      "member_openid": "member-1",
                      "mute_expire_at": "2026-08-05T11:23:04+08:00",
                      "username": "小光"
                    }
                  ]
                }
            """.trimIndent()
        )

        val setting = api.getRestrictChatSetting("group")

        val rule = setting ?.globalRule ?.recurringRules ?.single()
        assertEquals("recurring", setting ?.globalRule ?.mode)
        assertEquals(listOf(1, 2), rule ?.weekdays)
        assertEquals("member-1", setting ?.members ?.single() ?.member)
    }

    @Test
    fun shouldReturnNullWhenForbidden() = runTest {
        val api = createApi(
            response = """{"message":"应用无接口访问权限","code":11253,"err_code":40012010}""",
            status = HttpStatusCode.BadRequest
        )

        assertNull(api.getGroupInfo("group"))
    }

    private fun createApi(
        response: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        onRequest: (String) -> Unit = {}
    ): OpenAPI {
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            if (path.endsWith("getAppAccessToken"))
                jsonResponse(ACCESS_TOKEN)
            else {
                onRequest(path)
                jsonResponse(response, status)
            }
        }
        return OpenAPI(
            config = BotConfig(
                appId = "app-id",
                clientSecret = "secret",
                database = DatabaseConfig(
                    "jdbc:h2:mem:group-api", "org.h2.Driver", "", ""
                )
            ),
            filter = WordFilter(emptyList()),
            client = HttpClient(engine) {
                install(ContentNegotiation) {
                    json(json)
                }
            }
        )
    }

    private fun MockRequestHandleScope.jsonResponse(
        content: String,
        status: HttpStatusCode = HttpStatusCode.OK
    ) = respond(
        content = content,
        status = status,
        headers = headersOf(
            HttpHeaders.ContentType,
            ContentType.Application.Json.toString()
        )
    )

    private companion object {
        const val ACCESS_TOKEN = """{"access_token":"token","expires_in":"3600"}"""

        const val GROUP_INFO = """{"group_openid":"group","group_name":"读书分享会","group_finger_memo":"每周共读一本好书","group_class_text":"文化","group_tags":["阅读","文学"],"group_member_num":256}"""

        const val BOT_STATE = """{"member_openid":"bot-1","joined_at":"2024-05-09T20:36:28+08:00","allow_proactive_msg":true,"recv_msg_setting":"all","member_role":"admin"}"""
    }
}