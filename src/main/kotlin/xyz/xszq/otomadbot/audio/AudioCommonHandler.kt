package xyz.xszq.otomadbot.audio

import net.mamoe.mirai.console.permission.PermissionId
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.contact.AudioSupported
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.Audio
import net.mamoe.mirai.message.data.anyIsInstance
import net.mamoe.mirai.message.data.firstIsInstance
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import xyz.xszq.otomadbot.EventHandler
import xyz.xszq.otomadbot.api.PythonApi
import xyz.xszq.otomadbot.core.Cooldown
import xyz.xszq.otomadbot.core.ifReady
import xyz.xszq.otomadbot.core.update
import xyz.xszq.otomadbot.require
import java.io.File


object AudioCommonHandler: EventHandler("语音通用功能", "audio.common") {
    private val allowedSTT by lazy {
        PermissionService.INSTANCE.register(
            PermissionId("otm", "audio.stt"), "允许语音转文字")
    }
    private val cooldown = Cooldown("audio")
    override fun register() {
        GlobalEventChannel.subscribeAlways<MessageEvent> {
            ifReady(cooldown) {
                if (message.anyIsInstance<Audio>()) {
                    require(allowedSTT) {
                        val before = message.firstIsInstance<Audio>().toWav()!!
                        if (before.isFile)
                            before.deleteOnExit()
                        update(cooldown)
                        val resultPath = PythonApi.getScan(before.absolutePath)!!
                        if (resultPath.isNotEmpty()) {
                            val result = File(resultPath)
                            if (result.isFile)
                                result.deleteOnExit()
                            result.toExternalResource().use {
                                subject.sendMessage((subject as AudioSupported).uploadAudio(it))
                            }
                        }
                    }
                }
            }
        }
        allowedSTT
        super.register()
    }
}