package xyz.xszq.bot.feedback

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.xszq.bot.Member
import xyz.xszq.bot.Plugin
import xyz.xszq.bot.User
import xyz.xszq.bot.event.*
import xyz.xszq.bot.feedback.database.FeedbackDraftTable
import xyz.xszq.bot.feedback.database.FeedbackEmailTable
import xyz.xszq.bot.feedback.database.FeedbackTicketTable
import xyz.xszq.bot.feedback.service.FeedbackEmailer
import xyz.xszq.bot.message.Markdown
import xyz.xszq.bot.message.MessageChain
import xyz.xszq.bot.message.RemoteImage
import xyz.xszq.bot.payload.AdminCheckRequest
import xyz.xszq.bot.payload.markdown.Keyboard
import xyz.xszq.bot.payload.markdown.Keyboard.KeyboardRowBuilder
import xyz.xszq.bot.payload.markdown.RenderData
import xyz.xszq.bot.reply
import xyz.xszq.bot.subscribe.CommandNotMatchedException
import xyz.xszq.bot.subscribe.SubscribeBuilder
import xyz.xszq.bot.util.json
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

/**
 * 反馈插件
 */
@Suppress("unused")
class Feedback: Plugin() {

    internal val pendingBodies = ConcurrentHashMap<String, PendingBody>()
    internal val pendingEmails = ConcurrentHashMap<String, PendingEmail>()
    internal val pendingReplies = ConcurrentHashMap<String, PendingReply>()

    internal val pendingAliases = ConcurrentHashMap<String, PendingAlias>()

    lateinit var config: FeedbackConfig
    lateinit var emailer: FeedbackEmailer

    var pendingTtl: Long = 24 * 60 * 60_000L

    internal var onKeyboard: (Keyboard) -> Unit = {}

    @OptIn(ExperimentalHoplite::class)
    override suspend fun load() {
        config = ConfigLoaderBuilder.default()
            .addFileSource("./config/feedback.yml")
            .withExplicitSealedTypes()
            .build()
            .loadConfigOrThrow<FeedbackConfig>()
        emailer = FeedbackEmailer(config.email)

        transaction(database) {
            listOf(
                FeedbackTicketTable,
                FeedbackDraftTable,
                FeedbackEmailTable
            ).forEach { table ->
                if (!table.exists())
                    SchemaUtils.create(table)
            }
        }

        setRoute()
        restore()
        migrateEmails()
        logger.info { "[反馈] 插件加载完成。" }
    }

    override suspend fun unload() {
        close()
    }

    private val draftExpiry = ConcurrentHashMap<String, Job>()

    // 邮箱表建立前用户填在工单上的邮箱搬进来，免得升级后再被问一次
    private suspend fun migrateEmails() {
        FeedbackTicketTable.latestEmails(database).forEach { (userId, email) ->
            if (FeedbackEmailTable.load(database, userId) == null)
                FeedbackEmailTable.save(database, userId, email)
        }
    }


