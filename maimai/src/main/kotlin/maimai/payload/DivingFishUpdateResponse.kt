package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.Serializable

/**
 * 水鱼游玩记录导入响应
 *
 * @property creates 新建的记录数
 * @property updates 更新的记录数
 * @property message 接口返回的提示信息
 */
@Serializable
data class DivingFishUpdateResponse(
    val creates: Int,
    val message: String,
    val updates: Int
)