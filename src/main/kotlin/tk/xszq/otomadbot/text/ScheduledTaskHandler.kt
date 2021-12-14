@file:Suppress("MemberVisibilityCanBePrivate", "EXPERIMENTAL_API_USAGE")

package tk.xszq.otomadbot.text

import com.soywiz.korio.util.UUID
import dev.inmo.krontab.doWhile
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.code.MiraiCode.deserializeMiraiCode
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.nextMessage
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import tk.xszq.otomadbot.*
import tk.xszq.otomadbot.admin.isAdminCommandPermitted
import tk.xszq.otomadbot.core.OtomadBotCore
import java.io.File
import kotlin.collections.set

@Serializable
class GroupMessageTask(val groupId: Long, val time: String, val type: Int, val contentType: Int)

const val TASK_RANDOM = 0
const val TASK_QUEUE = 1
const val CONTENT_TEXT = 0
const val CONTENT_IMAGE = 1

object ScheduledMessageData: AutoSavePluginData("task") {
    var resourceList: Map<Long, MutableList<String>> by value()
    var groupTask: Map<Long, GroupMessageTask> by value()
    fun random(id: Long): String? = resourceList[id] ?.randomOrNull()
    fun queuePush(id: Long, content: String) {
        val tmp = resourceList.toMutableMap()
        if (!tmp.containsKey(id)) tmp[id] = mutableListOf()
        tmp[id]!!.add(content)
        resourceList = tmp
    }
    fun queuePull(id: Long): String? {
        if (!resourceList.containsKey(id) || resourceList[id]!!.isEmpty()) {
            return null
        }
        val result = resourceList[id] ?.first()
        val tmp = resourceList.toMutableMap()
        tmp[id] ?.removeFirst()
        resourceList = tmp
        return result
    }
    fun rmTask(task: GroupMessageTask) {
        val tmp = groupTask.toMutableMap()
        tmp.remove(task.groupId)
        groupTask = tmp
    }
    fun newTask(groupId: Long, time: String, type: Int, contentType: Int): GroupMessageTask {
        val tmp = groupTask.toMutableMap()
        val result = GroupMessageTask(groupId, time, type, contentType)
        tmp[groupId] = result
        groupTask = tmp
        return result
    }
}