    suspend fun setRoute() = route {
        // 反馈面板
        buttons(PANEL_BUTTON, TYPE_BUTTON) {
            val session = sessionOf(this)
            when (button) {
                TYPE_BUTTON -> chooseType(session, data)
                else -> showProgress(session)
            }
        }
        // 列表筛选与翻页
        buttons(LIST_BUTTON) {
            val session = sessionOf(this)
            val args = data.split("\n")
            if (args.size >= 2)
                showList(session, args[0], args[1].toIntOrNull() ?: 1)
        }
        // 管理员回复工单
        buttons(REPLY_BUTTON) {
            val session = sessionOf(this)
            val args = data.split("\n")
            if (args.size >= 2) {
                val status = FeedbackStatus.entries
                    .firstOrNull { it.value == args[0] }
                if (status != null)
                    promptReply(session, args[1], status)
            }
        }
        domain(
            name = FEEDBACK_DOMAIN,
            value = FEEDBACK_DOMAIN
        ) {
            // 用户提交反馈
            startsWith("反馈", matchWord = true) { _ ->
                val session = sessionOf(this)
                clearPending(session.userId)
                showPanel(session)
            }
            // 用户查看自己的反馈
            startsWith("反馈进度") {
                val session = sessionOf(this)
                clearPending(session.userId)
                showProgress(session)
            }
            // 管理员查看全部反馈
            startsWith("反馈列表") { raw ->
                val session = sessionOf(this)
                if (!isAdmin(session.userId))
                    return@startsWith
                val arg = raw.trim()
                val ticket = FeedbackTicketTable.load(database, arg)
                if (ticket != null)
                    showDetail(session, ticket, 1)
                else
                    showList(session, arg.ifBlank { ALL }, 1)
            }
            // 管理员回复工单
            startsWith("反馈回复") { raw ->
                val session = sessionOf(this)
                if (!isAdmin(session.userId))
                    return@startsWith
                clearPending(session.userId)
                val args = raw.trim().split(" ", limit = 2)
                if (args.size < 2 || args[0].isBlank() || args[1].isBlank()) {
                    reply("使用方法：反馈回复 反馈编号 回复内容")
                    return@startsWith
                }
                handleReply(session, args[0], args[1], FeedbackStatus.Open)
            }
            // 用户填写接收反馈的邮箱
            startsWith("设置邮箱") { raw ->
                val session = sessionOf(this)
                setEmail(session, raw)
            }
            // 草稿输入兜底
            always(FEEDBACK_INPUT) { onPendingInput(sessionOf(this)) }
        }
        // 别名举报
        channel<Pair<ReplyAble, String>>(REPORT_CHANNEL) { (source, payload) ->
            val report = runCatching {
                json.decodeFromString<ReportedAlias>(payload)
            }.getOrNull() ?: return@channel
            val session = sessionOf(source)
            clearPending(session.userId)
            pendingAliases[session.userId] = PendingAlias(
                session = session,
                game = report.game,
                songId = report.songId,
                alias = report.alias
            )
            saveDraft(session, FeedbackType.Alias.value)
            listenAlias(session)
            source.reply(Markdown.create {
                brief("举报别名", FeedbackType.Alias.hint)
                line()
                line("游戏：${report.game}")
                line("歌曲：${report.songId}.${report.songName}")
                line("别名：${report.alias}")
                line()
                line("请直接发送文字说明理由。")
                keyboard {
                    row {
                        at("⬇填写理由", " ", style = RenderData.FILLED_BLUE)
                    }
                }
            })
        }
    }


    fun close() {
        draftExpiry.values.forEach { it.cancel() }
        draftExpiry.clear()
        pendingBodies.clear()
        pendingEmails.clear()
        pendingReplies.clear()
        pendingAliases.clear()
    }


    private fun SubscribeBuilder.buttons(
        vararg ids: String,
        block: suspend InteractionEvent.() -> Unit
    ) = ids.forEach { id -> button(id, block) }

    private suspend fun showPanel(session: FeedbackSession) {
        val message = Markdown.create {
            brief("反馈", "请选择反馈类型：")
            keyboard {
                row {
                    typeButton(FeedbackType.Bug)
                    typeButton(FeedbackType.Suggestion)
                }
                row {
                    typeButton(FeedbackType.Abuse)
                    callback(
                        label = "📋处理进度",
                        data = PROGRESS_DATA,
                        style = RenderData.GRAY,
                        id = PANEL_BUTTON
                    )
                }
            }
        }
        message.keyboard?.let(onKeyboard)
        session.message.reply(message)
    }

    private fun KeyboardRowBuilder.typeButton(type: FeedbackType) = callback(
        label = type.label,
        data = type.value,
        style = RenderData.BLUE,
        id = TYPE_BUTTON
    )

    private fun listData(filter: String, page: Int) = "$filter\n$page"

    private suspend fun chooseType(session: FeedbackSession, value: String) {
        val type = FeedbackType.of(value) ?: return
        promptBody(session, type)
    }

    private suspend fun promptBody(
        session: FeedbackSession,
        type: FeedbackType
    ) {
        pendingBody(session, type)
        session.message.reply(Markdown.create {
            brief(type.label, type.hint)
            line()
            line("请直接发送要反馈的内容：")
            keyboard {
                row {
                    at("⬇填写正文", " ", style = RenderData.FILLED_BLUE)
                }
                row {
                    callback(
                        label = "📋处理进度",
                        data = PROGRESS_DATA,
                        style = RenderData.GRAY,
                        id = PANEL_BUTTON
                    )
                }
            }
        })
    }

