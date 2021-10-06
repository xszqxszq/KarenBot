package tk.xszq.otomadbot.admin

import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.subscribeMessages
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