package xyz.xszq.bot.maimai.controller

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import korlibs.io.util.UUID
import xyz.xszq.bot.Maimai
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.maimai.api.DivingFish
import xyz.xszq.bot.maimai.component.WaitingEventData
import xyz.xszq.bot.maimai.database.DivingFishBindTable
import xyz.xszq.bot.maimai.music.MusicDifficulty
import xyz.xszq.bot.maimai.payload.DivingFishRecordSimple
import xyz.xszq.bot.maimai.payload.DivingFishUpdateResponse
import xyz.xszq.bot.message.Image
import xyz.xszq.bot.message.RemoteImage
import xyz.xszq.bot.reply

@Suppress("unused")
class UpdateController(
    override val maimai: Maimai
): Controller(maimai) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }

    override suspend fun setRoute() = rhythm {
        startsWith(listOf("更新", "导")) {
            if (text.trim() !in listOf("更新", "导"))
                return@startsWith
            if (DivingFishBindTable[sender.id] == null) {
                hintBind()
                return@startsWith
            }
            if (reference ?.any { it is RemoteImage } == true || message.any { it is Image }) {
                val llmClient = bot.pluginLoader.llmClient
                if (llmClient == null) return@startsWith
                val images = (message.filterIsInstance<Image>().mapNotNull {
                    it.url.ifBlank { null }
                }) + (reference ?.filterIsInstance<RemoteImage>() ?.let {
                    it.map { image -> image.url }
                } ?: emptyList())
                val divingFish = maimai.backend("diving-fish") as DivingFish
                val records = maimai.query.parseScoreImage(llmClient, images).filter {
                    it.game == "maimai"
                }.map { record ->
                    val music = maimai.musics().first { record.title.lowercase() in it.name.lowercase() && record.type == it.type.value }
                    DivingFishRecordSimple(
                        title = divingFish.getDivingFishTitle(music.id, music.name),
                        achievements = record.achievement.replace("%", "").toDouble(),
                        dxScore = record.deluxeScore,
                        fc = record.combo,
                        fs = record.sync,
                        levelIndex = MusicDifficulty.from(record.difficulty)!!.value,
                        type = record.type
                    )
                }
                println(records)
                if (records.isEmpty())
                    return@startsWith

                val importToken = DivingFishBindTable[sender.id] ?: return@startsWith
                val response = client.post(
                    "https://www.diving-fish.com/api/maimaidxprober/player/update_records"
                ) {
                    headers {
                        append("Import-Token", importToken)
                    }
                    contentType(ContentType.Application.Json)
                    setBody(records)
                }
                if (!response.status.isSuccess()) {
                    reply("更新失败，请稍后重试")
                    return@startsWith
                }
                val result = response.body<DivingFishUpdateResponse>()
                reply("更新成功，已更新${result.updates + result.creates}条记录。")
                return@startsWith
            }
            val token = UUID.randomUUID().toString().replace("-", "")
            maimai.api.updateTokens[token] = WaitingEventData(this)
            reply("${maimai.config.apiServer}/update?token=$token")
            reply("请连接代理，并复制上方链接至微信中打开") {
                brief("更新查分器", "请连接代理（可点击下方查看教程），然后复制上方链接至微信中打开：")
                keyboard {
                    row {
                        link("设置代理", "https://bot-docs.otmdb.cn/maimai/update")
                    }
                }
            }
        }
        startsWith("绑定水鱼") { token ->
            if (token.isBlank()) {
                hintBind()
                return@startsWith
            }
            DivingFishBindTable.update(sender.id, token)

            reply("水鱼token绑定成功。") {
                brief("绑定水鱼", "水鱼token绑定成功。")
                keyboard {
                    row {
                        at("点击更新", "更新", enter = true)
                    }
                }
            }
        }
    }

    private suspend fun MessageEvent.hintBind() {
        reply("请使用“/绑定水鱼 水鱼成绩导入Token”来设置！") {
            brief("更新查分器", "请先点击下方输入水鱼成绩导入Token：")
            keyboard {
                row {
                    at("⬇点我输入", "/绑定水鱼 ")
                }
            }
        }
    }
}