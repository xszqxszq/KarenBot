package xyz.xszq.otomadbot.image

import net.mamoe.mirai.event.subscribeMessages
import xyz.xszq.events
import xyz.xszq.otomadbot.CommandModule
import xyz.xszq.otomadbot.CommonCommand
import xyz.xszq.otomadbot.mirai.quoteReply
import xyz.xszq.otomadbot.mirai.startsWithSimple


object ImageTemplateHandler: CommandModule("图像模板", "image.template") {
    override suspend fun subscribe() {
        events.subscribeMessages {
            startsWithSimple("/生成") { _, _ ->
                todo.checkAndRun(this)
            }
        }
    }
    val todo = CommonCommand("生成", "template") {
        quoteReply("由于经常被举报，此功能暂时不予开放。如果您愿意提供恶意举报者的相关线索，请联系号主，届时可恢复本功能。")
    }
}