package xyz.xszq.bot.rhythmgame

import LXNSConfig
import korlibs.io.file.std.localCurrentDirVfs
import xyz.xszq.bot.rhythmgame.maimai.Maimai

object RhythmGame {
    lateinit var lxnsConfig: LXNSConfig
    suspend fun init() {
        lxnsConfig = LXNSConfig.load(localCurrentDirVfs["lxns.yml"])
        Maimai.init()
    }
    fun subscribe() {
        Maimai.subscribe()
    }
}