package xyz.xszq.bot.chunithm.controller

import xyz.xszq.bot.chunithm.Chunithm
import xyz.xszq.bot.chunithm.api.LXNS
import xyz.xszq.bot.chunithm.database.MaimaiSettingsTable
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.newLine
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

    private suspend fun collections() = rhythm {
        startsWith("设置头像") { icon ->
            val chunithmData = chunithm.chunithmData
            val query = icon.trim()
            val avatarId = chunithmData.avatarIds.firstOrNull { id ->
                id == query.toIntOrNull() ||
                        chunithmData.characters[id] ?.name == query
            } ?: run {
                reply(buildString {
                    appendLine("使用方法：设置头像 <id/名称>")
                    appendLine("\t例：设置头像 500")
                    appendLine("\t例：设置头像 天王洲 なずな")
                    appendLine()
                    appendLine("\t头像列表：https://otmdb.cn/bot/chunithm/icons")
                }.trim().newLine()) {
                    brief("设置头像", buildString {
                        appendLine("使用方法：设置头像 id/名称")
                        appendLine("👉设置头像 500")
                        appendLine("👉设置头像 天王洲 なずな")
                        appendLine(" ")
                        append("⏬您可以点击下方按钮查看头像列表。")
                    })
                    keyboard {
                        row {
                            link("选择头像", "https://otmdb.cn/bot/chunithm/icons")
                            at("⚙ 设置头像", "/chu 设置头像 ")
                        }
                    }
                }
                return@startsWith
            }
            MaimaiSettingsTable[sender.id, MaimaiSettingsTable.ICON_KEY] =
                avatarId.toString()
            reply("设置头像成功。") {
                brief("设置头像", "设置头像成功。")
                keyboard {
                    row {
                        link("选择头像", "https://otmdb.cn/bot/chunithm/icons")
                        at("⚙ 设置头像", "/chu 设置头像 ")
                    }
                }
            }
        }
        startsWith("设置牌子") { plate ->
            val chunithmData = chunithm.chunithmData
            val query = plate.trim()
            val plateId = chunithmData.plateIds.firstOrNull { id ->
                id == query.toIntOrNull() ||
                        chunithmData.plates[id] ?.name == query
            } ?: run {
                reply(buildString {
                    appendLine("使用方法：设置牌子 <id/名称>")
                    appendLine("\t例：设置牌子 10129")
                    appendLine("\t例：设置牌子 NEEDY GIRL OVERDOSE 【1】")
                    appendLine()
                    appendLine("\t牌子列表：https://otmdb.cn/bot/chunithm/plates")
                }.trim().newLine()) {
                    brief("设置牌子", buildString {
                        appendLine("使用方法：设置牌子 id/名称")
                        appendLine("👉设置牌子 10129")
                        appendLine("👉设置牌子 NEEDY GIRL OVERDOSE 【1】")
                        appendLine(" ")
                        append("⏬您可以点击下方按钮查看牌子列表。")
                    })
                    keyboard {
                        row {
                            link("选择牌子", "https://otmdb.cn/bot/chunithm/plates")
                            at("⚙ 设置牌子", "/chu 设置牌子 ")
                        }
                    }
                }
                return@startsWith
            }
            MaimaiSettingsTable[sender.id, MaimaiSettingsTable.PLATE_KEY] =
                plateId.toString()
            reply("设置牌子成功。") {
                brief("设置牌子", "设置牌子成功。")
                keyboard {
                    row {
                        link("选择牌子", "https://otmdb.cn/bot/chunithm/plates")
                        at("⚙ 设置牌子", "/chu 设置牌子 ")
                    }
                }
            }
        }
        startsWith(listOf("设置chu", "设置b50")) {
            reply(buildString {
                appendLine("支持以下设置：")
                appendLine("→设置头像 头像ID/名称")
                appendLine("\t例：设置头像 500")
                appendLine("\t例：设置头像 天王洲 なずな")
                appendLine("→设置牌子 牌子ID/名称")
                appendLine("\t例：设置牌子 10129")
                appendLine("\t例：设置牌子 NEEDY GIRL OVERDOSE 【1】")
                appendLine("→设置查分器 查分器名称")
                appendLine("\t例：设置查分器 水鱼")
                appendLine("\t例：设置查分器 落雪")
            }.trim().newLine()) {
                brief("功能设置", "支持以下设定：")
                keyboard {
                    row {
                        at("👤设置头像", "/chu 设置头像", enter = true)
                        at("📰设置牌子", "/chu 设置牌子", enter = true)
                    }
                    row {
                        at("🐟使用水鱼查分", "设置查分器 水鱼", enter = true)
                        at("❄使用落雪查分", "设置查分器 落雪", enter = true)
                    }
                    row {
                        at("🔄自动选择查分器", "设置查分器 自动", enter = true)
                    }
                }
            }
        }
    }

    override suspend fun setRoute() {
        route()
        collections()
        chunithm.route("/chu", true) {
            startsWith(listOf("默认", "设为默认")) {
                MaimaiSettingsTable.setDefaultGame(sender.id, "chunithm")
                reply("设置成功，在不带“/mai”“/chu”命令前缀时，将默认选择使用中二节奏的相关功能")
            }
        }
    }
}