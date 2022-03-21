package tk.xszq.otomadbot

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import tk.xszq.otomadbot.api.*
import tk.xszq.otomadbot.core.OtomadBotCore
import tk.xszq.otomadbot.core.OtomadBotCore.logger
import tk.xszq.otomadbot.core.ifReady
import tk.xszq.otomadbot.core.update
import tk.xszq.otomadbot.image.EropicHandler
import tk.xszq.otomadbot.image.ImageCommonHandler
import tk.xszq.otomadbot.text.AutoReplyHandler
import tk.xszq.otomadbot.text.SentimentDetector

object GuildHandler: EventHandler("QQ频道", "guild") {
    val guildBot by lazy {
        runBlocking {
            Guild3rdPartyBot.get()
        }
    }
    override fun register() {
        embeddedServer(Netty, port = 19199) {
            routing {
                install(ContentNegotiation) {
                    json(Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    })
                }
                post("/") {
                    GOCQEvent.parse(call.receive()).run {
                        when (this) {
                            is GOCQGuildMessageEvent -> {
                                call.response.status(HttpStatusCode.OK)
                                call.respond("")
                                logger.info("[${channel.name}(${channel.id})] ${sender.nickname}(${sender.user_id})" +
                                        " -> $message")
                                val channelDigitId = channel.id.filter { it.isDigit() }.toLong()
                                AutoReplyHandler.matchText(message, channelDigitId)?.let { quoteReply(it) } ?: run {
                                    // TODO: Parse image
//                                    if (message.anyIsInstance<Image>() &&
//                                        AutoReplyHandler.config.rules.filter { it.value.group == group.id }.isNotEmpty()) {
//                                        val textList = mutableListOf<String>()
//                                        message.filterIsInstance<Image>().forEach {
//                                            textList.add(PythonApi.ocr(it.queryUrl()).lowercase())
//                                        }
//                                        AutoReplyHandler.matchImage(textList, group.id)?.let { quoteReply(it) }
//                                    }
                                }
                                if (GuildAt(channelDigitId).contentToString() in message) {
                                    ifReady(SentimentDetector.cooldown) {
                                        quoteReply(
                                            ImageCommonHandler.replyPic.getRandom(
                                                if (PythonApi.sentiment(message)!!) "reply"
                                                else "afraid"
                                            ).uploadAsImage(OtomadBotCore.bot.asFriend)
                                        )
                                        update(SentimentDetector.cooldown)
                                    }
                                }
                                if ("好康" in channel.name) {
                                    when {
                                        message.startsWith("/eropic") -> {
                                            val keyword = message.substringAfter("/eropic")
                                            EropicHandler.handleGuild(keyword, this, 1)
                                        }
                                        else -> {}
                                    }
                                }
                            }
                            is GOCQNormalMessageEvent -> {}
                            is GOCQHeartbeatEvent -> {
                                call.response.status(HttpStatusCode.OK)
                                call.respond("")
                            }
                        }
                    }
                }
            }
        }.start(false)
    }
}