object ScheduledTaskHandler: EventHandler("定时消息", "task", HandlerType.DEFAULT_DISABLED) {
    override fun register() {
        ScheduledMessageData.groupTask.forEach { (_, task) ->
            registerTask(task)
        }
        GlobalEventChannel.subscribeMessages {
            startsWithSimple("定时消息") { rawArgs, _ ->
                val args = rawArgs.toArgsList()
                args.firstOrNull()?.let { operation ->
                    when (operation) {
                        "启用" -> {
                            askNewTarget(this)?.let { target ->
                                quoteReply("请选择定时消息内容类型（回复序号）：\n  1.文本（默认）\n  2.图片")
                                val contentType = when (nextMessage().contentToString().toInt()) {
                                    1 -> CONTENT_TEXT
                                    2 -> CONTENT_IMAGE
                                    else -> CONTENT_TEXT
                                }
                                subject.sendMessage("请选择定时消息发送类型（回复序号）：\n" +
                                        "  1.随机（可重复发送）\n" +
                                        "  2.按内容添加顺序依次发送直到无内容可发（默认）")
                                val type = when (nextMessage().contentToString().toInt()) {
                                    1 -> TASK_RANDOM
                                    2 -> TASK_QUEUE
                                    else -> TASK_QUEUE
                                }
                                subject.sendMessage("请发送定时消息的cron表达式：\n  例如每小时0分0秒时发一条为“0 0 * * *”")
                                registerTask(
                                    ScheduledMessageData.newTask(target.id, nextMessage().contentToString(), type,
                                        contentType)
                                )
                                quoteReply("启用成功")
                            }
                        }
                        "禁用" -> {
                            askTarget(this)?.let { task ->
                                ScheduledMessageData.rmTask(task)
                                quoteReply("禁用成功")
                            }
                        }
                        "内容补充" -> {
                            askTarget(this)?.let { task ->
                                subject.sendMessage("请发送欲添加的内容：")
                                val new = nextMessage()
                                when (task.contentType) {
                                    CONTENT_TEXT -> {
                                        ScheduledMessageData.queuePush(task.groupId, new.serializeToMiraiCode())
                                        subject.sendMessage("已成功添加一条文本信息。")
                                    }
                                    CONTENT_IMAGE -> {
                                        var succeedNum = 0
                                        new.filterIsInstance<Image>().forEach { img ->
                                            try {
                                                ScheduledMessageData.queuePush(
                                                    task.groupId,
                                                    NetworkUtils.downloadFile(
                                                        img.queryUrl(),
                                                        OtomadBotCore.configFolder.resolve("image/message"),
                                                        UUID.randomUUID().toString()
                                                    )!!.absolutePath
                                                )
                                            } catch (e: Exception) {
                                                subject.sendMessage(e.stackTraceToString())
                                            }
                                            succeedNum += 1
                                        }
                                        subject.sendMessage("已成功添加 $succeedNum 张图片。")
                                    }
                                }
                                pass
                            }
                        }
                        "内容撤销" -> {
                            askTarget(this)?.let {
                                ScheduledMessageData.queuePull(it.groupId) ?.let {
                                    subject.sendMessage("已成功撤销最后添加的一条内容。")
                                } ?: run {
                                    subject.sendMessage("好像定时消息列表里还没有内容哦~")
                                }
                            }
                        }
                        else -> quoteReply("未知操作")
                    }
                    pass
                } ?: run {
                    quoteReply("使用方法：\n  定时消息 启用\n  定时消息 禁用\n  定时消息 内容补充\n  定时消息 内容撤销")
                }
            }
        }
        super.register()
    }
    suspend fun askTarget(event: MessageEvent) = event.run {
        val targets = ScheduledMessageData.groupTask.values.filter { task -> bot.groups.find {
            it.id == task.groupId }!!.members.find { it.id == sender.id } ?.isAdminCommandPermitted() ?: false }
        return@run when {
            targets.isEmpty() -> null
            targets.size == 1 -> targets.first()
            else -> {
                var selectText = ""
                targets.forEachIndexed { index, selTask ->
                    val group = bot.groups.find { it.id == selTask.groupId }
                    selectText += "\n $index. ${group!!.name} (${group.id})"
                }
                quoteReply("请选择目标群编号（回复其他内容取消操作）：$selectText")
                targets.getOrNull(nextMessage().contentToString().toIntOrNull() ?: -1)
            }
        }
    }
    suspend fun askNewTarget(event: MessageEvent) = event.run {
        val targets = bot.groups.filter { group -> group.members.find { it.id == sender.id } ?.isAdminCommandPermitted() ?: false }
        return@run when {
            targets.isEmpty() -> null
            targets.size == 1 -> targets.first()
            else -> {
                var selectText = ""
                targets.forEachIndexed { index, group ->
                    selectText += "\n $index. ${group.name} (${group.id})"
                }
                quoteReply("请选择目标群编号（回复其他内容取消操作）：$selectText")
                targets.getOrNull(nextMessage().contentToString().toIntOrNull() ?: -1)
            }
        }
    }
    fun registerTask(task: GroupMessageTask) {
        GlobalScope.launch {
            doWhile(task.time) {
                try {
                    val content = when (task.type) {
                        TASK_QUEUE -> ScheduledMessageData.queuePull(task.groupId)
                        TASK_RANDOM -> ScheduledMessageData.random(task.groupId)
                        else -> null
                    }
                    content?.let { raw ->
                        val group = OtomadBotCore.bot.groups.find { group -> group.id == task.groupId }
                        group?.sendMessage(
                            when (task.contentType) {
                                CONTENT_IMAGE -> {
                                    File(raw).toExternalResource().use {
                                        group.uploadImage(it)
                                    }
                                }
                                CONTENT_TEXT -> raw.deserializeMiraiCode()
                                else -> PlainText("")
                            })
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                ScheduledMessageData.groupTask.containsKey(task.groupId)
            }
        }
    }
}