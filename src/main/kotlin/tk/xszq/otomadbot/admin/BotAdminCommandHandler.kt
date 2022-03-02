package tk.xszq.otomadbot.admin

import kotlinx.coroutines.delay
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.code.MiraiCode.deserializeMiraiCode
import net.mamoe.mirai.message.data.SimpleServiceMessage
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.message.nextMessageOrNull
import tk.xszq.otomadbot.AdminEventHandler
import tk.xszq.otomadbot.core.OtomadBotCore
import tk.xszq.otomadbot.equalsTo
import tk.xszq.otomadbot.quoteReply
import tk.xszq.otomadbot.requireBotAdmin
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.NetworkInterface
import java.util.*


object BotAdminCommandHandler: AdminEventHandler() {
    override fun register() {
        GlobalEventChannel.subscribeMessages {
            equalsTo("/ip") {
                requireBotAdmin {
                    var ips = ""
                    getNetworkInterfaces().toList().forEach {
                        it.interfaceAddresses.forEach { ifa ->
                            ips += it.displayName + ": " + ifa.address.hostAddress + "\n"
                        }
                    }
                    quoteReply(ips)
                }
            }
            startsWith("/exec") { command ->
                requireBotAdmin {
                    val result = handleExec(command)
                    if (result.first.isNotEmpty())
                        quoteReply(result.first)
                    if (result.second.isNotEmpty())
                        quoteReply(result.second)
                }
            }
            equalsTo("/reload") {
                requireBotAdmin {
                    try {
                        OtomadBotCore.doReload()
                    } catch (e: Exception) {
                        quoteReply(e.stackTraceToString())
                        return@requireBotAdmin
                    }
                    quoteReply("重载成功")
                }
            }
            equalsTo("/test") {
                requireBotAdmin {
                    subject.sendMessage(SimpleServiceMessage(140,
                        "<?xml version='1.0' encoding='UTF-8' standalone='yes' ?><msg serviceID=\"60\" templateID=\"123\" action=\"web\" brief=\".*\" sourceMsgId=\"0\" url=\"\" flag=\"0\" adverSign=\"0\" multiMsgFlag=\"0\"><item layout=\"1\" advertiser_id=\"0\" aid=\"0\" /><item layout=\"1\" advertiser_id=\"0\" aid=\"0\"><summary size=\"×FF0000\">.*</summary></item><source name=\"\" icon=\"\" action=\"\" appid=\"-1\" /></msg>"))
                }
            }
            startsWith("/show") { raw ->
                requireBotAdmin {
                    subject.sendMessage(raw.deserializeMiraiCode())
                }
            }
            startsWith("/clean") { arg ->
                val limit = arg.toLong()
                quoteReply("有无无需清理的群？(n)")
                nextMessageOrNull(120000) ?.let { raw ->
                    val exclusion = raw.content.split(" ")
                    val target = bot.groups.filter { it.members.size < limit && it.id.toString() !in exclusion }
                    var confirm = "以下群将主动退出，确认？（y/n）"
                    target.forEach { confirm += "\n${it.id}. ${it.name} (${it.members.size} 人)"}
                    quoteReply(confirm)
                    nextMessageOrNull(120000) ?.let { ans ->
                        if (ans.content.lowercase() == "y") {
                            quoteReply("正在退出中……")
                            var counter = 0
                            var cycle = 0
                            target.forEach {
                                kotlin.runCatching {
                                    delay(1000)
                                    it.quit()
                                    counter += 1
                                    cycle = (cycle + 1) % 8
                                } .onFailure {
                                    quoteReply("Unknown exception: " + it.stackTraceToString())
                                }
                                if (cycle == 0) {
                                    quoteReply("$counter / ${target.size}")
                                }
                            }
                            quoteReply("操作完毕，已退出 ${target.size} 个群。")
                        }
                    }
                }
            }
        }
        super.register()
    }
    private fun getNetworkInterfaces(): Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
    private fun handleExec(command: String): Pair<String, String> {
        val rt = Runtime.getRuntime()
        val commands = arrayOf("/bin/bash", "-c", command) // TODO: Support Windows
        val proc = rt.exec(commands)
        val stdInput = BufferedReader(InputStreamReader(proc.inputStream))
        val stdError = BufferedReader(InputStreamReader(proc.errorStream))
        var tmpBuff: String?
        var stdout = ""
        var stderr = ""
        while (stdInput.readLine().also { tmpBuff = it } != null) {
            stdout += tmpBuff + "\n"
        }
        while (stdError.readLine().also { tmpBuff = it } != null) {
            stderr += tmpBuff + "\n"
        }
        return Pair(stdout, stderr)
    }
}