package xyz.xszq.bot.payload

/**
 * 媒体上传结果
 *
 * @property response 服务端响应
 * @property filename 远端文件名
 */
class MediaUpload(
    val response: FileResponse,
    val filename: String
)