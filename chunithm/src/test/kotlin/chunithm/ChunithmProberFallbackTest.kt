package xyz.xszq.bot.chunithm

import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import xyz.xszq.bot.BotSandbox
import xyz.xszq.bot.assertReplied
import xyz.xszq.bot.assertRepliedWithImage
import xyz.xszq.bot.chunithm.component.ChunithmData
import xyz.xszq.bot.chunithm.database.MaimaiSettingsTable
import xyz.xszq.bot.chunithm.database.ProberBindTable
import xyz.xszq.bot.chunithm.database.QQBindTable
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.reply
import xyz.xszq.bot.subscribe.Channel
import kotlin.test.Test
import kotlin.test.assertEquals

class ChunithmProberFallbackTest : ChunithmDatabaseTest() {
    private companion object {
        const val NEW_FRIEND_CODE = 722520985289030L
        const val STALE_FRIEND_CODE = 1L
        const val TEST_QQ = 10001L
        const val SCORE_MUSIC_ID = 3
        const val BIND_PROMPT = "绑定查分器"
    }

    @Test
    fun runAll() = runTest {
        val data = ChunithmData(dataPath = "./data/chunithm")
        val prober = MockLxnsProber(data)
        val lxns = prober.backend()
        data.load(lxns)
        val sandbox = setChunithm(
            scope = this,
            database = database,
            backends = listOf(mockUnboundDivingFish(), lxns)
        )
        sandbox.pluginLoader.subscribes.subscribe(
            "maimai", Channel<MessageEvent>("rhythm-game-bind") { target ->
                target.reply(BIND_PROMPT)
            }
        )
        try {
            testFetchFriendCodeByRefresh(sandbox, prober)
            testFetchFriendCodeByQQ(sandbox, prober)
            testFetchFriendCodeFailedShowsBindPrompt(sandbox, prober)
            testStaleFriendCodeRefresh(sandbox, prober)
            testStaleFriendCodeFailedShowsBindPrompt(sandbox, prober)
            testSongRecordSelfHeal(sandbox, prober)
            testScoreListWithoutRefreshShowsBindPrompt(sandbox, prober)
            testOtherPlayerNotFound(sandbox, prober)
        } finally {
            sandbox.cleanup()
        }
    }

    /**
     * 有 refresh 但没有好友码时用 OAuth 拉取好友码
     */
    private suspend fun testFetchFriendCodeByRefresh(
        sandbox: BotSandbox,
        prober: MockLxnsProber
    ) {
        val openid = "user-refresh"
        setBindings(openid, refresh = "test-refresh", prefer = "lxns")
        prober.reset()
        sandbox.clear()
        assertRepliedWithImage(sandbox, sandbox.user(openid) says "/chu b50")
        assertEquals(NEW_FRIEND_CODE.toString(), friendCode(openid))
    }

    /**
     * 没有 refresh 但有 QQ 号时用 QQ 拉取好友码
     */
    private suspend fun testFetchFriendCodeByQQ(
        sandbox: BotSandbox,
        prober: MockLxnsProber
    ) {
        val openid = "user-qq"
        setBindings(openid, qq = TEST_QQ, prefer = "lxns")
        prober.reset()
        prober.oauthFriendCode = null
        prober.qqFriendCode = NEW_FRIEND_CODE
        sandbox.clear()
        assertRepliedWithImage(sandbox, sandbox.user(openid) says "/chu b50")
        assertEquals(NEW_FRIEND_CODE.toString(), friendCode(openid))
    }

    /**
     * 拉不到好友码时提示重新绑定，而不是查询失败
     */
    private suspend fun testFetchFriendCodeFailedShowsBindPrompt(
        sandbox: BotSandbox,
        prober: MockLxnsProber
    ) {
        val openid = "user-nobind"
        setBindings(openid, prefer = "lxns")
        prober.reset()
        prober.oauthFriendCode = null
        sandbox.clear()
        assertReplied(sandbox, sandbox.user(openid) says "/chu b50", BIND_PROMPT)
    }

