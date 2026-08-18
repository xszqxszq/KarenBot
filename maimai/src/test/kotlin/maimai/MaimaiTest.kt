package xyz.xszq.bot.maimai

import kotlinx.coroutines.test.runTest
import xyz.xszq.bot.BotSandbox
import xyz.xszq.bot.assertReplied
import xyz.xszq.bot.assertRepliedAny
import xyz.xszq.bot.assertRepliedWithImage
import kotlin.test.Test
import kotlin.test.assertEquals

class MaimaiTest : MaimaiDatabaseTest() {

    @Test
    fun runAll() = runTest {
        val sandbox = setMaimai(this, database)
        try {
            testBind(sandbox)
            testUnboundQQ(sandbox)
            testUnboundProber(sandbox)

            testB50(sandbox, "maxscore")
            testB50(sandbox)
            testB40(sandbox)

            testScoreList(sandbox)

            testLevelList(sandbox)
            testLevelComplete(sandbox)
            testLevelIncomplete(sandbox)

            testInfo(sandbox)

            testCourse(sandbox)

            testCombo(sandbox)
            testSongRating(sandbox)
            testRecent(sandbox)

            testProgress(sandbox)

            testMusicId(sandbox)
            testMusicRandom(sandbox)
            testMusicSearch(sandbox)

            testMusicLevelSearch(sandbox)
            testMusicDesignerSearch(sandbox)
            testMusicVersionSearch(sandbox)
            testMusicArtistSearch(sandbox)
            testMusicRegexSearch(sandbox)
            testMusicBpmSearch(sandbox)
            testMusicComboSearch(sandbox)

            testMusicAliasesList(sandbox)
            testMusicAddAlias(sandbox)
            testMusicDeleteAlias(sandbox)
            testMusicToday(sandbox)
            testMusicButtons(sandbox)
            testMusicButtonsExtra(sandbox)
            testMusicDifficultyId(sandbox)
            testMusicWhatSong(sandbox)
            testMusicFitLevel(sandbox)
            testMusicGenreSongs(sandbox)
            testPreview(sandbox)

            testCalc(sandbox)

            testSettingsProber(sandbox)
            testSettingsCollections(sandbox)
            testSettingsHelp(sandbox)
            testCompatibility(sandbox)
            testSettingsQuickProber(sandbox)
            testCompatibilityExtra(sandbox)
            testSettingsNameplate(sandbox)
            testSettingsDefault(sandbox)

            testUpdate(sandbox)

            testHelp(sandbox)

            testQueue(sandbox)

            testGuess(sandbox)
            testGuessOpening(sandbox)
            testGuessAdmin(sandbox)
        } finally {
            sandbox.cleanup()
        }
    }

    private suspend fun testBind(sandbox: BotSandbox) {
        sandbox.clear()
        val msg = sandbox.user() says "/bind 943551369"
        assertReplied(sandbox, msg, "绑定成功")
    }

    private suspend fun testUnboundQQ(sandbox: BotSandbox) {
        sandbox.clear()
        assertReplied(sandbox, sandbox.user("unbound-user") says "/b50", "请输入您的QQ号")
    }

    private suspend fun testUnboundProber(sandbox: BotSandbox) {
        sandbox.clear()
        assertReplied(sandbox, sandbox.user("u2") says "/bind 1145148101919893", "还未在查分器上绑定QQ号")
    }

    private suspend fun testSettingsProber(sandbox: BotSandbox) {
        sandbox.clear()
        assertReplied(sandbox, sandbox.user() says "设置查分器 水鱼", "设置查分器成功")
        assertReplied(sandbox, sandbox.user() says "设置查分器 落雪", "设置查分器成功")
        assertReplied(sandbox, sandbox.user() says "设置查分器 自动", "设置查分器成功")
    }

    private suspend fun testSettingsCollections(sandbox: BotSandbox) {
        sandbox.clear()
        assertReplied(sandbox, sandbox.user() says "设置头像 1", "设置头像成功")
        assertReplied(sandbox, sandbox.user() says "设置牌子 1", "设置牌子成功")
    }

