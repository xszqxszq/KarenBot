@file:Suppress("unused")
package tk.xszq.otomadbot.api

import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import kotlinx.coroutines.delay
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.message.code.MiraiCode
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.SimpleServiceMessage
import net.mamoe.mirai.utils.MiraiExperimentalApi
import tk.xszq.otomadbot.bot
import tk.xszq.otomadbot.configMain
import tk.xszq.otomadbot.toArgsList

data class ApiResult(val status: Boolean, val data: Any)
@MiraiExperimentalApi
suspend fun doInitListener() {
    embeddedServer(Netty, port = configMain.listener["port"].toString().toInt()) {
        routing {
            get("/") {
                call.respondText(bot!!.isOnline.toString())
            }
            get("/relogin") {
                call.respondText {
                    try {
                        for (i in 0..30)
                            bot!!.login()
                        true
                    } catch (e: Exception) {
                        false
                    }.toString()
                }
            }
            post("/sendmsg") {
                val args = call.receiveParameters()
                var status = true
                var error = ""
                if (!args.contains("token") || !args.contains("subject")
                    || !args.contains("subject_type") || !args.contains("message") || !args.contains("message_type")) {
                    status = false
                    error = "Missing parameters"
                } else if (args["token"] != configMain.listener["token"].toString()) {
                    status = false
                    error = "Invalid token"
                } else {
                    var targetGroup = mutableListOf<Group>()
                    val groups = args.getOrFail("subject").toArgsList()
                    if (when (args["subject_type"]) {
                            "all" -> {
                                targetGroup = bot!!.groups.toMutableList(); true
                            }
                            "include" -> {
                                bot!!.groups.forEach { group ->
                                    if (group.id.toString() in groups) targetGroup.add(group)
                                }
                                true
                            }
                            "exclude" -> {
                                bot!!.groups.forEach { group ->
                                    if (group.id.toString() !in groups) targetGroup.add(group)
                                }
                                true
                            }
                            else -> { status = false; error = "Unknown Subject type"; false }
                        }) {
                        val realMessage = when(args["message_type"]) {
                            "xml" -> SimpleServiceMessage(60, args["message"]!!)
                            "plain" -> PlainText(args["message"]!!)
                            else -> MiraiCode.deserializeMiraiCode(args["message"]!!)
                        }
                        targetGroup.forEach { group ->
                            group.sendMessage(realMessage)
                            delay(1000)
                        }
                    }
                }
                call.respondText(Gson().toJson(ApiResult(status, error)))
            }
        }
    }.start(wait = false)
}