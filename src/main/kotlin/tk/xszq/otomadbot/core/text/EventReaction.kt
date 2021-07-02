@file:Suppress("unused", "MemberVisibilityCanBePrivate")
package tk.xszq.otomadbot.core.text

import kotlinx.serialization.Serializable
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ReadOnlyPluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.permission.PermissionId
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MemberJoinEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.events.NudgeEvent
import net.mamoe.mirai.message.data.content
import tk.xszq.otomadbot.core.Counter
import tk.xszq.otomadbot.core.requireNot

open class EventHandler(val funcName: String, val permName: String) {
    val denied by lazy {
        PermissionService.INSTANCE.register(
            PermissionId("otm", "deny.$permName"), "禁用$funcName")
    }
    open fun register() { denied }
}
object WelcomeHandler: EventHandler("欢迎消息", "welcome") {
    override fun register() {
        super.register()
        GlobalEventChannel.subscribeAlways<MemberJoinEvent> { event ->
            requireNot(WelcomeHandler.denied) {
                group.sendMessage(
                    when (event) {
                        is MemberJoinEvent.Invite -> TextSettings.welcome.invite
                        is MemberJoinEvent.Active -> TextSettings.welcome.active
                        is MemberJoinEvent.Retrieve -> TextSettings.welcome.retrieve
                        else -> ""
                    }
                )
            }
        }
    }
}
object Repeater: EventHandler("重复草字", "kusa") {
    val kusa = Counter()
    override fun register() {
        super.register()
        GlobalEventChannel.subscribeAlways<MessageEvent> {
            requireNot(denied) {
                if (message.content == "草") {
                    kusa.increase(subject)
                    if (kusa.get(subject) == 3L) {
                        subject.sendMessage("草")
                    }
                } else {
                    kusa.reset(subject)
                }
            }
        }
    }
}
object NudgeBounce: EventHandler("戳一戳回弹", "nudge.bounce") {
    override fun register() {
        super.register()
        GlobalEventChannel.subscribeAlways<NudgeEvent> {
            requireNot(denied) {
                if (target is Bot && from.id != target.id) {
                    from.nudge().sendTo(subject)
                }
            }
        }
    }
}
@Serializable
class WelcomeTextSettings {
    var invite = "(๑•̀ㅂ•́)و✧"
    var active = "(o゜▽゜)o☆"
    var retrieve = "(つ´∀｀)つ"
}
@Serializable
class RegexSettings {
    var midishow = "(?:.*(?:有无|有|发一下|发给我|发我|给我|发|找一下|找找|找|球球|求求|求|我想要|我要|要)(.*)的(?i)MID.*|" +
            "^(?:(?i)MIDI搜索|搜索(?i)MIDI)(.*))"
}

object TextSettings : AutoSavePluginConfig("text") {
    val welcome by value(WelcomeTextSettings())
    val regex by value(RegexSettings())
}