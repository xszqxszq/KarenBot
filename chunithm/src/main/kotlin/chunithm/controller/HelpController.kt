package xyz.xszq.bot.chunithm.controller

import xyz.xszq.bot.chunithm.Chunithm

@Suppress("unused")
class HelpController(
    override val chunithm: Chunithm
): Controller(chunithm) {
    override suspend fun setRoute() = chunithm.route {
        equalsTo("chu") {
            reply("请查看文档：https://otmdb.cn/bot/chunithm") {
                brief("中二节奏", buildString {
                    appendLine("这是一个查询中二节奏成绩及相关信息的功能。")
                    append("支持以下功能指令：")
                })
                keyboard {
                    row {
                        at("🔎查歌", "/chu 奶龙是什么歌")
                        at("💯Best30", "/chu b30")
                    }
                    row {
                        link("💯随心配30", "https://otmdb.cn/bot/chunithm/combo")
                        at("📖分数列表", "/chu 14分数列表")
                    }
                    row {
                        at("⏳定数表", "/chu 14+定数表")
                        link("更多功能...", "https://otmdb.cn/bot/chunithm")
                    }
                }
            }
        }
    }
}