package xyz.xszq.bot

import io.mockk.coEvery
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import xyz.xszq.bot.admin.Admin
import xyz.xszq.bot.event.ChannelEvent
import xyz.xszq.bot.event.GroupMessageEvent
import xyz.xszq.bot.payload.AdminCheckRequest
import kotlin.test.Test
import kotlin.test.assertEquals

class AdminTest {

    private val adminId = "BD11EC5ADAE7A0CA792984F3EC63A165"

    private suspend fun setAdmin(scope: TestScope): BotSandbox {
        val sandbox = BotSandbox(scope, mockTencentCos())
        val admin = Admin().apply {
            plugin = "admin"
            pluginLoader = sandbox.pluginLoader
        }
        admin.load()
        sandbox.cleanup = { admin.unload() }
        return sandbox
    }

    @Test
    fun runAll() = runTest {
        val sandbox = setAdmin(this)
        try {
            testMarkdown(sandbox)
            testLog(sandbox)
            testReloadConfig(sandbox)
            testReloadUnknown(sandbox)
            testMsg(sandbox)
            testMsgmd(sandbox)
            testMsgQueue(sandbox)
            testRecall(sandbox)
            testNonAdmin(sandbox)
            testAdminCheck(sandbox)
        } finally {
            sandbox.cleanup()
        }
    }

    private suspend fun testMarkdown(sandbox: BotSandbox) {
        val event = sandbox.user(adminId) says "markdown 测试内容"
        assertReplied(sandbox, event, "测试内容")
    }

    private suspend fun testLog(sandbox: BotSandbox) {
        val before = KarenBotApplication.debugLog
        val on = sandbox.user(adminId) says "log"
        assertReplied(sandbox, on, if (before) "调试日志已关闭。" else "调试日志已开启。")
        assertEquals(!before, KarenBotApplication.debugLog)
        val off = sandbox.user(adminId) says "log"
        assertReplied(sandbox, off, if (before) "调试日志已开启。" else "调试日志已关闭。")
        assertEquals(before, KarenBotApplication.debugLog)
    }

    private suspend fun testReloadConfig(sandbox: BotSandbox) {
        val event = sandbox.user(adminId) says "reload config"
        assertReplied(sandbox, event, "重载配置完成。")
    }

    private suspend fun testReloadUnknown(sandbox: BotSandbox) {
        val event = sandbox.user(adminId) says "reload 不存在的插件"
        assertReplied(sandbox, event, "未找到相应插件。")
    }

    private suspend fun testMsg(sandbox: BotSandbox) {
        sandbox.clear()
        sandbox.user(adminId) says "msg test-group 群消息内容"
        check(sandbox.replies.any { it is GroupMessageEvent && it.message.content.contains("群消息内容") }) {
            "Expected group message, got: ${sandbox.replies.map { it.message.content }}"
        }
    }

    private suspend fun testMsgmd(sandbox: BotSandbox) {
        sandbox.clear()
        sandbox.user(adminId) says "msgmd test-group markdown内容"
        check(sandbox.replies.any { it is GroupMessageEvent && it.message.content.contains("markdown内容") }) {
            "Expected group markdown message, got: ${sandbox.replies.map { it.message.content }}"
        }
    }

    private suspend fun testMsgQueue(sandbox: BotSandbox) {
        sandbox.clear()
        coEvery {
            sandbox.pluginLoader.api.sendGroupMessage(any(), "待补发内容", any(), any(), any(), null, any(), any(), any())
        } returns false
        sandbox.user(adminId) says "msg test-group 待补发内容"
        sandbox.group() says "随便一句话"
        check(sandbox.replies.any { it is GroupMessageEvent && it.message.content.contains("待补发内容") }) {
            "Expected queued message flushed, got: ${sandbox.replies.map { it.message.content }}"
        }
    }

    private suspend fun testRecall(sandbox: BotSandbox) {
        val fail = sandbox.user(adminId) says "recall test-group msg-1"
        assertReplied(sandbox, fail, "撤回失败")
        coEvery { sandbox.pluginLoader.api.recallGroupMessage(any(), any()) } returns true
        val ok = sandbox.user(adminId) says "recall test-group msg-1"
        assertReplied(sandbox, ok, "撤回成功")
    }

    private suspend fun testNonAdmin(sandbox: BotSandbox) {
        sandbox.clear()
        sandbox.user("random-user") says "markdown 不应回复"
        assertEquals(0, sandbox.replies.size)
    }

    private suspend fun testAdminCheck(sandbox: BotSandbox) {
        val ok = CompletableDeferred<Boolean>()
        sandbox.pluginLoader.manualTrigger(
            ChannelEvent(sandbox.pluginLoader.bot, channelName = "admin-check", data = AdminCheckRequest(adminId, ok))
        )
        sandbox.advanceIdle()
        assertEquals(true, ok.await())
        val no = CompletableDeferred<Boolean>()
        sandbox.pluginLoader.manualTrigger(
            ChannelEvent(sandbox.pluginLoader.bot, channelName = "admin-check", data = AdminCheckRequest("random-user", no))
        )
        sandbox.advanceIdle()
        assertEquals(false, no.await())
    }
}