    private suspend fun submit(
        session: FeedbackSession,
        type: String,
        body: String,
        game: String?,
        songId: Int?,
        alias: String?
    ): Boolean {
        val text = body.trim()
        if (text.isBlank()) {
            session.message.reply(Markdown.create {
                brief("反馈", "反馈正文不能为空，请发送文字或图片。")
                keyboard {
                    row {
                        at("⬇重新填写", " ", style = RenderData.FILLED_BLUE)
                    }
                }
            })
            return false
        }
        val email = FeedbackEmailTable.load(database, session.userId).orEmpty()
        val id = FeedbackTicketTable.insert(
            db = database,
            type = type,
            body = text,
            userId = session.userId,
            userName = session.userName,
            game = game.orEmpty(),
            songId = songId ?: 0,
            alias = alias.orEmpty(),
            groupId = session.groupId.orEmpty(),
            email = email
        )
        logger.info {
            "[反馈] ${session.userName}(${session.userId}) 提交了 " +
                "${FeedbackType.nameOf(type)} ${id.take(8)}"
        }
        session.message.reply(Markdown.create {
            brief("反馈", "反馈 #${id.take(8)} 提交成功，请等待管理员处理。")
            line()
            if (email.isBlank())
                line("若您想通过邮箱接收反馈，请点击下方填写邮箱")
            else
                line("处理结果会发送到 $email")
            keyboard {
                if (email.isBlank())
                    row {
                        at(
                            "📧填写邮箱",
                            "/设置邮箱 ",
                            style = RenderData.FILLED_BLUE
                        )
                    }
                row {
                    callback(
                        label = "📋处理进度",
                        data = PROGRESS_DATA,
                        style = RenderData.GRAY,
                        id = PANEL_BUTTON
                    )
                }
            }
        })
        return true
    }

    private suspend fun setEmail(session: FeedbackSession, raw: String) {
        val email = raw.trim()
        if (email.isNotBlank()) {
            saveEmail(session, email)
            return
        }
        // 切到邮箱输入，放弃其他草稿
        if (!pendingEmails.containsKey(session.userId)) {
            clearPending(session.userId)
            pendingEmail(session)
        }
        session.message.reply(Markdown.create {
            brief("填写邮箱", "请直接发送你的邮箱地址。")
        })
    }

    private suspend fun saveEmail(
        session: FeedbackSession,
        email: String
    ): Boolean {
        if (!validEmail(email)) {
            if (!pendingEmails.containsKey(session.userId))
                pendingEmail(session)
            session.message.reply(Markdown.create {
                brief("填写邮箱", "邮箱格式不正确，请重新填写。")
                keyboard {
                    row {
                        at("📧重新填写", "/设置邮箱 ", style = RenderData.FILLED_BLUE)
                    }
                }
            })
            return false
        }
        FeedbackEmailTable.save(database, session.userId, email)
        // 已经提交过的工单也一并补上，之前的反馈同样能收到回复
        FeedbackTicketTable.updateEmail(database, session.userId, email)
        session.message.reply("邮箱已记录，后续反馈都会发送到该邮箱。")
        return true
    }

    private suspend fun showProgress(session: FeedbackSession) {
        val tickets = FeedbackTicketTable.ofUser(database, session.userId, 10)
        if (tickets.isEmpty()) {
            session.message.reply(
                "你还没有提交过反馈，发送“反馈”即可提交。"
            )
            return
        }
        session.message.reply(Markdown.create {
            brief("反馈进度", "你最近提交的反馈：")
            line()
            tickets.forEach { ticket ->
                line(ticketLine(ticket))
                if (ticket.reply.isNotBlank()) {
                    line("> 回复：" + ticket.reply.replace("\n", "\n> "))
                    line()
                }
            }
        })
    }

