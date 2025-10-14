package xyz.xszq.bot.controller

import xyz.xszq.bot.Maimai
import xyz.xszq.bot.Maimai.Companion.textMode
import xyz.xszq.bot.MarkdownTemplates
import xyz.xszq.bot.reply

@Suppress("unused")
class HelpController(
    override val maimai: Maimai
): Controller(maimai) {
    override fun setRoute() = maimai.route {
        equalsTo("mai") {
            if (textMode()) {
                reply("请查看文档：https://otmdb.cn/bot/maimai")
            } else {
                reply(MarkdownTemplates.Templates.HELP)
            }
        }
    }
}