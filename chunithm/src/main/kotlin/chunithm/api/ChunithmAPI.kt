package xyz.xszq.bot.chunithm.api

import xyz.xszq.bot.chunithm.music.*

/**
 * 查分器的后端接口
 *
 * @property id 查分器 ID
 * @property name 查分器名称
 */
@Suppress("EmptyMethod")
interface ChunithmAPI {
    val id: String
    val name: String

    /**
     * 初始化后端
     */
    suspend fun load()

    /**
     * 查询玩家的 Best 50
     *
     * @param user 玩家
     * @return Rating 查询结果
     */
    suspend fun getPlayerRating(user: UserQueryParams): RatingResponse?

    /**
     * 查询玩家的某一首歌的成绩
     *
     * @param user 玩家
     * @param music 歌曲
     * @return 各难度成绩记录
     */
    suspend fun getPlayerRecord(user: UserQueryParams, music: MusicInfo): List<Record>?

    /**
     * 查询玩家的某些歌曲的成绩
     *
     * @param user 玩家
     * @param musics 歌曲列表
     * @return 查询结果
     */
    suspend fun getPlayerRecords(user: UserQueryParams, musics: List<MusicInfo>): RecordsResponse?
}