    private suspend fun showList(
        session: FeedbackSession,
        filter: String,
        page: Int
    ) {
        val known = FeedbackStatus.entries.any { it.value == filter }
        if (filter != ALL && !known)
            return
        val status = if (filter == ALL) "" else filter
        val total = FeedbackTicketTable.count(database, status)
        if (total == 0L) {
            session.message.reply("没有符合条件的反馈。")
            return
        }
        val totalPages = ((total + PAGE_SIZE - 1) / PAGE_SIZE).toInt()
        val nowPage = page.coerceIn(1, totalPages)
        val tickets = FeedbackTicketTable.page(
            db = database,
            status = status,
            offset = (nowPage - 1) * PAGE_SIZE,
            limit = PAGE_SIZE
        )
        val openCount = FeedbackTicketTable.openCount(database)
        session.message.reply(Markdown.create {
            val title = "共 $total 条，待处理 $openCount 条，第 $nowPage/$totalPages 页"
            brief("反馈列表", title)
            line()
            tickets.forEach { ticket ->
                line(ticketLine(ticket) + " " + detailLink(ticket))
            }
            keyboard {
                row {
                    listOf(
                        ALL to "全部",
                        FeedbackStatus.Open.value to "待处理",
                        FeedbackStatus.Solved.value to "已解决",
                        FeedbackStatus.Closed.value to "已关闭"
                    ).forEach { (value, label) ->
                        val style = if (value == filter)
                            RenderData.BLUE
                        else
                            RenderData.GRAY
                        callback(
                            label = label,
                            data = listData(value, nowPage),
                            style = style,
                            id = LIST_BUTTON
                        )
                    }
                }
                if (totalPages > 1) {
                    row {
                        if (nowPage > 1)
                            callback(
                                label = "⬅️上一页",
                                data = listData(filter, nowPage - 1),
                                style = RenderData.GRAY,
                                id = LIST_BUTTON
                            )
                        if (nowPage < totalPages)
                            callback(
                                label = "➡️下一页",
                                data = listData(filter, nowPage + 1),
                                style = RenderData.GRAY,
                                id = LIST_BUTTON
                            )
                    }
                }
            }
        })
    }

    private suspend fun showDetail(
        session: FeedbackSession,
        ticket: FeedbackTicket,
        page: Int
    ) {
        session.message.reply(detailMarkdown(ticket, page))
    }

