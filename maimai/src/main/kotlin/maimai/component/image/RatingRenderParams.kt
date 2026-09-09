package xyz.xszq.bot.maimai.component.image

import xyz.xszq.bot.maimai.music.Record

/**
 * Best 50 / 40 成绩图渲染参数
 *
 * @property ratingColor Rating 颜色编号
 * @property avatar 头像 ID
 * @property course 段位 ID
 * @property plate 牌子 ID
 * @property filter 随心配渲染参数
 * @property isNewDisabled 是否禁用 New 15
 * @property isScoreList 是否为分数列表
 */
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