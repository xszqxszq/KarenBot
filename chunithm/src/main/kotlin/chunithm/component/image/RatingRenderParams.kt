package xyz.xszq.bot.chunithm.component.image

import xyz.xszq.bot.chunithm.music.Record

data class RatingRenderParams(
    // 基本信息
    val nickname: String,
    val rating: Double,
    val ratingColor: Int,
    // 收藏品
    val avatar: Int,
    val level: Int,
    val plate: Int,
    // 模板参数
    val title: String,
    val oldCount: Int = 30,
    val newCount: Int = 20,
    val isScoreList: Boolean = false,
    // 成绩
    val oldRecords: List<Record>,
    val newRecords: List<Record>,
)