package xyz.xszq.bot.rhythmgame.chunithm

import io.github.oshai.kotlinlogging.KLogger
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import korlibs.io.file.std.localCurrentDirVfs
import xyz.xszq.bot.rhythmgame.DFProberClient
import xyz.xszq.bot.rhythmgame.chunithm.payload.MusicInfo
import xyz.xszq.bot.rhythmgame.chunithm.payload.PlayerData

class ChunithmProberClient(override val logger: KLogger): DFProberClient(logger) {
    suspend fun getMusicList(): List<MusicInfo> {
        repeat(3) {
            kotlin.runCatching {
                return client.get("$server/api/chunithmprober/music_data").body()
            }.onFailure {
                logger.error { "获取失败，正在重试中……" }
            }
        }
        return json.decodeFromString(localCurrentDirVfs["chunithm/music_data.json"].readString())
    }
    suspend fun getPlayerData(
        type: String = "qq",
        id: String
    ): Pair<HttpStatusCode, PlayerData?> = getInfo("api/chunithmprober/query/player", type, id) {
    }
}