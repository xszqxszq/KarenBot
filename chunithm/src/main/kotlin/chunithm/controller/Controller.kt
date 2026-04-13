package xyz.xszq.bot.chunithm.controller

import xyz.xszq.bot.Chunithm
import xyz.xszq.bot.chunithm.component.ChunithmQuery
import xyz.xszq.bot.chunithm.database.MaimaiSettingsTable
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.exception.NotFoundException
import xyz.xszq.bot.reply

sealed class Controller(
    open val chunithm: Chunithm
) {
    abstract suspend fun setRoute()
    open suspend fun unload() {}

    suspend fun rhythm(
        block: suspend xyz.xszq.bot.subscribe.SubscribeBuilder.() -> Unit
    ) {
        chunithm.route("/chu") {
            domain(
                name = "rhythm",
                value = "chunithm",
                defaultHandler = {
                    MaimaiSettingsTable.defaultGame(sender.id)
                },
                block = block
            )
        }
    }

    suspend fun handleError(
        event: MessageEvent,
        e: Throwable
    ) {
        with(event) {
            when (e) {
                is NotFoundException -> reply(e.message.orEmpty())
                else -> reply(ChunithmQuery.QUERY_FAILED)
            }
        }
    }
}