    private suspend fun testSettingsHelp(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "设置mai")
    }

    private suspend fun testCompatibility(sandbox: BotSandbox) {
        sandbox.clear()
        assertReplied(sandbox, sandbox.user() says "兼容模式", "兼容模式启用成功")
        assertReplied(sandbox, sandbox.user() says "兼容模式 关闭", "兼容模式禁用成功")
    }

    private suspend fun testSettingsQuickProber(sandbox: BotSandbox) {
        sandbox.clear()
        assertReplied(sandbox, sandbox.user() says "设置水鱼", "设置查分器成功")
        assertReplied(sandbox, sandbox.user() says "水鱼", "设置查分器成功")
        assertReplied(sandbox, sandbox.user() says "设置落雪", "设置查分器成功")
        assertReplied(sandbox, sandbox.user() says "落雪", "设置查分器成功")
    }

    private suspend fun testCompatibilityExtra(sandbox: BotSandbox) {
        sandbox.clear()
        assertReplied(sandbox, sandbox.user() says "取消兼容模式", "兼容模式禁用成功")
        assertReplied(sandbox, sandbox.user() says "打开兼容模式", "兼容模式启用成功")
        assertReplied(sandbox, sandbox.user() says "启用兼容模式", "兼容模式启用成功")
        assertReplied(sandbox, sandbox.user() says "禁用兼容模式", "兼容模式禁用成功")
    }

    private suspend fun testSettingsNameplate(sandbox: BotSandbox) {
        sandbox.clear()
        assertReplied(sandbox, sandbox.user() says "设置姓名框 1", "设置牌子成功")
    }

    private suspend fun testSettingsDefault(sandbox: BotSandbox) {
        sandbox.clear()
        assertReplied(sandbox, sandbox.user() says "/mai 默认", "设置成功")
    }

    private suspend fun testB50(sandbox: BotSandbox, args: String? = null) {
        sandbox.clear()
        val content = args?.let { "/b50 $it" } ?: "/b50"
        assertRepliedWithImage(sandbox, sandbox.user() says content)
    }

    private suspend fun testB40(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedWithImage(sandbox, sandbox.user() says "/b40")
    }

    private suspend fun testLevelList(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedWithImage(sandbox, sandbox.user() says "13定数表")
    }

    private suspend fun testScoreList(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedWithImage(sandbox, sandbox.user() says "13分数列表")
    }

    private suspend fun testLevelComplete(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedWithImage(sandbox, sandbox.user() says "13完成表")
    }

    private suspend fun testLevelIncomplete(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "13未完成表")
    }

    private suspend fun testInfo(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedWithImage(sandbox, sandbox.user() says "info 852")
    }

    private suspend fun testCourse(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "段位表 初段")
    }

    private suspend fun testCombo(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "东方50")
        assertRepliedAny(sandbox, sandbox.user() says "寸50")
        assertRepliedAny(sandbox, sandbox.user() says "13ap50")
    }

    private suspend fun testSongRating(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "歌50 852")
    }

    private suspend fun testRecent(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "r50")
    }

    private suspend fun testProgress(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "13进度")
    }

    private suspend fun testMusicId(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "id 852")
    }

    private suspend fun testMusicRandom(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "随个")
    }

    private suspend fun testMusicSearch(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "查歌 TiamaT")
    }

    private suspend fun testMusicLevelSearch(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "定数查歌 12.0")
    }

    private suspend fun testMusicDesignerSearch(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "谱师查歌 ニャイン")
    }

    private suspend fun testMusicVersionSearch(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "版本查歌 DX")
    }

    private suspend fun testMusicArtistSearch(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "曲师查歌 Team Grimoire")
    }

    private suspend fun testMusicRegexSearch(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "正则查歌 .*")
    }

    private suspend fun testMusicBpmSearch(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "BPM查歌 180")
    }

    private suspend fun testMusicComboSearch(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "搜索 东方")
    }

    private suspend fun testMusicAliasesList(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "852有什么别名")
    }

    private suspend fun testMusicAddAlias(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "添加别名 852 测试别名")
    }

    private suspend fun testMusicDeleteAlias(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "添加别名 852 testdel")
        assertReplied(sandbox, sandbox.user() says "删除别名 852 testdel", "别名删除成功")
        assertReplied(sandbox, sandbox.user() says "删除别名", "使用方法")
        assertReplied(sandbox, sandbox.user() says "删除别名 852 不存在的别名", "该别名不存在")
        assertReplied(sandbox, sandbox.user() says "删除别名 不存在的歌曲 任意", "未找到该歌曲")
        sandbox.clear()
        sandbox.user("not-admin") says "删除别名 852 testdel"
        assertEquals(0, sandbox.replies.size)
    }

    private suspend fun testMusicToday(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "今日舞萌")
    }

    private suspend fun testMusicButtons(sandbox: BotSandbox) {
        sandbox.clear()
        assertReplied(sandbox, sandbox.tapButton("maimai-id", "852"), "TiamaT")
        assertRepliedAny(sandbox, sandbox.tapButton("maimai-search-word", "TiamaT\n1"))
        assertRepliedAny(sandbox, sandbox.tapButton("maimai-search-level", "12.0:12.0\n1"))
    }

    private suspend fun testMusicButtonsExtra(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.tapButton("maimai-search-level-fit", "12.0:12.0\n1"))
        assertRepliedAny(sandbox, sandbox.tapButton("maimai-search-designer", "ニャイン\n1"))
        assertRepliedAny(sandbox, sandbox.tapButton("maimai-search-version", "DX\n1"))
        assertRepliedAny(sandbox, sandbox.tapButton("maimai-search-artist", "Team Grimoire\n1"))
        assertRepliedAny(sandbox, sandbox.tapButton("maimai-search-bpm", "180\n1"))
        assertRepliedAny(sandbox, sandbox.tapButton("maimai-search-combo", "东方\n1"))
    }

    private suspend fun testMusicDifficultyId(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "紫id852")
    }

    private suspend fun testMusicWhatSong(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "852是什么歌")
    }

    private suspend fun testMusicFitLevel(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "拟合定数查歌 13.0")
    }

    private suspend fun testMusicGenreSongs(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "东方有什么歌")
    }

    private suspend fun testPreview(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "预览 189")
    }

    private suspend fun testCalc(sandbox: BotSandbox) {
        sandbox.clear()
        assertReplied(sandbox, sandbox.user() says "分数线 紫852 100.5", "分数线")
    }

    private suspend fun testUpdate(sandbox: BotSandbox) {
        sandbox.clear()
        assertReplied(sandbox, sandbox.user() says "绑定水鱼 testtoken", "绑定成功")
        assertRepliedAny(sandbox, sandbox.user() says "更新")
    }

    private suspend fun testHelp(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "/mai")
    }

    private suspend fun testQueue(sandbox: BotSandbox) {
        sandbox.clear()
        assertReplied(sandbox, sandbox.group() says "排卡管理 添加机厅 测试", "添加机厅成功")
        assertReplied(sandbox, sandbox.group() says "排卡管理 添加别名 测试 test", "添加机厅别名成功")
        assertReplied(sandbox, sandbox.group() says "排卡管理 查看别名 测试", "机厅别名如下")
        assertReplied(sandbox, sandbox.group() says "排卡管理 删除别名 测试 test", "删除机厅别名成功")
        assertReplied(sandbox, sandbox.group() says "测试3", "更新成功")
        assertRepliedAny(sandbox, sandbox.group() says "几")
        assertReplied(sandbox, sandbox.group() says "排卡管理 删除机厅 测试", "删除机厅成功")
        assertReplied(sandbox, sandbox.group() says "排卡管理 添加分组 不存在", "该分组不存在")
    }

    private suspend fun testGuess(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "猜歌")
        assertRepliedAny(sandbox, sandbox.user() says "不玩了")
    }

    private suspend fun testGuessOpening(sandbox: BotSandbox) {
        sandbox.clear()
        assertRepliedAny(sandbox, sandbox.user() says "舞萌开字母")
        assertRepliedAny(sandbox, sandbox.user() says "开字母 s")
        assertRepliedAny(sandbox, sandbox.user() says "开歌 852")
        assertRepliedAny(sandbox, sandbox.user() says "不玩了")
    }

    private suspend fun testGuessAdmin(sandbox: BotSandbox) {
        sandbox.clear()
        assertReplied(sandbox, sandbox.group() says "禁用猜歌", "猜歌设置")
        assertReplied(sandbox, sandbox.tapButton("admin/guess", "0,test-group"), "禁用猜歌成功")
        assertReplied(sandbox, sandbox.group() says "猜歌", "当前群猜歌已被禁用")
        assertReplied(sandbox, sandbox.group() says "启用猜歌", "猜歌设置")
        assertReplied(sandbox, sandbox.tapButton("admin/guess", "1,test-group"), "启用猜歌成功")
        assertRepliedAny(sandbox, sandbox.group() says "猜歌")
        assertRepliedAny(sandbox, sandbox.group() says "不玩了")
    }
}
