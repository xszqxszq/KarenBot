package xyz.xszq.otomadbot.admin

import kotlinx.coroutines.delay
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.permission.PermitteeId.Companion.permitteeId
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.code.MiraiCode.deserializeMiraiCode
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.message.nextMessageOrNull
import xyz.xszq.OtomadBotCore
import xyz.xszq.events
import xyz.xszq.otomadbot.CommandModule
import xyz.xszq.otomadbot.CommonCommand
import xyz.xszq.otomadbot.CommonCommandWithArg
import xyz.xszq.otomadbot.CommonCommandWithArgOf
import xyz.xszq.otomadbot.mirai.equalsTo
import xyz.xszq.otomadbot.mirai.quoteReply
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.NetworkInterface
import java.util.*


object Admin: CommandModule("", "admin") {
    override suspend fun subscribe() {
        events.subscribeMessages {
            equalsTo("/ip") {
                ip.checkAndRun(this)
            }
            startsWith("/exec") { command ->
                exec.checkAndRun(this, command)
            }
            equalsTo("/reload") {
                reload.checkAndRun(this)
            }
            startsWith("/show") { raw ->
                show.checkAndRun(this, raw)
            }
//            equalsTo("/muted") {
//                showMuted.checkAndRun(this)
//            }
            startsWith("/clean") { limit ->
                clean.checkAndRun(this, limit.toLong())
            }
//            startsWith("/stat") {
//                showTop10.checkAndRun(this)
//            }
        }
    }
    val ip = CommonCommand("", "ip", false, checkSender = true) {
        var ips = ""
        getNetworkInterfaces().toList().forEach {
            it.interfaceAddresses.forEach { ifa ->
                ips += it.displayName + ": " + ifa.address.hostAddress + "\n"
            }
        }
        quoteReply(ips)
    }
    val exec = CommonCommandWithArg("", "exec", false, checkSender = true) { cmd ->
        val result = handleExec(cmd!!)
        if (result.first.isNotEmpty())
            quoteReply(result.first)
        if (result.second.isNotEmpty())
            quoteReply(result.second)
    }
    val reload = CommonCommand("", "reload", false, checkSender = true) {
        try {
            OtomadBotCore.doReload()
        } catch (e: Exception) {
            quoteReply(e.stackTraceToString())
            return@CommonCommand
        }
        quoteReply("重载成功")
    }
    val show = CommonCommandWithArg("", "show", false, checkSender = true) { raw ->
        subject.sendMessage(raw!!.deserializeMiraiCode())
    }
//    val showMuted = CommonCommand("", "showMuted", false, checkSender = true) {
//        quoteReply(OtomadBotCore.validator.bots.map { b ->
//            b.groups.filter { it.botAsMember.isMuted }.map { "${it.name}(${it.id})" }
//        }.flatten().joinToString("\n"))
//    }
    val clean = CommonCommandWithArgOf<Long>("", "clean", false, checkSender = true) { limit ->
        quoteReply("有无无需清理的群？(n)")
        nextMessageOrNull(120000) ?.let { raw ->
            val exclusion = raw.content.split(" ")
            val target = bot.groups.filter { it.members.size < limit!! && it.id.toString() !in exclusion
                    && it.members.none { m -> m.permitteeId.hasPermission(allowPerm) } }
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
//    val showTop10 = CommonCommand("", "showTop10", false, checkSender = true) {
//        quoteReply(Statistics.frequency.toList().sortedBy { it.second }.take(10).joinToString("\n") {
//            "${it.first}: ${it.second}"
//        })
//    }
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