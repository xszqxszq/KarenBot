package xyz.xszq.bot.chunithm.component

import xyz.xszq.bot.Chunithm
import xyz.xszq.bot.chunithm.api.ChunithmAPI
import xyz.xszq.bot.chunithm.database.MaimaiSettingsTable
import xyz.xszq.bot.chunithm.database.QQBindTable
import xyz.xszq.bot.chunithm.exception.QQBindRequiredException
import xyz.xszq.bot.chunithm.exception.UserBindRequiredException
import xyz.xszq.bot.chunithm.exception.UserNotFoundException
import xyz.xszq.bot.chunithm.music.PlayerSettings
import xyz.xszq.bot.chunithm.music.RatingResponse
import xyz.xszq.bot.chunithm.music.UserQueryParams
import xyz.xszq.bot.event.MessageEvent

class ChunithmQuery(
    val chunithm: Chunithm
) {
    companion object {
        const val NO_BACKEND_BINDINGS = "您还未在查分器上绑定QQ号，请前往水鱼/落雪查分器设置您的QQ号。"
        const val NO_QQ_BINDINGS = "为了继续后续查询，请输入\"/bind qq号\"绑定您的QQ号："
        const val NO_RECORDS = "在当前筛选条件下未查询到歌曲记录。"
        const val TOO_MANY_RECORDS = "在当前条件下查询到的曲目过多，请缩小范围。"
        const val USER_NOT_FOUND = "您查询的用户不存在。"
        const val USER_DENIED = "您查询的用户设置了查分器隐私或未同意查分器协议，请检查设置。"
        const val USER_EULA = "请先前往查分器同意用户协议再进行查询。"
        const val NEED_AUTHORIZATION = "该功能需要您授权BOT访问您的成绩信息"
        const val QUERY_FAILED = "查询失败，请重试"
    }

    private fun isRetryableError(e: Throwable) = when (e) {
        is UserNotFoundException -> true
        else -> false
    }

    // 获取要查询的目标用户的参数
    suspend fun getQueryParams(
        event: MessageEvent,
        queryArgs: String ?= null
    ): UserQueryParams = when {
        queryArgs.isNullOrBlank() -> {
            val qq = QQBindTable[event.sender.id] ?: throw QQBindRequiredException()
            val settings = MaimaiSettingsTable.settings(event.sender.id)
            UserQueryParams.QQ(qq, event, true, settings)
        }
        queryArgs.startsWith("qq") -> {
            val qq = queryArgs.substringAfter("qq").toLongOrNull()
            qq ?.let {
                UserQueryParams.QQ(qq, event, false)
            } ?: run {
                UserQueryParams.Username(queryArgs, event, false)
            }
        }
        else -> {
            UserQueryParams.Username(queryArgs, event, false)
        }
    }

    // 根据用户设置列出后端
    suspend fun listBackends(
        user: UserQueryParams
    ): List<ChunithmAPI> {
        var backends = listOf(
            chunithm.backend("diving-fish"),
            chunithm.backend("lxns"),
        ).toMutableList()
        if (user.isSelf)
            MaimaiSettingsTable[user.event.sender.id, "prober"] ?.let { prefer ->
                if (prefer.isBlank())
                    return@let
                backends = (backends.filter { it.id == prefer })
                    .toMutableList()
            }
        return backends
    }

    private fun mergeSettings(
        existing: PlayerSettings?,
        userSettings: PlayerSettings?
    ): PlayerSettings? = when {
        userSettings == null -> existing
        existing == null ->
            if (userSettings.avatar == null && userSettings.plate == null) null
            else userSettings
        else -> PlayerSettings(
            avatar = userSettings.avatar ?: existing.avatar,
            plate = userSettings.plate ?: existing.plate
        )
    }

    suspend fun rating(
        user: UserQueryParams
    ): Pair<RatingResponse, ChunithmAPI> {
        val result = listBackends(user).firstNotNullOfOrNull { backend ->
            runCatching {
                backend.getPlayerRating(user)
            }.onFailure { e ->
                if (!isRetryableError(e))
                    throw e
            }.getOrNull() ?.let { Pair(it, backend) }
        } ?: when {
            user is UserQueryParams.Username -> throw UserNotFoundException()
            else -> throw UserBindRequiredException()
        }
        // TODO: 设置表中用中二单独一个前缀
//        result.first.settings = mergeSettings(result.first.settings, user.settings)
        return result
    }
}