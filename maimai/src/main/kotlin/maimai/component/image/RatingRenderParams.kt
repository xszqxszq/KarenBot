package xyz.xszq.bot.maimai.component.image

import xyz.xszq.bot.maimai.music.Record

data class RatingRenderParams(
    // 基本信息
    val nickname: String,
    val rating: Int,
    val ratingColor: Int,
    // 收藏品
    val avatar: Int,
    val course: Int,
    val plate: Int,
    // 条件查询参数
    val filter: FilterParams ?= null,
    // 模板参数
    val title: String,
    val oldCount: Int = 35,
    val newCount: Int = 15,
    val isNewDisabled: Boolean = false,
    val isScoreList: Boolean = false,
    // 成绩
    val oldRecords: List<Record>,
    val newRecords: List<Record>,
)