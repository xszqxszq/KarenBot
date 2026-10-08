package xyz.xszq.bot.feedback

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.xszq.bot.BotSandbox
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.message.Markdown
import xyz.xszq.bot.database.newSuspendedTransaction
import xyz.xszq.bot.event.ChannelEvent
import xyz.xszq.bot.util.json
import xyz.xszq.bot.feedback.database.FeedbackDraftTable
import xyz.xszq.bot.feedback.database.FeedbackTicketTable
import xyz.xszq.bot.payload.markdown.Action
import xyz.xszq.bot.payload.markdown.Keyboard
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FeedbackTest {
    private val database: Database
        get() = connectIfNeeded()

    private fun freshDatabase(): Database {
        val name = "feedback${counter.incrementAndGet()}"
        val db = Database.connect(
            url = "jdbc:h2:mem:$name;MODE=MySQL;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
        )
        transaction(db) {
            SchemaUtils.create(FeedbackTicketTable, FeedbackDraftTable)
        }
        return db
    }

    @BeforeTest
    fun resetDatabase() {
        connectIfNeeded()
    }

    @Test
    fun testSubmitByPanel() = runTest {
        val sandbox = setFeedback(this, freshDatabase())
        try {
            // 打开面板
            val panel = sandbox.user("user-1") says "/反馈"
            val panelReply = sandbox.awaitReplyContaining(
                panel, "请选择反馈类型"
            )
            assertNotNull(panelReply)

            // 选择类型后进入正文填写
            val choose = sandbox.tapButton(
                button = typeButton(),
                data = "bug",
                id = "user-1"
            )
            val prompt = sandbox.awaitReplyContaining(choose, "请填写你遇到的 Bug")
            assertNotNull(prompt)

            // 正文落库并回复编号，回复挂在用户那条消息上
            val submit = sandbox.user("user-1") says "查询成绩时崩溃了"
            assertNotNull(sandbox.awaitReplyContaining(submit, "提交成功"))

            val ticket = transaction(sandbox.pluginLoader.database) {
                FeedbackTicketTable.selectAll().firstOrNull()
            }
            assertNotNull(ticket, "工单应已写入数据库")
        } finally {
            sandbox.cleanup()
        }
    }

    @Test
    fun testAbuseTypeAndProgress() = runTest {
        val sandbox = setFeedback(this, freshDatabase())
        try {
            val choose = sandbox.tapButton(
                button = typeButton(),
                data = "abuse",
                id = "user-2"
            )
            val prompt = sandbox.awaitReplyContaining(
                choose, "请填写要举报的滥用行为"
            )
            assertNotNull(prompt)

            val submit = sandbox.user("user-2") says "有人刷屏"
            assertNotNull(sandbox.awaitReplyContaining(submit, "提交成功"))

            val progress = sandbox.user("user-2") says "/反馈进度"
            assertNotNull(sandbox.awaitReplyContaining(progress, "有人刷屏"))
        } finally {
            sandbox.cleanup()
        }
    }

    @Test
    fun testEmailFormatRejected() = runTest {
        val sandbox = setFeedback(this, freshDatabase())
        try {
            val choose = sandbox.tapButton(
                button = typeButton(),
                data = "bug",
                id = "user-3"
            )
            sandbox.awaitReplyContaining(choose, "请填写你遇到的 Bug")
            val submit = sandbox.user("user-3") says "邮箱测试"
            val card = sandbox.awaitReplyContaining(submit, "提交成功")
            assertNotNull(card)

            // 邮箱入口只做输入框预填，用户直接补上地址发送
            val opened = sandbox.user("user-3") says "/设置邮箱"
            assertNotNull(
                sandbox.awaitReplyContaining(opened, "请直接发送你的邮箱地址")
            )

            val invalid = sandbox.user("user-3") says "不是邮箱"
            assertNotNull(
                sandbox.awaitReplyContaining(invalid, "邮箱格式不正确")
            )

            val valid = sandbox.user("user-3") says "me@example.com"
            assertNotNull(sandbox.awaitReplyContaining(valid, "邮箱已记录"))
        } finally {
            sandbox.cleanup()
        }
    }

    @Test
    fun testPendingExpiresAndUserCanRetry() = runTest {
        // 用很短的 TTL 模拟放弃填写
        val sandbox = setFeedback(this, freshDatabase(), pendingTtl = 200L)
        try {
            val choose = sandbox.tapButton(
                button = typeButton(),
                data = "bug",
                id = "user-4"
            )
            val prompt = sandbox.awaitReplyContaining(choose, "请填写你遇到的 Bug")
            assertNotNull(prompt)
            // 用户不填，等过期
            Thread.sleep(600L)

            // 过期后草稿必须从库中清掉，否则重启会被复活
            val left = transaction(sandbox.pluginLoader.database) {
                FeedbackDraftTable.selectAll().count()
            }
            assertEquals(0L, left, "过期草稿应已清除")

            // 也必须能重新发起，而不是被请先填写永久卡住
            val again = sandbox.tapButton(
                button = typeButton(),
                data = "bug",
                id = "user-4"
            )
            val againPrompt = sandbox.awaitReplyContaining(
                again, "请填写你遇到的 Bug"
            )
            assertNotNull(againPrompt)
        } finally {
            sandbox.cleanup()
        }
    }

    @Test
    fun testDraftIsPersisted() = runTest {
        val sandbox = setFeedback(this, freshDatabase())
        try {
            val choose = sandbox.tapButton(
                button = typeButton(),
                data = "bug",
                id = "user-5"
            )
            val prompt = sandbox.awaitReplyContaining(choose, "请填写你遇到的 Bug")
            assertNotNull(prompt)

            // 草稿应在点选类型时落库
            val draft = transaction(sandbox.pluginLoader.database) {
                FeedbackDraftTable.selectAll().firstOrNull()
            }
            assertNotNull(draft, "草稿应写入数据库")
            assertEquals("bug", draft.get(FeedbackDraftTable.status))

            val submit = sandbox.user("user-5") says "内容"
            assertNotNull(sandbox.awaitReplyContaining(submit, "提交成功"))

            // 提交成功后草稿应被清除
            val deadline = System.nanoTime() + 5_000_000_000L
            var left = 1L
            while (left != 0L && System.nanoTime() < deadline) {
                left = transaction(sandbox.pluginLoader.database) {
                    FeedbackDraftTable.selectAll().count()
                }
                if (left != 0L) Thread.sleep(50)
            }
            assertEquals(0L, left, "提交完成后草稿应被清除")
        } finally {
            sandbox.cleanup()
        }
    }

    @Test
    fun testDraftSurvivesRestart() = runTest {
        // 模拟重启前留下的草稿
        val db = freshDatabase()
        newSuspendedTransaction(db = db) {
            FeedbackDraftTable.save(db, "user-6", "测试用户", "bug", groupId = "")
        }
        val sandbox = setFeedback(this, db)
        try {
            // 不打扰用户，直接补发正文即可完成提交
            val submit = sandbox.user("user-6") says "重启前没填完的 Bug"
            assertNotNull(sandbox.awaitReplyContaining(submit, "提交成功"))
        } finally {
            sandbox.cleanup()
        }
    }

    @Test
    fun testCommandAbandonsDraft() = runTest {
        val sandbox = setFeedback(this, freshDatabase())
        try {
            val choose = sandbox.tapButton(
                button = typeButton(),
                data = "bug",
                id = "user-10"
            )
            sandbox.awaitReplyContaining(choose, "请填写你遇到的 Bug")

            // 改发别的命令即放弃草稿，不能被拦下
            val progress = sandbox.user("user-10") says "反馈进度"
            assertNotNull(
                sandbox.awaitReplyContaining(progress, "你还没有提交过反馈")
            )

            // 草稿已丢弃，之后随便发的内容不会再被当成正文提交
            val after = sandbox.user("user-10") says "随便说说"
            assertNull(
                sandbox.awaitReplyContaining(after, "提交成功", 2000L)
            )
        } finally {
            sandbox.cleanup()
        }
    }

    @Test
    fun testProgressShowsReply() = runTest {
        val sandbox = setFeedback(this, freshDatabase())
        try {
            // 先提交一条，再由管理员回复
            val choose = sandbox.tapButton(
                button = typeButton(),
                data = "bug",
                id = "user-7"
            )
            val prompt = sandbox.awaitReplyContaining(choose, "请填写你遇到的 Bug")
            assertNotNull(prompt)
            val submit = sandbox.user("user-7") says "查询结果不对"
            assertNotNull(sandbox.awaitReplyContaining(submit, "提交成功"))

            val id = transaction(sandbox.pluginLoader.database) {
                FeedbackTicketTable.selectAll().firstOrNull()
                    ?.get(FeedbackTicketTable.id)
            }
            assertNotNull(id)
            FeedbackTicketTable.updateReply(
                db = sandbox.pluginLoader.database,
                id = id,
                reply = "已修复，请更新后再试",
                status = "solved"
            )

            // 用户通过进度面板自己查看回复，BOT 不主动投递
            val progress = sandbox.user("user-7") says "/反馈进度"
            assertNotNull(sandbox.awaitReplyContaining(progress, "已修复，请更新后再试"))
        } finally {
            sandbox.cleanup()
        }
    }

    @Test
    fun testSubmitInGroup() = runTest {
        val sandbox = setFeedback(this, freshDatabase())
        try {
            // 群聊里同样可以走完整流程
            val panel = sandbox.group() says "/反馈"
            val panelReply = sandbox.awaitReplyContaining(
                panel, "请选择反馈类型"
            )
            assertNotNull(panelReply)

            // 群消息的发送者由沙箱固定为 test-user
            val choose = sandbox.tapButton(
                button = typeButton(),
                data = "suggestion",
                id = "test-user",
                group = "test-group"
            )
            assertNotNull(sandbox.awaitReplyContaining(choose, "请填写你想要的新功能"))

            val submit = sandbox.group() says "希望支持导出成绩"
            assertNotNull(sandbox.awaitReplyContaining(submit, "提交成功"))

            // 提交时的会话被记录下来，供管理员回复时回到该群
            val groupId = transaction(sandbox.pluginLoader.database) {
                FeedbackTicketTable.selectAll().firstOrNull()
                    ?.get(FeedbackTicketTable.groupId)
            }
            assertEquals("test-group", groupId)
        } finally {
            sandbox.cleanup()
        }
    }

    @Test
    fun testBlankBodyRejected() = runTest {
        val sandbox = setFeedback(this, freshDatabase())
        try {
            val choose = sandbox.tapButton(
                button = typeButton(),
                data = "bug",
                id = "user-9"
            )
            assertNotNull(
                sandbox.awaitReplyContaining(choose, "请填写你遇到的 Bug")
            )

            // 空正文不算提交，草稿要留着让用户接着填
            val blank = sandbox.user("user-9") says "   "
            assertNotNull(
                sandbox.awaitReplyContaining(blank, "反馈正文不能为空")
            )
            assertEquals(0L, transaction(sandbox.pluginLoader.database) {
                FeedbackTicketTable.selectAll().count()
            })

            assertNotNull(
                sandbox.awaitReplyContaining(
                    sandbox.user("user-9") says "补上的正文", "提交成功"
                )
            )
        } finally {
            sandbox.cleanup()
        }
    }

    @Test
    fun testPanelButtonsCarryData() = runTest {
        // QQ 把空的 formid 回传成 null，核心会当未知互动直接丢掉
        // 所以面板上每个回调按钮都必须带上非空数据
        var panel: Keyboard? = null
        val sandbox = setFeedback(this, freshDatabase()) { panel = it }
        try {
            assertNotNull(
                sandbox.awaitReplyContaining(
                    sandbox.user("user-8") says "/反馈", "请选择反馈类型"
                )
            )
            val buttons = assertNotNull(panel)
                .content.rows.flatMap { row -> row.buttons }
            assertEquals(4, buttons.size, "面板应为两行共四个按钮")
            buttons.filter { it.action.type == Action.CALLBACK }
                .forEach { button ->
                    assertTrue(
                        !button.action.data.isNullOrEmpty(),
                        "按钮 ${button.id} 的回调数据不能为空"
                    )
                }
        } finally {
            sandbox.cleanup()
        }
    }

    @Test
    fun testEmailIsReused() = runTest {
        val sandbox = setFeedback(this, freshDatabase())
        try {
            // 第一条反馈提交后填一次邮箱
            val choose = sandbox.tapButton(
                button = typeButton(),
                data = "bug",
                id = "user-11"
            )
            sandbox.awaitReplyContaining(choose, "请填写你遇到的 Bug")
            assertNotNull(
                sandbox.awaitReplyContaining(
                    sandbox.user("user-11") says "第一条", "提交成功"
                )
            )
            sandbox.awaitReplyContaining(
                sandbox.user("user-11") says "设置邮箱", "请直接发送你的邮箱地址"
            )
            assertNotNull(
                sandbox.awaitReplyContaining(
                    sandbox.user("user-11") says "me@example.com", "邮箱已记录"
                )
            )

            // 再提交一条，不应再要一次邮箱
            val again = sandbox.tapButton(
                button = typeButton(),
                data = "bug",
                id = "user-11"
            )
            sandbox.awaitReplyContaining(again, "请填写你遇到的 Bug")
            val second = sandbox.awaitReplyContaining(
                sandbox.user("user-11") says "第二条", "提交成功"
            )
            assertNotNull(second)
            assertTrue(second.text.contains("me@example.com"))

            // 新工单也带上了邮箱
            val emails = transaction(sandbox.pluginLoader.database) {
                FeedbackTicketTable.selectAll()
                    .map { it[FeedbackTicketTable.email] }
            }
            assertEquals(2, emails.size)
            assertTrue(emails.all { it == "me@example.com" })
        } finally {
            sandbox.cleanup()
        }
    }

    @Test
    fun testEmailMigratedFromTickets() = runTest {
        // 邮箱表建立前的历史工单里已经填过邮箱
        val db = freshDatabase()
        FeedbackTicketTable.insert(
            db = db,
            type = "bug",
            body = "升级前提交的",
            userId = "user-12",
            userName = "测试用户",
            email = "old@example.com"
        )
        val sandbox = setFeedback(this, db)
        try {
            val choose = sandbox.tapButton(
                button = typeButton(),
                data = "bug",
                id = "user-12"
            )
            sandbox.awaitReplyContaining(choose, "请填写你遇到的 Bug")
            val submit = sandbox.awaitReplyContaining(
                sandbox.user("user-12") says "升级后提交的", "提交成功"
            )

            // 老邮箱被搬进邮箱表，升级后不该再问一次
            assertNotNull(submit)
            assertTrue(submit.text.contains("old@example.com"))
        } finally {
            sandbox.cleanup()
        }
    }

    private fun typeButton() = "feedback-type"

    @Test
    fun testAliasReportFromGroup() = runTest {
        val sandbox = setFeedback(this, freshDatabase())
        try {
            // 模拟舞萌插件校验通过后送来的举报
            val source = sandbox.group() says "/mai 举报别名 834 潘"
            val payload = ReportedAlias(
                game = "舞萌DX",
                songId = 834,
                songName = "Panopticon",
                alias = "潘"
            )
            sandbox.pluginLoader.subscribes.handle(
                ChannelEvent(
                    bot = sandbox.pluginLoader.bot,
                    channelName = "feedback-report",
                    data = source to json.encodeToString(payload)
                )
            )
            sandbox.advanceIdle()
            assertNotNull(
                sandbox.awaitReplyContaining(source, "请直接发送文字说明理由")
            )

            // 用户补上理由后受理，工单带上被举报的别名
            val reason = sandbox.group() says "这个别名不合适"
            assertNotNull(sandbox.awaitReplyContaining(reason, "提交成功"))

            val alias = transaction(sandbox.pluginLoader.database) {
                FeedbackTicketTable.selectAll().firstOrNull()
                    ?.get(FeedbackTicketTable.alias)
            }
            assertEquals("潘", alias)
        } finally {
            sandbox.cleanup()
        }
    }

    private suspend fun setFeedback(
        scope: kotlinx.coroutines.test.TestScope,
        database: Database,
        pendingTtl: Long = 15 * 60_000L,
        onKeyboard: (Keyboard) -> Unit = {}
    ): BotSandbox {
        // 旧实例的草稿监听仍在按 openid 匹配，必须先卸掉，否则会吃掉本次的消息
        loaded?.unload()
        val sandbox = BotSandbox(scope, database = database)
        val feedback = Feedback().apply {
            plugin = "feedback"
            pluginLoader = sandbox.pluginLoader
            this.pendingTtl = pendingTtl
            this.onKeyboard = onKeyboard
            load()
        }
        loaded = feedback
        sandbox.cleanup = { feedback.unload() }
        sandbox.advanceIdle()
        return sandbox
    }

    private companion object {
        private var loaded: Feedback? = null
        private val counter = java.util.concurrent.atomic.AtomicInteger()
        private var connected = false
        private lateinit var connectedDatabase: Database

        private fun connectIfNeeded(): Database {
            if (connected)
                return connectedDatabase
            connectedDatabase = Database.connect(
                url = "jdbc:h2:mem:feedback;MODE=MySQL;DB_CLOSE_DELAY=-1",
                driver = "org.h2.Driver",
            )
            connected = true
            return connectedDatabase
        }
    }
}