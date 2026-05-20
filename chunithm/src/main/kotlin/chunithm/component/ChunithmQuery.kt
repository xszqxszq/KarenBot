package xyz.xszq.bot.chunithm.component

import kotlinx.coroutines.CancellationException
import xyz.xszq.bot.Chunithm
import xyz.xszq.bot.chunithm.api.ChunithmAPI
import xyz.xszq.bot.chunithm.api.LXNS
import xyz.xszq.bot.chunithm.database.MaimaiSettingsTable
import xyz.xszq.bot.chunithm.database.QQBindTable
import xyz.xszq.bot.chunithm.exception.*
import xyz.xszq.bot.chunithm.music.*
import xyz.xszq.bot.chunithm.music.Rating.ratingFloor
import xyz.xszq.bot.event.GroupMessageEvent
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
        const val NEED_AUTHORIZATION = "该功能需要您在查分器授权BOT访问您的成绩信息"
        const val QUERY_FAILED = "查询失败，请重试"

        private val queryExceptionOrder = listOf(
            FilterNoResultException::class.java,
            FilterTooManyException::class.java,
            QQBindRequiredException::class.java,
            UserDeniedException::class.java,
            AuthorizationException::class.java,
            UserOARequiredException::class.java,
            NoDataException::class.java,
            UserNotFoundException::class.java,
            UserBindRequiredException::class.java,
            NotSupportedException::class.java,
            UnknownException::class.java,
        )
    }

    // 获取要查询的目标用户的参数
    suspend fun getQueryParams(
        event: MessageEvent,
        queryArgs: String ?= null
    ): UserQueryParams = when {
        event is GroupMessageEvent && event.mentions.isNotEmpty() && event.mentions.none { it.isBot || it.isSelf } -> {
            val mentioned = event.mentions.first()
            val qq = QQBindTable[mentioned.id] ?: throw IgnoreException()
            val settings = MaimaiSettingsTable.settings(mentioned.id)
            UserQueryParams.QQ(qq, event, true, settings)
        }
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
    ): List<ChunithmAPI> {
        var backends = listOf(
            chunithm.backend("lxns"),
            chunithm.backend("diving-fish"),
        ).toMutableList()
        if (user.isSelf && !listAll)
            MaimaiSettingsTable[user.event.sender.id, "prober"] ?.let { prefer ->
                if (prefer.isBlank())
                    return@let
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
    ): Pair<RatingResponse, ChunithmAPI> {
        if (user.isMaxScore())
            return Pair(maxScoreRating(), listBackends(user).first())
        val result = queryBackends(user, listBackends(user)) { backend ->
            val response = backend.getPlayerRating(user) ?: return@queryBackends null
            if (response.oldRatingList.isEmpty() && response.newRatingList.isEmpty())
                throw NoDataException(api = backend)
            response
        }
        // TODO: 设置表中用中二单独一个前缀
//        result.first.settings = mergeSettings(result.first.settings, user.settings)
        return result
    }


    suspend fun records(
        user: UserQueryParams,
        musics: List<MusicInfo>
    ): Pair<RecordsResponse, ChunithmAPI> {
        val result = queryBackends(user, listBackends(user)) { backend ->
            backend.getPlayerRecords(user, musics)
        }
//        result.first.settings = mergeSettings(result.first.settings, user.settings)
        return result
    }

    suspend fun record(
        user: UserQueryParams,
        music: MusicInfo
    ): List<Record> {
        val result = queryBackends(user, listBackends(user)) { backend ->
            backend.getPlayerRecord(user, music)
        }
        return result.first
    }
    suspend fun recent(
        user: UserQueryParams,
    ): Pair<RecordsResponse, ChunithmAPI> {
        val backends = listBackends(user).filterIsInstance<LXNS>()
        if (backends.isEmpty())
            throw NotSupportedException("该功能仅支持落雪查分器")
        val result = queryBackends(user, backends) { backend ->
            backend.getPlayerRecent(user)
        }
        val response = result.first
        val backend = result.second
//        response.settings = mergeSettings(response.settings, user.settings)
        return Pair(response, backend)
    }

    private suspend fun <T: Any, A: ChunithmAPI> queryBackends(
        user: UserQueryParams,
        backends: List<A>,
        block: suspend (A) -> T?
    ): Pair<T, A> {
        val failures = mutableListOf<QueryFailure>()
        backends.forEach { backend ->
            runCatching {
                block(backend)
            }.onSuccess { result ->
                result ?.let {
                    return Pair(it, backend)
                }
            }.onFailure { e ->
                if (e is CancellationException)
                    throw e
                if (e is Exception)
                    failures.add(QueryFailure(backend, e.asQueryException(user)))
            }
        }
        throw failures.takeIf { it.isNotEmpty() } ?.selectException(user) ?: UnknownException()
    }

    private data class QueryFailure(
        val backend: ChunithmAPI,
        val exception: Exception
    )

    private fun Exception.asQueryException(user: UserQueryParams) = when {
        user is UserQueryParams.QQ && this is UserNotFoundException -> UserBindRequiredException(message)
        else -> this
    }

    private fun List<QueryFailure>.selectException(user: UserQueryParams): Exception {
        val failures = filter { failure ->
            failure.exception !is UserOARequiredException || shouldHandleUserOARequired(user)
        }.ifEmpty { this }
        return queryExceptionOrder.firstNotNullOfOrNull { type ->
            failures.firstOrNull { type.isInstance(it.exception) } ?.exception
        } ?: failures.first().exception
    }

    private fun List<QueryFailure>.shouldHandleUserOARequired(user: UserQueryParams): Boolean {
        if (user.isSelf && firstOrNull() ?.backend is LXNS)
            return true
        return any { failure ->
            failure.backend.id == "diving-fish" && when (failure.exception) {
                is NoDataException -> true
                is UserBindRequiredException -> true
                else -> false
            }
        }
    }

    private fun UserQueryParams.isMaxScore(): Boolean {
        if (this !is UserQueryParams.Username)
            return false
        return username.lowercase() in listOf("maxscore", "理论", "理论值")
    }
    private fun maxScoreRecords(): List<Record> = chunithm.musics().flatMap {
        it.charts
    }.map { chart ->
        Record(
            music = chart.music,
            chart = chart,
            achievement = 1010000,
            comboStatus = ComboStatus.AllJusticeCritical,
            chainStatus = ChainStatus.Platinum,
            clear = "catastrophy",
            rate = "sssp",
            rating = Rating.calc(chart, 1010000)
        )
    }.sortedByDescending { it.rating }
    private fun maxScoreRating(): RatingResponse {
        val scores = maxScoreRecords()
        val old = scores.filter { !it.music.isNew }.take(30)
        val new = scores.filter { it.music.isNew }.take(20)
        val rating = (old.sumOf { it.rating } + new.sumOf { it.rating } / 50).ratingFloor()
        return RatingResponse(
            player = PlayerInfo("理论值", rating, 23),
            oldRatingList = old,
            newRatingList = new,
        )
    }
}