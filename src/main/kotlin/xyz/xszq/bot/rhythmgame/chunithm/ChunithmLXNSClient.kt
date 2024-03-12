package xyz.xszq.bot.rhythmgame.chunithm

import io.github.oshai.kotlinlogging.KLogger
import xyz.xszq.bot.rhythmgame.LXNSProberClient
import xyz.xszq.bot.rhythmgame.lxns.payload.Song

class ChunithmLXNSClient(override val logger: KLogger) : LXNSProberClient(logger) {
    val clientType = "chunithm"
    suspend fun getSongList() = getInfo<List<Song>>(clientType, "song/list")
}