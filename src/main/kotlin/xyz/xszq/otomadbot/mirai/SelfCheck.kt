package xyz.xszq.otomadbot.mirai

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.BotOnlineEvent
import net.mamoe.mirai.utils.MiraiInternalApi
import xyz.xszq.OtomadBotCore
import xyz.xszq.otomadbot.CommandModule
import xyz.xszq.otomadbot.SafeYamlConfig
import xyz.xszq.otomadbot.text.EventReaction

object SelfCheck: CommandModule("", "self_check") {
    val client = HttpClient()
    var frozen = false
    override suspend fun subscribe() {
        GlobalEventChannel.subscribeAlways<BotOnlineEvent> {
            GlobalScope.launch {
                while (true) {
                    if (OtomadBotCore.cookies.isBlank())
                        continue
                    val html = client.get("https://accounts.qq.com/safe/message/unlock?lock_info=5_5") {
                        header("Cookie", OtomadBotCore.cookies)
                    }.bodyAsText()
                    frozen = if ("该账号未被封禁" in html) {
                        false
                    } else {
                        if (!frozen) {
                            bot.getFriendOrFail(SelfCheckConfig.data.announceAdmin).sendMessage("bot检测到被风控")
                        }
                        true
                    }
                    delay(60000L)
                }
            }
        }
    }

}

@kotlinx.serialization.Serializable
data class SelfCheckConfigData(val announceAdmin: Long = 0L)

object SelfCheckConfig: SafeYamlConfig<SelfCheckConfigData>(OtomadBotCore, "self_check", SelfCheckConfigData())