    /**
     * 好友码失效时重新拉取后继续查询
     */
    private suspend fun testStaleFriendCodeRefresh(
        sandbox: BotSandbox,
        prober: MockLxnsProber
    ) {
        val openid = "user-stale"
        setBindings(
            openid,
            friendCode = STALE_FRIEND_CODE,
            refresh = "test-refresh",
            prefer = "lxns"
        )
        prober.reset()
        sandbox.clear()
        assertRepliedWithImage(sandbox, sandbox.user(openid) says "/chu b50")
        assertEquals(NEW_FRIEND_CODE.toString(), friendCode(openid))
    }

    /**
     * 好友码失效且拉不到新好友码时提示重新绑定
     */
    private suspend fun testStaleFriendCodeFailedShowsBindPrompt(
        sandbox: BotSandbox,
        prober: MockLxnsProber
    ) {
        val openid = "user-stale-failed"
        setBindings(openid, friendCode = STALE_FRIEND_CODE, prefer = "lxns")
        prober.reset()
        sandbox.clear()
        assertReplied(sandbox, sandbox.user(openid) says "/chu b50", BIND_PROMPT)
    }

    /**
     * 单曲成绩的好友码失效时同样重新拉取
     */
    private suspend fun testSongRecordSelfHeal(
        sandbox: BotSandbox,
        prober: MockLxnsProber
    ) {
        val openid = "user-song"
        setBindings(
            openid,
            friendCode = STALE_FRIEND_CODE,
            refresh = "test-refresh",
            prefer = "lxns"
        )
        prober.reset()
        prober.playerCodes = setOf(STALE_FRIEND_CODE, NEW_FRIEND_CODE)
        prober.ratingCodes = setOf(STALE_FRIEND_CODE, NEW_FRIEND_CODE)
        prober.songCodes = setOf(NEW_FRIEND_CODE)
        sandbox.clear()
        assertRepliedWithImage(sandbox, sandbox.user(openid) says "/chu 歌50 $SCORE_MUSIC_ID")
        assertEquals(NEW_FRIEND_CODE.toString(), friendCode(openid))
    }

    /**
     * 分数列表需要 OAuth 而没有 refresh 时提示重新绑定
     */
    private suspend fun testScoreListWithoutRefreshShowsBindPrompt(
        sandbox: BotSandbox,
        prober: MockLxnsProber
    ) {
        val openid = "user-records"
        setBindings(openid, friendCode = NEW_FRIEND_CODE, prefer = "lxns")
        prober.reset()
        sandbox.clear()
        assertReplied(sandbox, sandbox.user(openid) says "/chu 13分数列表", BIND_PROMPT)
    }

    /**
     * 查询他人好友码失效时提示用户不存在
     */
    private suspend fun testOtherPlayerNotFound(
        sandbox: BotSandbox,
        prober: MockLxnsProber
    ) {
        val openid = "user-other"
        setBindings(openid, prefer = "lxns")
        prober.reset()
        sandbox.clear()
        val reply = sandbox.awaitReply(sandbox.user(openid) says "/chu b50 100000000000000")
        assertEquals("您查询的用户不存在。", reply ?.text)
    }

    private suspend fun setBindings(
        openid: String,
        friendCode: Long ?= null,
        refresh: String ?= null,
        qq: Long ?= null,
        prefer: String ?= null
    ) {
        newSuspendedTransaction(db = database) {
            friendCode ?.let {
                ProberBindTable[openid, "lxns", "chunithm-friend-code"] = it.toString()
            }
            refresh ?.let {
                ProberBindTable[openid, "lxns", "refresh"] = it
            }
            qq ?.let { value ->
                QQBindTable.insert {
                    it[id] = openid
                    it[QQBindTable.qq] = value
                }
            }
            prefer ?.let {
                MaimaiSettingsTable[openid, "prober"] = it
            }
        }
    }

    private suspend fun friendCode(openid: String) =
        ProberBindTable[openid, "lxns", "chunithm-friend-code"]
}
