package xyz.xszq.bot.meme.payload

import kotlinx.serialization.Serializable

/**
 * 图片上传请求
 *
 * @property type 图片类型：`url` / `path` / `data`
 *
 */
@Serializable
data class MemeUpload(
    val type: String,
    val url: String ?= null,
    val path: String ?= null,
    val data: String ?= null,
)