package tk.xszq.otomadbot.core

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value

object BinConfig: AutoSavePluginConfig("bin") {
    val ffmpeg by value("")
    val ffmpegPath by value("")
    val python by value("/usr/bin/python3")
    val pitchshift by value("PitchCorrection4Mirai.py")
}