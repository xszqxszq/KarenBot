package tk.xszq.otomadbot.audio

import net.mamoe.mirai.console.permission.PermissionId
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.contact.AudioSupported
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.Audio
import net.mamoe.mirai.message.data.OnlineAudio
import net.mamoe.mirai.message.data.anyIsInstance
import net.mamoe.mirai.message.data.firstIsInstance
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import tk.xszq.otomadbot.EventHandler
import tk.xszq.otomadbot.NetworkUtils
import tk.xszq.otomadbot.api.PythonApi
import tk.xszq.otomadbot.require
import java.io.File


object AudioCommonHandler: EventHandler("语音通用功能", "audio.common") {
    private val allowedSTT by lazy {
        PermissionService.INSTANCE.register(
            PermissionId("otm", "audio.stt"), "允许语音转文字")
    }
    override fun register() {
        GlobalEventChannel.subscribeAlways<MessageEvent> {
            if (message.anyIsInstance<Audio>()) {
                require(allowedSTT) {
                    val raw = NetworkUtils
                        .downloadTempFile(
                            (message.firstIsInstance<Audio>() as OnlineAudio).urlForDownload, ext="silk")!!
                    val before = AudioEncodeUtils.silkToMp3(raw)
                    val resultPath = PythonApi.getScan(before.absolutePath)!!
                    if (resultPath.isNotEmpty()) {
                        val resultRaw = File(resultPath)
                        val result = AudioEncodeUtils.mp3ToSilk(resultRaw)
                        result.toExternalResource().use {
                            subject.sendMessage((subject as AudioSupported).uploadAudio(it))
                        }
                        if (result.isFile)
                            result.delete()
                        if (resultRaw.isFile)
                            resultRaw.delete()
                    }
                    if (raw.isFile)
                        raw.delete()
                    if (before.isFile)
                        before.delete()
                }
            }
        }
        allowedSTT
        super.register()
    }
}