    private suspend fun handleReply(
        session: FeedbackSession,
        ticketId: String,
        reply: String,
        status: FeedbackStatus
    ): Boolean {
        val ticket = FeedbackTicketTable.load(database, ticketId)
        if (ticket == null) {
            session.message.reply("未找到该反馈，可能已被删除。")
            return false
        }
        val text = reply.trim()
        if (text.isBlank()) {
            session.message.reply("回复内容不能为空。")
            return false
        }
        FeedbackTicketTable.updateReply(
            db = database,
            id = ticketId,
            reply = text,
            status = status.value
        )
        logger.info { "[反馈] 已回复 ${ticketId.take(8)} ${status.value}" }
        session.message.reply(Markdown.create {
            brief("反馈", "回复成功，状态已置为${status.label}。")
            line()
            line("用户可在“反馈进度”中查看你的回复。")
            if (ticket.email.isNotBlank())
                line("同时会向 ${ticket.email} 发送邮件。")
            else
                line("用户未填写邮箱，不会发送邮件。")
        })
        if (ticket.email.isNotBlank()) {
            val replied = ticket.copy(reply = text, status = status.value)
            launchEmail(replied, ticketId)
        }
        return true
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun launchEmail(ticket: FeedbackTicket, ticketId: String) {
        GlobalScope.launch {
            emailer.reply(ticket) ?.let { messageId ->
                FeedbackTicketTable.updateEmailMessageId(
                    db = database,
                    id = ticketId,
                    messageId = messageId
                )
            }
        }
    }

    private suspend fun promptReply(
        session: FeedbackSession,
        ticketId: String,
        status: FeedbackStatus
    ) {
        val ticket = FeedbackTicketTable.load(database, ticketId)
        if (ticket == null) {
            session.message.reply("未找到该反馈，可能已被删除。")
            return
        }
        pendingReply(session, ticketId, status)
        session.message.reply(Markdown.create {
            val hint = "请直接发送回复内容，状态将置为${status.label}"
            brief("回复反馈", hint)
            line()
            if (ticket.email.isBlank())
                line("用户未填写邮箱，不会发送邮件通知。")
            else
                line("回复后会同时发送邮件到 ${ticket.email}。")
            line()
            line("被回复的内容：")
            line("> " + ticket.body.replace("\n", "\n> "))
            keyboard {
                row {
                    at("✍填写回复", " ", style = RenderData.FILLED_BLUE)
                }
                row {
                    callback(
                        label = "⬅️返回列表",
                        data = listData(ALL, 1),
                        style = RenderData.GRAY,
                        id = LIST_BUTTON
                    )
                }
            }
        })
    }

    private fun detailMarkdown(
        ticket: FeedbackTicket,
        page: Int
    ) = Markdown.create {
        val title = "${FeedbackType.nameOf(ticket.type)} · ${timeOf(ticket)}"
        brief("反馈 ${ticket.id.take(8)}", title)
        line()
        line("状态：${FeedbackStatus.nameOf(ticket.status)}")
        line("提交者：${ticket.userName}（${ticket.userId}）")
        if (ticket.email.isNotBlank())
            line("邮箱：${ticket.email}")
        if (ticket.alias.isNotBlank()) {
            line("被举报歌曲：${ticket.songId}.${ticket.game}")
            line("被举报别名：${ticket.alias}")
        }
        if (ticket.groupId.isNotBlank())
            line("来源：群 ${ticket.groupId}")
        line()
        line("反馈内容：")
        line("> " + ticket.body.replace("\n", "\n> "))
        if (ticket.reply.isNotBlank()) {
            line()
            line("最近回复：")
            line("> " + ticket.reply.replace("\n", "\n> "))
        }
        keyboard {
            row {
                callback(
                    label = "✍回复用户",
                    data = replyData(ticket.id, FeedbackStatus.Open),
                    style = RenderData.BLUE,
                    id = REPLY_BUTTON
                )
            }
            row {
                callback(
                    label = "✅回复并解决",
                    data = replyData(ticket.id, FeedbackStatus.Solved),
                    style = RenderData.BLUE,
                    id = REPLY_BUTTON
                )
                callback(
                    label = "🔒回复关闭",
                    data = replyData(ticket.id, FeedbackStatus.Closed),
                    style = RenderData.GRAY,
                    id = REPLY_BUTTON
                )
            }
            row {
                callback(
                    label = "⬅️返回列表",
                    data = listData(ALL, page),
                    style = RenderData.GRAY,
                    id = LIST_BUTTON
                )
            }
        }
    }

    private fun replyData(ticketId: String, status: FeedbackStatus) =
        "${status.value}\n$ticketId"

    private fun detailLink(ticket: FeedbackTicket) = "[查看详情](" +
        "mqqapi://aio/inlinecmd?command=" +
        URLEncoder.encode("/反馈列表 ${ticket.id}", Charsets.UTF_8) +
        "&enter=true&reply=false)"

    private fun ticketLine(ticket: FeedbackTicket) = buildString {
        append(FeedbackType.iconOf(ticket.type))
        append(" ")
        append(timeOf(ticket))
        append(" ")
        append(FeedbackStatus.iconOf(ticket.status))
        append(" ")
        append(truncate(ticket.body))
    }

    private fun timeOf(ticket: FeedbackTicket) = buildString {
        append(ticket.createdAt.monthNumber.toString().padStart(2, '0'))
        append("-")
        append(ticket.createdAt.dayOfMonth.toString().padStart(2, '0'))
        append(" ")
        append(ticket.createdAt.hour.toString().padStart(2, '0'))
        append(":")
        append(ticket.createdAt.minute.toString().padStart(2, '0'))
    }

    private fun truncate(text: String, length: Int = SUMMARY_LENGTH) = text
        .replace("\n", " ")
        .let { if (it.length > length) it.take(length) + "..." else it }

    private suspend fun isAdmin(userId: String): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        pluginLoader.subscribes.handle(ChannelEvent(
            bot = pluginLoader.bot,
            channelName = "admin-check",
            data = AdminCheckRequest(userId, deferred)
        ))
        return runCatching { withTimeout(5000L) { deferred.await() } }
            .getOrDefault(false)
    }

