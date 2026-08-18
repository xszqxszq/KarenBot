package xyz.xszq.bot

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.jetbrains.exposed.sql.Database
import xyz.xszq.bot.event.Event
import xyz.xszq.bot.event.GroupMessageEvent
import xyz.xszq.bot.event.InteractionEvent
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.message.MessageChain
import xyz.xszq.bot.message.PlainText
import xyz.xszq.bot.payload.markdown.MarkdownData
import xyz.xszq.bot.subscribe.SubscribeManager

@OptIn(ExperimentalCoroutinesApi::class)
class BotSandbox(
    private val scope: TestScope,
    cos: TencentCos = mockk(relaxed = true),
    database: Database = mockk(relaxed = true),
) {
    val replies = java.util.Collections.synchronizedList(mutableListOf<MessageEvent>())
    private val replyMap = java.util.concurrent.ConcurrentHashMap<String, MessageEvent>()

    private val dispatcher = StandardTestDispatcher(scope.testScheduler)
    lateinit var pluginLoader: PluginLoader

    var cleanup: suspend () -> Unit = {}

    init {
        val api = mockk<OpenAPI>(relaxed = true).apply {
            coEvery {
                uploadC2CFile(any(), any(), any(), any())
            } returns xyz.xszq.bot.payload.FileResponse("file-uuid", "file-info", 0)
            coEvery {
                uploadGroupFile(any(), any(), any(), any())
            } returns xyz.xszq.bot.payload.FileResponse("file-uuid", "file-info", 0)
            coEvery {
                sendC2CMessage(any(), any<String>(), any(), any(), any(), any(), any(), any(), any())
            } coAnswers {
                val eid = arg<String?>(5) ?: ""
                val mid = arg<String?>(6) ?: ""
                val text = arg<MarkdownData?>(3)?.let { it.content ?: secondArg<String>() } ?: secondArg<String>()
                val msg = MessageEvent(
                    bot = pluginLoader.bot, eventId = eid, id = mid,
                    message = MessageChain(PlainText(text)),
                    sender = User(pluginLoader.bot, firstArg()),
                )
                replies += msg
                replyMap[eid] = msg
                true
            }
            coEvery {
                sendGroupMessage(any(), any<String>(), any(), any(), any(), any(), any(), any(), any())
            } coAnswers {
                val eid = arg<String?>(5) ?: ""
                val mid = arg<String?>(6) ?: ""
                val text = arg<MarkdownData?>(3)?.let { it.content ?: secondArg<String>() } ?: secondArg<String>()
                val msg = GroupMessageEvent(
                    bot = pluginLoader.bot, eventId = eid, id = mid,
                    message = MessageChain(PlainText(text)),
                    sender = Member(pluginLoader.bot, ""),
                    group = Group(pluginLoader.bot, firstArg()),
                )
                replies += msg
                replyMap[eid] = msg
                true
            }
        }
        pluginLoader = PluginLoader(api,
            cos,
            database,
            subscribes = SubscribeManager(dispatcher))
    }

    fun replyFor(event: Event) = replyMap[event.eventId]

    fun awaitReply(event: Event, timeoutMs: Long = 30_000): MessageEvent? {
        val deadline = System.nanoTime() + timeoutMs * 1_000_000L
        var reply: MessageEvent? = null
        while (reply == null && System.nanoTime() < deadline) {
            reply = replyFor(event)
            if (reply == null)
                Thread.sleep(100)
        }
        return reply
    }

    fun clear() {
        replies.clear()
        replyMap.clear()
    }

    suspend fun advanceIdle() = scope.advanceUntilIdle()

    fun user(id: String = "test-user") = UserActor(id)

    fun group(id: String = "test-group") = GroupActor(id)

    suspend fun tapButton(button: String, data: String = "", id: String = "test-user"): InteractionEvent {
        val globalSeq = replySeq++
        val event = InteractionEvent(
            bot = pluginLoader.bot,
            eventId = "$id-$globalSeq",
            id = "$id-i-$globalSeq",
            data = data,
            button = button,
            sender = User(pluginLoader.bot, id),
        )
        pluginLoader.manualTrigger(event)
        scope.advanceUntilIdle()
        return event
    }

    inner class UserActor(private val id: String) {
        private var seq = 0

        suspend infix fun says(text: String): MessageEvent {
            val globalSeq = replySeq++
            val event = MessageEvent(
                bot = pluginLoader.bot,
                eventId = "$id-$globalSeq",
                id = "$id-m-$globalSeq",
                message = MessageChain(PlainText(text)),
                sender = User(pluginLoader.bot, id),
            )
            pluginLoader.manualTrigger(event)
            scope.advanceUntilIdle()
            return event
        }
    }

    inner class GroupActor(private val id: String) {
        private var seq = 0

        suspend infix fun says(text: String): MessageEvent {
            val globalSeq = replySeq++
            val event = GroupMessageEvent(
                bot = pluginLoader.bot,
                eventId = "$id-$globalSeq",
                id = "$id-m-$globalSeq",
                message = MessageChain(PlainText(text)),
                sender = Member(pluginLoader.bot, "test-user"),
                group = Group(pluginLoader.bot, id),
            )
            pluginLoader.manualTrigger(event)
            scope.advanceUntilIdle()
            return event
        }
    }

    private var replySeq = 0
}
