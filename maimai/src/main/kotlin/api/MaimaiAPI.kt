package xyz.xszq.bot.api

import xyz.xszq.bot.music.*

interface MaimaiAPI {
    val id: String
    val name: String
    suspend fun load()
    suspend fun getPlayerRating(user: UserQueryParams): RatingResponse?
    suspend fun getPlayerRecord(user: UserQueryParams, music: MusicInfo): List<Record>?
    suspend fun getPlayerRecords(user: UserQueryParams, musics: List<MusicInfo>): RecordsResponse?
}