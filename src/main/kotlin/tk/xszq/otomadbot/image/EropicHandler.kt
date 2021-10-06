package tk.xszq.otomadbot.image

import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import tk.xszq.otomadbot.EventHandler
import tk.xszq.otomadbot.HandlerType
import tk.xszq.otomadbot.NetworkUtils.downloadTempFile
import tk.xszq.otomadbot.api.RandomEropic
import tk.xszq.otomadbot.core.Cooldown
import tk.xszq.otomadbot.core.ifReady
import tk.xszq.otomadbot.core.remaining
import tk.xszq.otomadbot.core.update
import tk.xszq.otomadbot.quoteReply
import tk.xszq.otomadbot.requireOr
import tk.xszq.otomadbot.text.TextSettings

object EropicHandler: EventHandler("随机涩图", "eropic", HandlerType.RESTRICTED_DISABLED) {
    private val cooldown = Cooldown("eropic")
    override fun register() {
        GlobalEventChannel.subscribeGroupMessages {
            finding(Regex(TextSettings.regex.eropic)) { finding ->
                requireOr(allowed, group.members.size in 4L..60L) {
                    handle(finding, this)
                }
            }
        }
        super.register()
    }
    private suspend fun handle(keyword: MatchResult, event: GroupMessageEvent) = event.run {
        val before = cooldown.get(group)
        ifReady(cooldown) {
            update(cooldown)
            val result = RandomEropic.get(keyword.groupValues.getOrNull(1)?.trim())
            when {
                result.code == 0 && result.count > 0 -> {
                    val pid = result.data[0].pid.toString()
                    val author = result.data[0].author
                    val img = downloadTempFile(result.data[0].url, RandomEropic.pixivHeader)
                    img?.let { file ->
                        file.toExternalResource().use {
                            kotlin.runCatching {
                                group.sendMessage(it.uploadAsImage(event.group)).recallIn(60000L)
                                group.sendMessage("PID: $pid\n作者：$author")
                            }.onFailure { err ->
                                bot.logger.error(err)
                                quoteReply("被腾讯拦截了o(╥﹏╥)o\n请稍后重试")
                                cooldown.set(group, before)
                            }
                        }
                        file.delete()
                    }
                }
                result.code == 429 -> {
                    quoteReply("今天给的涩图太多了，下次想看还需${result.quota_min_ttl}秒")
                    cooldown.set(group, before)
                }
                result.code == 404 -> quoteReply("看起来图库里没有该关键词的涩图(R-15)哦~")
                else -> {
                    quoteReply("无法获取涩图，api返回状态码：${result.code}")
                    cooldown.set(group, before)
                }
            }
        } ?: run {
            quoteReply("冲得太快了，达咩~\n还要再等 " + remaining(cooldown) + " 秒哦")
        }
    }
}