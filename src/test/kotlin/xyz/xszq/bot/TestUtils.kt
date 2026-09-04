package xyz.xszq.bot

import io.mockk.mockk
import xyz.xszq.bot.event.InteractionEvent
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.message.MessageChain
import xyz.xszq.bot.message.PlainText

fun testBot() = Bot(
    api = mockk(relaxed = true),
    cos = mockk(relaxed = true)
)

fun messageEvent(
    text: String,
    bot: Bot = testBot(),
    eventId: String = "eventId",
    id: String = "messageId",
    userId: String = "userId"
) = MessageEvent(
    bot = bot,
    eventId = eventId,
    id = id,
    message = MessageChain(PlainText(text)),
    sender = User(bot, userId)
)

fun interactionEvent(
    button: String,
    data: String = "data",
    bot: Bot = testBot(),
    eventId: String = "eventId",
    id: String = "interaction",
    userId: String = "userId"
) = InteractionEvent(
    bot = bot,
    eventId = eventId,
    id = id,
    data = data,
    button = button,
    sender = User(bot, userId)
)