package xyz.xszq.bot.controller

import xyz.xszq.bot.Maimai
import xyz.xszq.bot.database.DivingFishBindTable
import xyz.xszq.bot.database.MaimaiBindTable
import xyz.xszq.bot.event.GroupEvent
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.reply

@Suppress("unused")
class UpdateController(
    override val maimai: Maimai
): Controller(maimai) {
    val connector = maimai.localConnector

    fun MessageEvent.isAllowed() = sender.id in connector.config.allowed ||
            (this is GroupEvent && group.id in connector.config.allowed)
    override fun setRoute() = maimai.route {
        startsWith("SGWC") { encoded ->
            if (!isAllowed())
                return@startsWith
            if (encoded.isBlank())
                return@startsWith
            val response = connector.qr(encoded)
            if (response.userID == -1L) {
                reply("验证失败，请重试！")
                return@startsWith
            }
            MaimaiBindTable.update(sender.id, response.userID)
            reply("绑定成功。")
        }
        startsWith("uid") { userID ->
            if (!isAllowed())
                return@startsWith
            if (userID.isBlank() || userID.any { !it.isDigit() })
                return@startsWith
            val uid = userID.toLong()
            MaimaiBindTable.update(sender.id, uid)
            reply("绑定成功。")
        }
        startsWith("绑定") { token ->
            if (!isAllowed())
                return@startsWith
            if (token.isBlank()) {
                reply("使用方法：/绑定 水鱼查分器token")
                return@startsWith
            }
            DivingFishBindTable.update(sender.id, token)
            reply("水鱼token绑定成功。")
        }
        startsWith("导") { _ ->
            if (!isAllowed())
                return@startsWith
            update(this)
        }
        startsWith("更新") { _ ->
            if (!isAllowed())
                return@startsWith
            update(this)
        }
        startsWith("region") {
            if (!isAllowed())
                return@startsWith
            val userId = MaimaiBindTable[sender.id] ?: run {
                reply("请先绑定账号！\n绑定账号：@bot XXXXXXXXXX")
                return@startsWith
            }
            runCatching {
                connector.region(userId)
            }.onFailure {
                it.printStackTrace()
            }.getOrNull() ?.let { regions ->
                reply(buildString {
                    appendLine("您的全国游玩记录如下：")
                    regions.forEach { region ->
                        appendLine("[${region.region}] ${region.playCount} 次 (首次游玩于 ${region.created})")
                    }
                }.trim())
            } ?: run {
                reply("查询失败，请检查uid是否正确")
            }
        }
    }

    suspend fun update(event: MessageEvent) = event.run {
        val userId = MaimaiBindTable[sender.id]
        val importToken = DivingFishBindTable[sender.id]
        if (userId == null || importToken == null) {
            reply("请先绑定账号和水鱼导入token！\n绑定账号：@bot XXXXXXXXXX\n绑定水鱼：/绑定 水鱼查分器token")
            return@run
        }
        reply("正在更新中……")
        runCatching {
            connector.update(userId, importToken)
        }.onFailure {
            it.printStackTrace()
        }.getOrNull() ?.let { response ->
            reply(buildString {
                append("更新成功。")
            })
        } ?: run {
            reply("更新失败，请检查水鱼token是否正确")
        }
    }
}