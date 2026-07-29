package xyz.xszq.bot.maimai.controller

import xyz.xszq.bot.Maimai

@Suppress("unused")
class HelpController(
    override val maimai: Maimai
): Controller(maimai) {
    override suspend fun setRoute() = maimai.route {
        equalsTo("mai") {
            reply("请查看文档：https://otmdb.cn/bot/maimai") {
                brief("舞萌DX", buildString {
                    appendLine("这是一个查询舞萌DX成绩及相关信息的功能。")
                    append("支持以下功能指令：")
                })
                keyboard {
                    row {
                        at("🔎查歌", "/mai 牛奶歌是什么歌")
                        at("📋单曲成绩", "/mai info 海底谭")
                    }
                    row {
                        at("💯Best50", "/mai b50")
                        link("随心配50", "https://otmdb.cn/bot/maimai/combo")
                    }
                    row {
                        at("⏳完成表", "/mai 橙将完成表")
                        at("📖分数列表", "/mai 13分数列表")
                    }
                    row {
                        at("🕹️开字母", "舞萌开字母")
                        link("更多功能...", "https://otmdb.cn/bot/maimai")
                    }
                }
            }
        }
    }
}