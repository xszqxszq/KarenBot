package tk.xszq.otomadbot.image

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kds.iterators.fastForEachWithIndex
import com.soywiz.korio.async.launchImmediately
import kotlinx.coroutines.coroutineScope
import net.mamoe.mirai.console.permission.PermissionId
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.permission.PermitteeId.Companion.permitteeId
import net.mamoe.mirai.console.util.retryCatching
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import tk.xszq.otomadbot.*
import tk.xszq.otomadbot.api.*
import tk.xszq.otomadbot.core.*
import tk.xszq.otomadbot.core.OtomadBotCore.bot
import tk.xszq.otomadbot.text.TextSettings

object EropicHandler: EventHandler("随机涩图", "eropic", HandlerType.RESTRICTED_DISABLED) {
    private val r18 by lazy {
        PermissionService.INSTANCE.register(
            PermissionId("otm", "eropic.r18"), "允许R18")
    }
    private val cooldown = Cooldown("eropic")
    override fun register() {
        PicaComic.reloadConfig()
        PicaComic.login()
        GlobalEventChannel.subscribeGroupMessages {
            finding(Regex(TextSettings.regex.eropic)) { finding ->
                requireOr(allowed, false) {
                    handle(finding, this)
                    pass
                }
            }
            finding(Regex(TextSettings.regex.eropicBatch)) { keyword ->
                requireOr(allowed, false) {
                    quoteReply("请稍等，正在获取中……")
                    handle(keyword, this, 10)
                }
            }
            "/k" {
                require(r18) {
                    quoteReply("请稍等，正在获取中……")
                    handleKonachan(this)
                }
            }
            "/p" {
                require(r18) {
                    quoteReply("请稍等，正在获取中……")
                    (1..10).forEach { _ ->
                        runCatching {
                            handlePica(this)
                        }.onSuccess {
                            return@require
                        }
                    }
                }
            }
        }
        r18
        super.register()
    }
    private suspend fun handle(keyword: MatchResult, event: GroupMessageEvent, amount: Int = 1) = event.run {
        val before = cooldown.get(group)
        ifReady(cooldown) {
            update(cooldown)
            val result = RandomEropic.get(keyword.groupValues.getOrNull(1)?.trim(),
                r18 = group.permitteeId.hasPermission(r18), num = amount)
            if (result.data.isNotEmpty()) {
                kotlin.runCatching {
                    val shield = bot.getFriendOrFail(2854196306L)
                    val content = ForwardMessageBuilder(shield)
                    coroutineScope {
                        result.data.fastForEach {
                            val pid = it.pid.toString()
                            val author = it.author
                            launchImmediately {
                                OtomadBotCore.logger.info(it.urls.toString())
                                retryCatching(10) {
                                    NetworkUtils.downloadAsByteArray(
                                        it.urls["regular"] ?: it.urls["original"]!!,
                                        RandomEropic.pixivHeader, true)
                                }.onSuccess { now ->
                                    kotlin.runCatching {
                                        now.toExternalResource().use { ex ->
                                            content.add(shield,
                                                ex.uploadAsImage(group)
                                                        + PlainText("\nPID: $pid\n作者：$author"))
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
                    kotlin.runCatching {
                        content.build().sendTo(group).recallIn(115000L)
                        quoteReply("涩图已分享，如果看不到可能是太涩被腾讯拦截了").recallIn(115000L)
                    }.onFailure { err ->
                        bot.logger.error(err)
                        quoteReply("被腾讯拦截了o(╥﹏╥)o\n请稍后重试")
                        cooldown.set(group, before)
                    }
                }.onFailure { err ->
                    bot.logger.error(err)
                    quoteReply("下载不下来，怎么会事呢")
                    cooldown.set(group, before)
                }
            } else {
                quoteReply("没有找到涩图，怎么会事呢")
                cooldown.set(group, before)
            }
            pass
        } ?: run {
            quoteReply("冲得太快了，达咩~\n还要再等 " + remaining(cooldown) + " 秒哦")
        }
    }
    suspend fun handleGuild(keyword: String, event: GOCQGuildMessageEvent, amount: Int = 1) = event.run {
        ifReady(cooldown) {
            update(cooldown)
            val result = RandomEropic.get(keyword.trim(), r18 = false, num = amount)
            if (result.data.isNotEmpty()) {
                kotlin.runCatching {
                    val content = MessageChainBuilder()
                    coroutineScope {
                        result.data.fastForEach {
                            val pid = it.pid.toString()
                            val author = it.author
                            launchImmediately {
                                OtomadBotCore.logger.info(it.urls.toString())
                                retryCatching(10) {
                                    NetworkUtils.downloadAsByteArray(
                                        it.urls["regular"] ?: it.urls["original"]!!,
                                        RandomEropic.pixivHeader, true
                                    )
                                }.onSuccess { now ->
                                    kotlin.runCatching {
                                        now.toExternalResource().use { ex ->
                                            content.add(
                                                ex.uploadAsImage(bot.asFriend)
                                                        + PlainText("\nPID: $pid\n作者：$author")
                                            )
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
                    kotlin.runCatching {
                        channel.sendMessage(content.build()) ?: throw Exception("传不上来")
                    }.onFailure { err ->
                        err.printStackTrace()
                        quoteReply("被腾讯拦截了o(╥﹏╥)o\n请稍后重试")
                        cooldown.reset(channel.id.toLong())
                    }
                }.onFailure { err ->
                    bot.logger.error(err)
                    quoteReply("下载不下来，怎么会事呢")
                    cooldown.reset(channel.id.toLong())
                }
            } else {
                quoteReply("没有找到涩图，怎么会事呢")
                cooldown.reset(channel.id.toLong())
            }
        }
    }
    private suspend fun handleKonachan(event: GroupMessageEvent) = event.run {
        val shield = bot.getFriendOrFail(2854196306L)
        val content = ForwardMessageBuilder(shield)
            .add(shield, "略略略~我现在还不能陪你们聊天\uD83D\uDE1B不如@我说“菜单”，各种群游戏先玩起来，看谁最\uD83D\uDC2E"
                .toPlainText())
            .add(shield, "快来找出隐藏其中的AI~"
                .toPlainText())
        coroutineScope {
            Konachan.fetchList().forEach { eropic ->
                launchImmediately {
                    val author = eropic.author
                    val kid = eropic.id
                    retryCatching(10) {
                        NetworkUtils.downloadAsByteArray(eropic.sample_url, proxy = true)
                    }.onSuccess { now ->
                        kotlin.runCatching {
                            now.toExternalResource().use { ex ->
                                content.add(shield, ex.uploadAsImage(group)
                                            + "\nID: $kid\n作者：$author".toPlainText())
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
        group.sendMessage(content.build())
        quoteReply("分享成功")
    }
    private suspend fun handlePica(event: GroupMessageEvent) = event.run {
        val selected = PicaComic.random().random()
        val images = PicaComic.getPage(selected._id, 1, 1) !!.pages.docs.map { it.media.toUrl() }
        val shield = bot.getFriendOrFail(2854196306L)
        val content = ForwardMessageBuilder(shield)
            .add(shield, selected.title.toPlainText())
            .add(shield, "作者：${selected.author}".toPlainText())
        val imgData = mutableMapOf<Int, ByteArray>()
        coroutineScope {
            images.fastForEachWithIndex { index, now ->
                launchImmediately {
                    imgData[index] = NetworkUtils.downloadAsByteArray(now, proxy = true)
                }
            }
        }
        images.fastForEachWithIndex { index, _ ->
            imgData[index]!!.toExternalResource().use {
                content.add(shield, it.uploadAsImage(group))
            }
        }
        group.sendMessage(content.build())
        quoteReply("分享成功")
    }
}