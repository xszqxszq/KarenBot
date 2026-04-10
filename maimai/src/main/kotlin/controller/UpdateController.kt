package xyz.xszq.bot.controller

import korlibs.io.util.UUID
import xyz.xszq.bot.Maimai
import xyz.xszq.bot.Maimai.Companion.textMode
import xyz.xszq.bot.component.MarkdownTemplates
import xyz.xszq.bot.component.WaitingEventData
import xyz.xszq.bot.database.DivingFishBindTable
import xyz.xszq.bot.message.Markdown
import xyz.xszq.bot.reply

@Suppress("unused")
class UpdateController(
    override val maimai: Maimai
): Controller(maimai) {
    override suspend fun setRoute() = maimai.route("/mai") {
        startsWith("更新") {
            if (DivingFishBindTable[sender.id] == null) {
                if (textMode())
                    reply("请先使用“/绑定水鱼 水鱼成绩导入Token”来设置！")
                else
                    reply(Markdown(
                        MarkdownTemplates.Templates.brief(
                            "更新查分器",
                            "请先点击下方输入水鱼成绩导入Token："
                        ),
                        MarkdownTemplates.Keyboards.BIND_DF
                    ))
                return@startsWith
            }
            val token = UUID.randomUUID().toString().replace("-", "")
            maimai.api.updateTokens[token] = WaitingEventData(this)
            reply("${maimai.config.apiServer}/update?token=$token")
            if (textMode())
                reply("请连接代理，并复制上方链接至微信中打开")
            else
                reply(Markdown(
                    MarkdownTemplates.Templates.brief(
                        "更新查分器",
                        "请点击查看下方教程并设置好代理，然后复制上方链接至微信中打开："
                    ),
                    MarkdownTemplates.Keyboards.HELP_PROXY
                ))
            reply("请连接代理，并复制上方链接至微信中打开")
        }
        startsWith("绑定水鱼") { token ->
            if (token.isBlank()) {
                if (textMode())
                    reply("使用方法：/绑定 水鱼成绩导入Token")
                else
                    reply(Markdown(
                        MarkdownTemplates.Templates.brief(
                            "绑定水鱼",
                            "请点击下方输入水鱼成绩导入Token："
                        ),
                        MarkdownTemplates.Keyboards.BIND_DF
                    ))
                return@startsWith
            }
            DivingFishBindTable.update(sender.id, token)

            if (textMode())
                reply("水鱼token绑定成功。")
            else
                reply(Markdown(
                    MarkdownTemplates.Templates.brief(
                        "绑定水鱼",
                        "水鱼token绑定成功。"
                    ),
                    MarkdownTemplates.Keyboards.UPDATE
                ))
        }
    }
}