@file:Suppress("unused", "DeferredResultUnused")
package tk.xszq.otomadbot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.Bot
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.message.data.toMessageChain
import net.mamoe.mirai.utils.BotConfiguration
import net.mamoe.mirai.utils.ExternalResource.Companion.sendAsImageTo
import net.mamoe.mirai.utils.MiraiExperimentalApi
import net.mamoe.mirai.utils.MiraiInternalApi
import tk.xszq.otomadbot.api.MediaWikiUtils
import tk.xszq.otomadbot.api.PyApi
import tk.xszq.otomadbot.api.doInitListener
import tk.xszq.otomadbot.database.*
import tk.xszq.otomadbot.media.*
import java.io.File

var bot: Bot? = null

/**
 * Initialize connections to servers like database.
 */
@MiraiExperimentalApi
suspend fun doInitConnection(retry: Boolean = false): Bot? {
    if (!retry) {
        Databases.init()
        doInitListener()
        bot = BotFactory.newBot(configAccount!!.id, configAccount!!.password) {
            autoReconnectOnForceOffline()
            fileBasedDeviceInfo()
            heartbeatStrategy = BotConfiguration.HeartbeatStrategy.REGISTER // Suggested in https://github.com/mamoe/mirai/issues/1261
        }
    }
    bot!!.login()
    return bot
}

/**
 * Subscribe to specified events.
 */
@MiraiInternalApi
@MiraiExperimentalApi
suspend fun doInitSubscribe() {
    /* Welcome New Members */
    groupMessages.subscribeAlways<MemberJoinEvent> {
        when (this) {
            is MemberJoinEvent.Invite -> require("welcome.invite") {
                group.sendMessage(configMain.text["joinInvite"]!!)
            }
            is MemberJoinEvent.Retrieve -> require("welcome.retrieve") {
                group.sendMessage(configMain.text["joinRetrieve"]!!)
            }
            is MemberJoinEvent.Active -> require("welcome.active") {
                group.sendMessage(configMain.text["joinActive"]!!)
            }
            else -> pass
        }
    }
    /* Nudge */
    bot!!.eventChannel.subscribeAlways<NudgeEvent> {
        require("nudge.bounce", subject) { if (target.id == bot.id) from.nudge().sendTo(subject) }
    }
    groupMessages.subscribeGroupMessages {
        always {
            /* KUSA Repeater */
            require("reply.kusa") {
                val kusa = getCounter("kusa")
                if (message.content != "草")
                    kusa.set(0)
                else
                    kusa.increase()
                if (kusa.value() == 3L)
                    subject.sendMessage("草")
            }
            /* Check for reply rules */
            requireMember("reply.answer") {
                ReplyRules.match(this)?.let {
                    quoteReply(it)
                }
            }
            /* Weak detection */
            require("detect.weak") {
                getCooldown("weakcd").onReady {
                    val analyseResult =
                        TextAnalyser.analyse(message.filterIsInstance<PlainText>().toMessageChain().content)
                    val weak = getCounter("weak")
                    if (TextResult.SHOWING_WEAK in analyseResult) {
                        weak.increase()
                        if (weak.value() == 3L) {
                            File("$pathPrefix/image/hint/1.png").sendAsImageTo(group)
                            weak.set(0)
                        }
                    } else {
                        if (weak.value() > 0)
                            weak.decrease()
                        else weak.set(0)
                    }
                    pass
                }
            }
        }
        /* Atmosphere handler */
        atBot {
            getCooldown("replypic").onReady { cooldown ->
                PyApi().getSentiment(this.message.content)?.let {
                    if (it.positive || it.value < 0.75) replyPic.getRandom("reply")?.sendAsImageTo(group)
                    else replyPic.getRandom("afraid")?.sendAsImageTo(group)
                    cooldown.update()
                }
            }
        }
    }
    /* Image auto reply && Eropic Automatic Download */
    groupMessages.subscribeAlways<GroupMessageEvent> {
        requireMember("reply.answer") {
            withContext(Dispatchers.IO) {
                doHandleImage(this@subscribeAlways)
            }
        }
    }
    /* Register Commands */
    bot!!.registerCommands(AdminUtils)
    bot!!.registerCommands(AudioUtils)
    bot!!.registerCommands(EropicUtils)
    bot!!.registerCommands(ImageUtils)
    bot!!.registerCommands(MediaWikiUtils)
    bot!!.registerCommands(RandomUtils)
    bot!!.registerCommands(WebUtils)
    /* Invite Request */
    bot!!.eventChannel.subscribeAlways<BotInvitedJoinGroupRequestEvent> { accept() }
    /* Friend Request */
    bot!!.eventChannel.subscribeAlways<NewFriendRequestEvent> { accept() }
    /* Dragon King */
    groupMessages.subscribeAlways<GroupTalkativeChangeEvent> {
        if (now.id == bot.id) {
            group.sendMessage("(′▽`)")
        }
    }
    groupMessages.subscribeAlways<GroupMessageEvent> {
        require("content.monitor") {
            requireSender("reply.answer") {
                if (!groupMonitors.containsKey(group.id))
                    groupMonitors[group.id] = GroupMonitor(bot, group.id)
                groupMonitors[group.id]!!.insert(this)
                groupMonitors[group.id]!!.routineCheck()
            }
        }
    }
}

/**
 * Entry point of this kt.
 */
@MiraiExperimentalApi
@MiraiInternalApi
suspend fun main() {
    doInit()
    doInitConnection()
    doInitSubscribe()
    MiraiMonitor(bot!!).launch()
    bot!!.join()
}
