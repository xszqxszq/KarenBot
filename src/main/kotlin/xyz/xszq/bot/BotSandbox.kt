package xyz.xszq.bot

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import xyz.xszq.bot.event.GroupMessageEvent
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.message.MessageChain
import xyz.xszq.bot.message.PlainText
import xyz.xszq.bot.subscribe.SubscribeManager

@OptIn(ExperimentalCoroutinesApi::class)
class BotSandbox(
    private val scope: TestScope,
) {
    val replies = mutableListOf<MessageEvent>()
    private val replyMap = mutableMapOf<String, MessageEvent>()

    private val dispatcher = StandardTestDispatcher(scope.testScheduler)
    lateinit var pluginLoader: PluginLoader

    init {
        val api = mockk<OpenAPI>(relaxed = true).apply {
            coEvery {
                sendC2CMessage(any(), any<String>(), any(), any(), any(), any(), any(), any())
            } coAnswers {
                val eid = arg<String?>(5) ?: ""
                val mid = arg<String?>(6) ?: ""
                val msg = MessageEvent(
                    bot = pluginLoader.bot, eventId = eid, id = mid,
                    message = MessageChain(PlainText(secondArg())),
                    sender = User(pluginLoader.bot, firstArg()),
                )
                replies += msg
                replyMap[eid] = msg
                true
            }
            coEvery {
                sendGroupMessage(any(), any<String>(), any(), any(), any(), any(), any(), any())
            } coAnswers {
                val eid = arg<String?>(5) ?: ""
                val mid = arg<String?>(6) ?: ""
                val msg = GroupMessageEvent(
                    bot = pluginLoader.bot, eventId = eid, id = mid,
                    message = MessageChain(PlainText(secondArg())),
                    sender = Member(pluginLoader.bot, ""),
                    group = Group(pluginLoader.bot, firstArg()),
                )
                replies += msg
                replyMap[eid] = msg
                true
            }
        }
        pluginLoader = PluginLoader(api,
            mockk(relaxed = true),
            mockk(relaxed = true),
            subscribes = SubscribeManager(dispatcher))
    }

    fun replyFor(event: MessageEvent) = replyMap[event.eventId]

    suspend fun advanceIdle() = scope.advanceUntilIdle()

    fun user(id: String = "test-user") = UserActor(id)

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

    private var replySeq = 0
}