    private fun isPending(userId: String) =
        pendingBodies.containsKey(userId) ||
        pendingEmails.containsKey(userId) ||
        pendingReplies.containsKey(userId) ||
        pendingAliases.containsKey(userId)

    private fun MessageEvent.draftInput() = buildString {
        append(text.trim())
        message.filterIsInstance<RemoteImage>().forEach { image ->
            appendLine()
            append(image.url)
        }
    }.trim()

    private fun sessionOf(source: ReplyAble) = when (source) {
        is MessageEvent -> FeedbackSession(
            message = source,
            userId = source.sender.id,
            userName = source.sender.username,
            groupId = (source as? GroupEvent) ?.group ?.id
        )
        else -> error("不支持的事件类型：${source::class.simpleName}")
    }

    private fun sessionOf(event: InteractionEvent) = when (event) {
        is GroupInteractionEvent -> FeedbackSession(
            message = GroupMessageEvent(
                bot = event.bot,
                eventId = event.eventId,
                id = event.id,
                message = MessageChain(),
                sender = Member(
                    bot = event.bot,
                    id = event.sender.id,
                    username = event.sender.username,
                    isBot = event.sender.isBot,
                    isSelf = event.sender.isSelf
                ),
                group = event.group,
                seq = event.seq
            ),
            userId = event.sender.id,
            userName = event.sender.username,
            groupId = event.group.id
        )
        else -> FeedbackSession(
            message = MessageEvent(
                bot = event.bot,
                eventId = event.eventId,
                id = event.id,
                message = MessageChain(),
                sender = event.sender,
                seq = event.seq
            ),
            userId = event.sender.id,
            userName = event.sender.username,
            groupId = null
        )
    }

    private suspend fun pendingBody(session: FeedbackSession, type: FeedbackType) {
        val userId = session.userId
        pendingBodies[userId] = PendingBody(session, type)
        saveDraft(session, type.value)
        expire(userId)
    }

    private suspend fun pendingEmail(session: FeedbackSession) {
        val userId = session.userId
        pendingEmails[userId] = PendingEmail(session)
        saveDraft(session, STATUS_EMAIL)
        expire(userId)
    }

    private suspend fun pendingReply(
        session: FeedbackSession,
        ticketId: String,
        status: FeedbackStatus
    ) {
        val userId = session.userId
        pendingReplies[userId] = PendingReply(session, ticketId, status)
        saveDraft(session, "$STATUS_REPLY${status.value}", ticketId)
        expire(userId)
    }

    // 草稿到期后静默回收，不打扰用户
    @OptIn(DelicateCoroutinesApi::class)
    private fun expire(userId: String) {
        draftExpiry.remove(userId) ?.cancel()
        draftExpiry[userId] = GlobalScope.launch {
            delay(pendingTtl)
            clearPending(userId)
        }
    }

    private fun listenAlias(session: FeedbackSession) = expire(session.userId)

    private suspend fun onPendingInput(session: FeedbackSession) {
        if (!isPending(session.userId))
            throw CommandNotMatchedException()
        val message = session.message
        val done = when {
            pendingBodies.containsKey(session.userId) -> {
                val pending = pendingBodies[session.userId] ?: return
                submit(
                    session = session,
                    type = pending.type.value,
                    body = message.draftInput(),
                    game = null,
                    songId = null,
                    alias = null
                )
            }
            pendingEmails.containsKey(session.userId) ->
                saveEmail(session, message.draftInput())
            pendingReplies.containsKey(session.userId) -> {
                val pending = pendingReplies[session.userId] ?: return
                handleReply(
                    session,
                    pending.ticketId,
                    message.draftInput(),
                    pending.status
                )
            }
            pendingAliases.containsKey(session.userId) -> {
                val pending = pendingAliases[session.userId] ?: return
                submitAlias(session, pending, message.draftInput())
            }
            else -> true
        }
        if (done)
            clearPending(session.userId)
    }

    private suspend fun submitAlias(
        session: FeedbackSession,
        pending: PendingAlias,
        reason: String
    ) = submit(
        session = session,
        type = FeedbackType.Alias.value,
        body = reason,
        game = pending.game,
        songId = pending.songId,
        alias = pending.alias
    )

