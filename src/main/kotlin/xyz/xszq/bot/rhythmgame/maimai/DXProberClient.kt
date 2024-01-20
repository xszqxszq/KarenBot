@file:Suppress("unused")

package xyz.xszq.bot.rhythmgame.maimai

import io.github.oshai.kotlinlogging.KLogger
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import korlibs.io.file.std.localCurrentDirVfs
import kotlinx.serialization.json.*
import xyz.xszq.bot.rhythmgame.DFProberClient
import xyz.xszq.bot.rhythmgame.maimai.MaimaiUtils.acc2rate
import xyz.xszq.bot.rhythmgame.maimai.MaimaiUtils.getNewRa
import xyz.xszq.bot.rhythmgame.maimai.MaimaiUtils.getPlateVerList
import xyz.xszq.bot.rhythmgame.maimai.MaimaiUtils.levelIndex2Label
import xyz.xszq.bot.rhythmgame.maimai.payload.*

class DXProberClient(override val logger: KLogger): DFProberClient(logger) {
    suspend fun getMusicList(): List<MusicInfo> {
        repeat(3) {
            kotlin.runCatching {
                return client.get("$server/api/maimaidxprober/music_data").body()
            }.onFailure {
                logger.error { "获取失败，正在重试中……" }
            }
        }
        return json.decodeFromString(localCurrentDirVfs["maimai/music_data.json"].readString())
    }

    suspend fun getChartStat(): HashMap<String, List<ChartStat>> {
        kotlin.runCatching {
            return client.get("$server/api/maimaidxprober/chart_stats").body<ChartStatResponse>().charts
        }.onFailure {
            logger.error { "数据获取失败" }
        }
        return hashMapOf()
    }

    suspend fun getPlayerData(
        type: String = "qq",
        id: String
    ): Pair<HttpStatusCode, PlayerData?> = getInfo("api/maimaidxprober/query/player", type, id) {
        put("b50", true)
    }

    suspend fun getDataByVersion(
        type: String = "qq",
        id: String,
        versions: List<String>
    ): Pair<HttpStatusCode, PlateResponse?> = getInfo("api/maimaidxprober/query/plate", type, id) {
        put("version", JsonArray(versions.map { JsonPrimitive(it) }))
    }
    suspend fun getPlayerDataManually(
        type: String = "qq",
        id: String
    ): PlayerData? {
        val (code, info) = getInfo<PlayerData>("api/maimaidxprober/query/player", type, id) {
            put("b50", true)
        }
        if (code != HttpStatusCode.OK || info == null)
            return null
        val data = getInfo<PlateResponse>("api/maimaidxprober/query/plate", type, id) {
            put("version", JsonArray(getPlateVerList("all").map { JsonPrimitive(it) }))
        }
        val scores = data.second!!.verList.map {
            val music = Maimai.musics.getById(it.id.toString())!!
            val ds = music.ds[it.levelIndex]
            PlayScore(it.achievements, ds, 0, it.fc, it.fs, it.level, it.levelIndex,
                levelIndex2Label(it.levelIndex), getNewRa(ds, it.achievements), acc2rate(it.achievements),
                it.id, music.title, it.type
            )
        }
        val b35 = scores.filter { !Maimai.musics.getById(it.songId.toString())!!.basicInfo.isNew }
            .sortedBy { -it.ra }.take(35)
        val b15 = scores.filter { Maimai.musics.getById(it.songId.toString())!!.basicInfo.isNew }
            .sortedBy { -it.ra }.take(15)
        val rating = b35.sumOf { it.ra } + b15.sumOf { it.ra }
        return PlayerData(info.nickname, rating, info.additionalRating, info.username, info.plate, buildMap {
            put("sd", b35)
            put("dx", b15)
        })
    }
}