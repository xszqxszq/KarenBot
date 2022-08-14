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
import xyz.xszq.otomadbot.CommandEvent
import xyz.xszq.otomadbot.CommandModule
import xyz.xszq.otomadbot.CommonCommand
import xyz.xszq.otomadbot.CommonCommandWithArgs
import xyz.xszq.otomadbot.mirai.equalsTo
import xyz.xszq.otomadbot.mirai.quoteReply
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.NetworkInterface
import java.util.*


object Admin: CommandModule("管理员指令", "admin") {
    override suspend fun subscribe() {
        events.subscribeMessages {
            equalsTo("/ip") {
                ip.checkAndRun(this)
            }
            startsWith("/exec") { command ->
                exec.checkAndRun(CommandEvent(listOf(command), this))
            }
            equalsTo("/reload") {
                reload.checkAndRun(this)
            }
            startsWith("/show") { raw ->
                show.checkAndRun(CommandEvent(listOf(raw), this))
            }
            startsWith("/clean") { arg ->
                clean.checkAndRun(CommandEvent(listOf(arg), this))
            }
        }
    }
    val botAdmin = allowPerm
    val ip = CommonCommand("", "ip", false, checkSender = true) {
        var ips = ""
        getNetworkInterfaces().toList().forEach {
            it.interfaceAddresses.forEach { ifa ->
                ips += it.displayName + ": " + ifa.address.hostAddress + "\n"
            }
        }
        quoteReply(ips)
    }
    val exec = CommonCommandWithArgs("", "exec", false, checkSender = true) {
        val result = handleExec(args.first())
        if (result.first.isNotEmpty())
            event.quoteReply(result.first)
        if (result.second.isNotEmpty())
            event.quoteReply(result.second)
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
    val show = CommonCommandWithArgs("", "show", false, checkSender = true) {
        event.subject.sendMessage(args.first().deserializeMiraiCode())
    }
    val clean = CommonCommandWithArgs("", "clean", false, checkSender = true) {
        val limit = args.first().toLong()
        event.quoteReply("有无无需清理的群？(n)")
        event.nextMessageOrNull(120000) ?.let { raw ->
            val exclusion = raw.content.split(" ")
            val target = event.bot.groups.filter { it.members.size < limit && it.id.toString() !in exclusion
                    && it.members.none { m -> m.permitteeId.hasPermission(botAdmin) } }
            var confirm = "以下群将主动退出，确认？（y/n）"
            target.forEach { confirm += "\n${it.id}. ${it.name} (${it.members.size} 人)"}
            event.quoteReply(confirm)
            event.nextMessageOrNull(120000) ?.let { ans ->
                if (ans.content.lowercase() == "y") {
                    event.quoteReply("正在退出中……")
                    var counter = 0
                    var cycle = 0
                    target.forEach {
                        kotlin.runCatching {
                            delay(1000)
                            it.quit()
                            counter += 1
                            cycle = (cycle + 1) % 8
                        } .onFailure {
                            event.quoteReply("Unknown exception: " + it.stackTraceToString())
                        }
                        if (cycle == 0) {
                            event.quoteReply("$counter / ${target.size}")
                        }
                    }
                    event.quoteReply("操作完毕，已退出 ${target.size} 个群。")
                }
            }
        }
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