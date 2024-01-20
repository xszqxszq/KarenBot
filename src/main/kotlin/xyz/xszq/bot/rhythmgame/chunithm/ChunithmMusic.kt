package xyz.xszq.bot.rhythmgame.chunithm

import io.github.oshai.kotlinlogging.KLogger
import xyz.xszq.bot.rhythmgame.chunithm.payload.MusicInfo

class ChunithmMusic(val logger: KLogger) {
    private val musics = mutableMapOf<String, MusicInfo>()
    fun updateMusicInfo(data: List<MusicInfo>) {
        musics.putAll(data.associateBy { it.id })
    }
}