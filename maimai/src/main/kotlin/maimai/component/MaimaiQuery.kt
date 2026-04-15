package xyz.xszq.bot.maimai.component

import xyz.xszq.bot.Maimai
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.maimai.api.LXNS
import xyz.xszq.bot.maimai.api.MaimaiAPI
import xyz.xszq.bot.maimai.database.MaimaiSettingsTable
import xyz.xszq.bot.maimai.database.QQBindTable
import xyz.xszq.bot.maimai.exception.NotSupportedException
import xyz.xszq.bot.maimai.exception.QQBindRequiredException
import xyz.xszq.bot.maimai.exception.UserBindRequiredException
import xyz.xszq.bot.maimai.exception.UserNotFoundException
import xyz.xszq.bot.maimai.music.*

class MaimaiQuery(
    val maimai: Maimai
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
        user: UserQueryParams,
        listAll: Boolean = false
    ): List<MaimaiAPI> {
        var backends = listOf(
            maimai.backend("diving-fish"),
            maimai.backend("lxns"),
        ).toMutableList()
        if (user.isSelf && !listAll)
            MaimaiSettingsTable[user.event.sender.id, "prober"] ?.let { prefer ->
                if (prefer.isBlank())
                    return@let
                // TODO: 还是按这样查但是解决一下会抛出落雪查分器OA提示的问题
//                backends = ((backends.filter { it.id == prefer }) + backends.filter { it.id != prefer })
//                    .toMutableList()
                backends = backends.filter { it.id == prefer }.toMutableList()
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
    ): Pair<RatingResponse, MaimaiAPI> {
        if (user.isMaxScore())
            return Pair(maxScoreRating(), listBackends(user).first())
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
        result.first.settings = mergeSettings(result.first.settings, user.settings)
        return result
    }

    suspend fun records(
        user: UserQueryParams,
        musics: List<MusicInfo>
    ): Pair<RecordsResponse, MaimaiAPI> {
        val result = listBackends(user).firstNotNullOfOrNull { backend ->
            runCatching {
                backend.getPlayerRecords(user, musics)
            }.onFailure { e ->
                if (!isRetryableError(e))
                    throw e
            }.getOrNull() ?.let { Pair(it, backend) }
        } ?: when {
            user is UserQueryParams.Username -> throw UserNotFoundException()
            else -> throw UserBindRequiredException()
        }
        result.first.settings = mergeSettings(result.first.settings, user.settings)
        return result
    }

    suspend fun record(
        user: UserQueryParams,
        music: MusicInfo
    ): List<Record> {
        val result = listBackends(user).firstNotNullOfOrNull { backend ->
            runCatching {
                backend.getPlayerRecord(user, music)
            }.onFailure { e ->
                if (!isRetryableError(e))
                    throw e
            }.getOrNull()
        } ?: when {
            user is UserQueryParams.Username -> throw UserNotFoundException()
            else -> throw UserBindRequiredException()
        }
        return result
    }
    suspend fun recent(
        user: UserQueryParams,
    ): Pair<RecordsResponse, MaimaiAPI> {
        val backend = listBackends(user).filterIsInstance<LXNS>().firstOrNull()
            ?: throw NotSupportedException("该功能仅支持落雪查分器")
        val response = runCatching {
            backend.getPlayerRecent(user)
        }.onFailure { e ->
            if (!isRetryableError(e))
                throw e
        }.getOrNull() ?: when {
            user is UserQueryParams.Username -> throw UserNotFoundException()
            else -> throw UserBindRequiredException()
        }
        response.settings = mergeSettings(response.settings, user.settings)
        return Pair(response, backend)
    }

    private fun UserQueryParams.isMaxScore(): Boolean {
        if (this !is UserQueryParams.Username)
            return false
        return username.lowercase() in listOf("maxscore", "理论", "理论值")
    }
    private fun maxScoreRecords(): List<Record> = maimai.musics().flatMap {
        it.charts
    }.map { chart ->
        Record(
            music = chart.music,
            chart = chart,
            achievement = 1010000,
            comboStatus = ComboStatus.AllPerfectPlus,
            syncStatus = SyncStatus.FullSyncDeluxePlus,
            deluxeScore = chart.maxDeluxeScore,
            rate = "sssp",
            rating = Rating.calc(chart, 1010000)
        )
    }.sortedByDescending { it.rating }
    private fun maxScoreRating(): RatingResponse {
        val scores = maxScoreRecords()
        val old = scores.filter { !it.music.isNew }.take(35)
        val new = scores.filter { it.music.isNew }.take(15)
        val rating = old.sumOf { it.rating } + new.sumOf { it.rating }
        return RatingResponse(
            player = PlayerInfo("理论值", rating, 23),
            oldRatingList = old,
            newRatingList = new,
        )
    }
}