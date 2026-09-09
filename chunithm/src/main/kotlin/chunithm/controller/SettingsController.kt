package xyz.xszq.bot.chunithm.controller

import xyz.xszq.bot.chunithm.Chunithm
import xyz.xszq.bot.chunithm.api.LXNS
import xyz.xszq.bot.chunithm.database.MaimaiSettingsTable
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.reply

/**
 * 个人设置功能
 */
@Suppress("unused")
class SettingsController(
    override val chunithm: Chunithm
): Controller(chunithm) {
    private suspend fun route() = chunithm.route {
        channel<MessageEvent>("chunithm-update-friend-code") { event ->
            runCatching {
                (chunithm.backend("lxns") as LXNS).updateFriendCode(event.sender.id)
            }
        }
    }

    override suspend fun setRoute() {
        route()
        chunithm.route("/chu", true) {
            startsWith(listOf("默认", "设为默认")) {
                MaimaiSettingsTable.setDefaultGame(sender.id, "chunithm")
                reply("设置成功，在不带“/mai”“/chu”命令前缀时，将默认选择使用中二节奏的相关功能")
            }
        }
    }
}