    /**
     * 恢复草稿
     */
    suspend fun restore() {
        val drafts = FeedbackDraftTable.active(database, expireBefore())
        FeedbackDraftTable.purgeExpired(database, expireBefore())
        drafts.forEach { draft ->
            val session = sessionOf(draft)
            val type = FeedbackType.of(draft.status)
            when {
                type != null -> pendingBody(session, type)
                draft.status == STATUS_EMAIL -> pendingEmail(session)
                draft.status.startsWith(STATUS_REPLY) -> {
                    val raw = draft.status.removePrefix(STATUS_REPLY)
                    val status = FeedbackStatus.entries
                        .firstOrNull { it.value == raw }
                        ?: return@forEach
                    pendingReply(session, draft.feedbackId, status)
                }
                else -> return@forEach
            }
            logger.info { "[反馈] 已恢复 ${draft.userId} 的草稿 ${draft.status}" }
        }
    }

    private fun FeedbackSession.withMessage(message: MessageEvent) =
        copy(message = message)

    private fun sessionOf(draft: FeedbackDraft): FeedbackSession {
        val bot = pluginLoader.bot
        val message = if (draft.groupId.isNotBlank())
            GroupMessageEvent(
                bot = bot,
                eventId = "",
                id = "",
                message = MessageChain(),
                sender = Member(
                    bot = bot,
                    id = draft.userId,
                    username = draft.userName
                ),
                group = bot.group(draft.groupId)
            )
        else
            MessageEvent(
                bot = bot,
                eventId = "",
                id = "",
                message = MessageChain(),
                sender = User(bot, draft.userId, draft.userName)
            )
        return FeedbackSession(
            message = message,
            userId = draft.userId,
            userName = draft.userName,
            groupId = draft.groupId.ifBlank { null }
        )
    }

    // 写完再回复用户，避免重启时草稿还没落库
    private suspend fun saveDraft(
        session: FeedbackSession,
        status: String,
        feedbackId: String = ""
    ) = FeedbackDraftTable.save(
        db = database,
        userId = session.userId,
        userName = session.userName,
        status = status,
        feedbackId = feedbackId,
        groupId = session.groupId.orEmpty()
    )

    private suspend fun clearPending(userId: String) {
        draftExpiry.remove(userId) ?.cancel()
        pendingBodies.remove(userId)
        pendingEmails.remove(userId)
        pendingReplies.remove(userId)
        pendingAliases.remove(userId)
        FeedbackDraftTable.remove(database, userId)
    }

    private fun expireBefore() = (Clock.System.now() - pendingTtl.milliseconds)
        .toLocalDateTime(TimeZone.currentSystemDefault())

    private fun validEmail(email: String) = email.length <= 254 &&
        EMAIL_REGEX.matches(email)

    internal data class FeedbackSession(
        val message: MessageEvent,
        val userId: String,
        val userName: String,
        val groupId: String?
    )

    internal data class PendingBody(
        val session: FeedbackSession,
        val type: FeedbackType
    )

    internal data class PendingEmail(
        val session: FeedbackSession
    )

    internal data class PendingAlias(
        val session: FeedbackSession,
        val game: String,
        val songId: Int,
        val alias: String
    )

    internal data class PendingReply(
        val session: FeedbackSession,
        val ticketId: String,
        val status: FeedbackStatus
    )

    private companion object {
        const val ALL = "all"
        const val FEEDBACK_DOMAIN = "feedback"
        const val FEEDBACK_INPUT = "feedback-input"
        const val PANEL_BUTTON = "feedback-panel"
        const val TYPE_BUTTON = "feedback-type"
        const val LIST_BUTTON = "feedback-list"
        const val REPLY_BUTTON = "feedback-reply"
        const val PROGRESS_DATA = "progress"
        const val PAGE_SIZE = 10
        const val SUMMARY_LENGTH = 24
        const val STATUS_EMAIL = "email"
        const val STATUS_REPLY = "reply:"
        const val REPORT_CHANNEL = "feedback-report"

        val EMAIL_REGEX =
            Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    }
}