package xyz.xszq.bot.rhythmgame.lxns.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Score(
    val id: Int,
    @SerialName("song_name")
    val songName: String,
    val level: String,
    @SerialName("level_index")
    val levelIndex: LevelIndex,
    val score: Int,
    val rating: Double,
    val clear: ClearType,
    @SerialName("full_combo")
    val fullCombo: FullComboType,
    @SerialName("full_chain")
    val fullChain: FullChainType,
    val rank: RankType,
    @SerialName("play_time")
    val playTime: String,
    @SerialName("upload_time")
    val uploadTime: String
)
