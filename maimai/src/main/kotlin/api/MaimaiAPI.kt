package xyz.xszq.bot.api

import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.music.*

interface MaimaiAPI {
    val name: String
    suspend fun load()
    suspend fun getMusicList(): Map<Int, MusicInfo>
    suspend fun getGameVersions(): Map<String, GameVersion>
    suspend fun getPlayerRating(event: MessageEvent, args: String): RatingResponse?
    suspend fun getPlayerRecord(event: MessageEvent, args: String, music: MusicInfo): List<Record>?
    suspend fun getPlayerRecords(event: MessageEvent, args: String, musics: List<MusicInfo>): RecordsResponse?
}