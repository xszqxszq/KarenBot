package xyz.xszq.bot.chunithm.controller

import xyz.xszq.bot.Chunithm
import xyz.xszq.bot.Chunithm.Companion.textMode
import xyz.xszq.bot.chunithm.component.MarkdownTemplates.Templates.brief
import xyz.xszq.bot.payload.markdown.Keyboard
import xyz.xszq.bot.reply

@Suppress("unused")
class HelpController(
    override val chunithm: Chunithm
): Controller(chunithm) {
    override suspend fun setRoute() = chunithm.route {
        equalsTo("chu") {
            if (textMode()) {
                reply("请查看文档：https://otmdb.cn/bot/chunithm")
            } else {
                reply(brief("中二节奏", buildString {
                    appendLine("这是一个查询中二节奏成绩及相关信息的功能。")
                    append("支持以下功能指令：")
                }).toMessage(Keyboard.create {
                    row {
                        at("🔎查歌", "/chu 奶龙是什么歌", id = "1")
                        at("💯Best30", "/chu b30", id = "2")
                    }
                    row {
                        link("💯随心配30", "https://otmdb.cn/bot/chunithm/combo", id = "3")
                        at("📖分数列表", "/chu 14分数列表", id = "4")
                    }
                }))
            }
        }
    }
}