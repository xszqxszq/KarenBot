package xyz.xszq.karenbot.image

import com.soywiz.korio.async.launchImmediately
import kotlinx.coroutines.coroutineScope
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.console.util.ContactUtils.getContact
import net.mamoe.mirai.console.util.retryCatching
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.message.data.buildForwardMessage
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import xyz.xszq.karenbot.CommandModule
import xyz.xszq.karenbot.GroupCommand
import xyz.xszq.karenbot.NetworkUtils
import xyz.xszq.karenbot.api.Konachan
import xyz.xszq.karenbot.mirai.quoteReply


object EropicHandler: CommandModule("", "image.eropic") {
    override suspend fun subscribe() {
//        events.subscribeGroupMessages {
//            "/k" {
//                konachan.checkAndRun(this)
//            }
//        }
    }
    @OptIn(ConsoleExperimentalApi::class)
    val konachan = GroupCommand("", "konachan", defaultEnabled=false) {
        quoteReply("获取中，请等待私发")
        val shield = bot.getContact(2854196306L) as User
        sender.sendMessage(buildForwardMessage(subject) {
            shield named "星怒" says "略略略~我现在还不能陪你们聊天\uD83D\uDE1B不如@我说“菜单”，各种群游戏先玩起来，看谁最\uD83D\uDC2E"
            shield named "星怒" says "快来找出隐藏其中的AI~"
            coroutineScope {
                Konachan.fetchList().take(10).forEach { eropic ->
                    launchImmediately {
                        retryCatching(10) {
                            NetworkUtils.downloadAsByteArray(eropic.sample_url, proxy = true)
                        }.onSuccess { now ->
                            kotlin.runCatching {
                                now.toExternalResource().use { ex ->
                                    shield named "星怒" says sender.uploadImage(ex)
                                }
                            }.onFailure { e ->
                                e.printStackTrace()
                            }
                        }.onFailure { e ->
                            e.printStackTrace()
                        }
                    }
                }
            }
        })
        sender.sendMessage("分享成功")
    }
}