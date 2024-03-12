package xyz.xszq.bot.rhythmgame.chunithm

import io.github.oshai.kotlinlogging.KotlinLogging
import xyz.xszq.bot.dao.ProberBinding
import xyz.xszq.bot.rhythmgame.maimai.Maimai
import xyz.xszq.nereides.event.GlobalEventChannel
import xyz.xszq.nereides.event.MessageEvent
import xyz.xszq.nereides.message.ark.ListArk

object Chunithm {
    private val logger = KotlinLogging.logger("chunithm")
//    private val musics = ChunithmMusic(logger)
//    private val prober = ChunithmProberClient(logger)
    val lxnsClient = ChunithmLXNSClient(logger)
    suspend fun init() {
//        musics.updateMusicInfo(prober.getMusicList())
    }
//    private suspend fun getCredential(arg: String, event: MessageEvent): Pair<String, String>? = event.run {
//        if (arg.isNotBlank()) Pair("username", arg)
//        else ProberBinding.queryBindings(subjectId) ?: run {
//            reply(ListArk.build {
//                desc { "chunithm功能说明" }
//                prompt { "chunithm" }
//                text { "您暂未在机器人处绑定账号信息，请指定用户名，或先进行绑定操作！" }
//                text { "请使用使用“/chu bind 用户名或qq号”来绑定(此绑定与查分器绑定无关，在查分器绑定之后仍需在Bot这里绑定一次)" }
//                text { "" }
//                text { "（常见问题：为什么需要再在机器人这里绑定一次QQ号？答：因为机器人获取不到QQ号！！获取不到！！）" }
//                link("https://otmdb.cn/jump/maimaidxprober") { "点我进入查分器" }
//            })
//            null
//        }
//    }
//    fun route() = GlobalEventChannel.subscribePublicMessages("chunithm", "/chu", false) {
//        startsWith(listOf("b30", "/b30")) {
//
//        }
//    }
}