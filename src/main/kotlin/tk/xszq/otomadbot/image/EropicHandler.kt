package tk.xszq.otomadbot.image

import net.mamoe.mirai.console.permission.PermissionId
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.permission.PermitteeId.Companion.permitteeId
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.sendTo
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import tk.xszq.otomadbot.*
import tk.xszq.otomadbot.NetworkUtils.downloadTempFile
import tk.xszq.otomadbot.api.RandomEropic
import tk.xszq.otomadbot.core.*
import tk.xszq.otomadbot.text.TextSettings

object EropicHandler: EventHandler("随机涩图", "eropic", HandlerType.RESTRICTED_DISABLED) {
    private val r18 by lazy {
        PermissionService.INSTANCE.register(
            PermissionId("otm", "eropic.r18"), "允许R18")
    }
    private val cooldown = Cooldown("eropic")
    override fun register() {
        GlobalEventChannel.subscribeGroupMessages {
            finding(Regex(TextSettings.regex.eropic)) { finding ->
                requireOr(allowed, false) {
                    handle(finding, this)
                    pass
                }
            }
        }
        r18
        super.register()
    }
    private suspend fun handle(keyword: MatchResult, event: GroupMessageEvent) = event.run {
        val before = cooldown.get(group)
        ifReady(cooldown) {
            update(cooldown)
            val result = RandomEropic.get(keyword.groupValues.getOrNull(1)?.trim(),
                r18 = group.permitteeId.hasPermission(r18))
            when {
                result.code == 0 && result.count > 0 -> {
                    kotlin.runCatching {
                        val pid = result.data[0].pid.toString()
                        val author = result.data[0].author
                        OtomadBotCore.logger.info(result.data[0].url)
                        val img = downloadTempFile(result.data[0].url, RandomEropic.pixivHeader, proxy = true)
                        img?.let { file ->
                            file.toExternalResource().use {
                                kotlin.runCatching {
                                    val shield = bot.getFriendOrFail(2854196306L)
                                    val content = ForwardMessageBuilder(shield)
                                        .add(shield, it.uploadAsImage(group))
                                        .add(shield, PlainText("PID: $pid\n作者：$author"))
                                        .build()
                                    content.sendTo(group).recallIn(60000L)
                                    quoteReply("涩图已分享，如果看不到可能是太涩被腾讯拦截了").recallIn(60000L)
                                }.onFailure { err ->
                                    bot.logger.error(err)
                                    quoteReply("被腾讯拦截了o(╥﹏╥)o\n请稍后重试")
                                    cooldown.set(group, before)
                                }
                            }
                            file.delete()
                        }
                    }.onFailure { err ->
                        bot.logger.error(err)
                        quoteReply("下载不下来，怎么会事呢")
                        cooldown.set(group, before)
                    }
                }
                result.code == 429 -> {
                    quoteReply("今天给的涩图太多了，下次想看还需${result.quota_min_ttl}秒")
                    cooldown.set(group, before)
                }
                result.code == 404 -> {
                    quoteReply("看起来图库里没有该关键词的涩图(R-15)哦~")
                    cooldown.set(group, before)
                }
                else -> {
                    quoteReply("无法获取涩图，api返回状态码：${result.code}")
                    cooldown.set(group, before)
                }
            }
            pass
        } ?: run {
            quoteReply("冲得太快了，达咩~\n还要再等 " + remaining(cooldown) + " 秒哦")
        }
    }
}