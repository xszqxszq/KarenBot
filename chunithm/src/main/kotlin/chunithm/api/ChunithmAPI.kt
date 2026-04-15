package xyz.xszq.bot.chunithm.api

import xyz.xszq.bot.chunithm.music.*

interface ChunithmAPI {
    val id: String
    val name: String
    suspend fun load()
    suspend fun getPlayerRating(user: UserQueryParams): RatingResponse?
    suspend fun getPlayerRecord(user: UserQueryParams, music: MusicInfo): List<Record>?
    suspend fun getPlayerRecords(user: UserQueryParams, musics: List<MusicInfo>): RecordsResponse?
}