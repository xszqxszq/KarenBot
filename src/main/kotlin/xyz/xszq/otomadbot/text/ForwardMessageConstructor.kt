package xyz.xszq.otomadbot.text

import com.soywiz.korio.async.launchImmediately
import kotlinx.coroutines.coroutineScope
import net.mamoe.mirai.console.util.retryCatching
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.data.ForwardMessage
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.toPlainText
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import xyz.xszq.otomadbot.EventHandler
import xyz.xszq.otomadbot.HandlerType
import xyz.xszq.otomadbot.NetworkUtils
import xyz.xszq.otomadbot.api.Konachan
import xyz.xszq.otomadbot.requireBotAdmin

object ForwardMessageConstructor: EventHandler("forward", "转发消息伪造",
    HandlerType.DEFAULT_DISABLED) {
    override fun register() {
        GlobalEventChannel.subscribeAlways<GroupMessageEvent> {
            if (it.message.first() is ForwardMessage) {
                println(it.message.serializeToMiraiCode())
            }
        }
        GlobalEventChannel.subscribeMessages {
            startsWith("伪造消息", true) { target ->
                requireBotAdmin {
                    val group = bot.groups.find { it.id == 799059220L }
                    val badMessage = Konachan.fetchList()
                    val content = ForwardMessageBuilder(group as Contact)
                    content.add(group.members[target.toLong()] as User, "给大伙发点色的".toPlainText())
                    content.add(group.members[target.toLong()] as User, "我要开车了".toPlainText())
                    coroutineScope {
                        badMessage.take(10).forEach { eropic ->
                            launchImmediately {
                                retryCatching(10) {
                                    NetworkUtils.downloadAsByteArray(eropic.sample_url, proxy = true)
                                }.onSuccess { now ->
                                    kotlin.runCatching {
                                        now.toExternalResource().use { ex ->
                                            content.add(
                                                group.members[target.toLong()] as User, ex.uploadAsImage(group))
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
                    subject.sendMessage(content.build())
                }
            }
        }
        super.register()
    }
}