package xyz.xszq.bot.random

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import korlibs.io.util.UUID
import kotlinx.coroutines.delay
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.xszq.bot.Plugin
import xyz.xszq.bot.database.GroupCommandSettings
import xyz.xszq.bot.database.whenEnabled
import xyz.xszq.bot.event.GroupMessageEvent
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.message.At
import xyz.xszq.bot.message.Image
import xyz.xszq.bot.message.Markdown
import xyz.xszq.bot.message.PlainText
import xyz.xszq.bot.newLine
import xyz.xszq.bot.payload.markdown.Action
import xyz.xszq.bot.payload.markdown.Permission
import xyz.xszq.bot.payload.markdown.RenderData
import xyz.xszq.bot.random.payload.BilibiliResponse
import xyz.xszq.bot.random.payload.BilibiliVideoInfo
import xyz.xszq.bot.random.payload.RandomOtomads
import xyz.xszq.bot.reply
import xyz.xszq.bot.util.json
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

/**
 * 随机娱乐插件
 *
 * 聚合带随机语义的回复功能，包括随机金发图片（含自动检测）、
 * 随机数字、随机 UUID、随机音MAD，数据目录在 data/random
 */
@Suppress("unused")
class RandomPlugin: Plugin() {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    val randomImage = RandomImage()
    private val lastDetect = ConcurrentHashMap<String, Long>()

    override suspend fun load() {
        randomImage.init()

        transaction(database) {
            listOf(BlondeDetectionCache).forEach { table ->
                if (!table.exists())
                    SchemaUtils.create(table)
            }
        }

        setRoute()
        logger.info { "[随机] 插件加载完成。" }
    }

    suspend fun setRoute() = route {
        // @机器人或识别到金发时，随机回复一张金发图
        always {
            val isBlank = message.text.isBlank() &&
                message.filter { it !is PlainText && it !is At }.isEmpty()
            val hasAt = this !is GroupMessageEvent ||
                (mentions.isNotEmpty() && mentions.all { it.isSelf })
            if (isBlank && hasAt) {
                reply(Image(randomImage.random()))
                return@always
            }
            val images = message.filterIsInstance<Image>()
            if (images.isEmpty())
                return@always
            when (this) {
                is GroupMessageEvent -> whenEnabled("random.blonde.auto") {
                    handleBlondeDetect(images)
                }
                else -> handleBlondeDetect(images)
            }
        }
        // 金发识别开关
        startsWith(listOf("禁用黄毛识别", "禁止黄毛识别", "关闭黄毛识别", "禁用金发识别", "关闭金发识别")) {
            if (this is GroupMessageEvent)
                reply(showBlondePanel(disable = true))
        }
        startsWith(listOf("启用黄毛识别", "允许黄毛识别", "打开黄毛识别", "启用金发识别", "打开金发识别")) {
            if (this is GroupMessageEvent)
                reply(showBlondePanel(disable = false))
        }
        button("random/blonde") {
            val args = data.split(",")
            val disable = args[0].toInt() == 0
            val group = args[1]
            GroupCommandSettings.setEnabled(group, "random.blonde.auto", args[0] == "1")
            if (disable)
                reply("禁用金发识别成功，启用请发送“启用金发识别”。")
            else
                reply("启用金发识别成功，禁用请发送“禁用金发识别”。")
        }
        // 获取随机金发图片
        startsWith(listOf("来点金发", "来点金毛", "来点黄毛", "随机金发", "随机黄毛")) {
            reply(Image(randomImage.random()))
        }
        // 获取随机 UUID
        equalsTo("随机uuid") {
            reply(UUID.randomUUID().toString())
        }
        // 获取随机数字
        startsWith("随机数字") { raw ->
            val args = raw.split(" ").filter { it.isNotBlank() }
            reply(
                when (args.size) {
                    0 -> Random.nextInt().toString()
                    1 -> Random.nextLong(args.first().toLong()).toString()
                    2 -> Random.nextLong(
                        from = args.first().toLong(),
                        until = args.last().toLong()
                    ).toString()
                    else -> buildString {
                        appendLine("随机数字将生成 {x|下界<=x<上界} 内的数字。使用方法：")
                        appendLine("\t随机数字")
                        appendLine("\t随机数字 上界")
                        appendLine("\t随机数字 下界 上界")
                    }.trim().newLine()
                }
            )
        }
        // 获取随机音MAD
        startsWith(listOf("随机音mad", "随机音骂", "otamad")) {
            val list = client.get("https://otmdb.cn/otomad/otamad_random.json")
                .body<RandomOtomads>()

            var video: BilibiliVideoInfo? = null
            repeat(10) {
                val url = list.randomSites.random()
                val bvid = url.substringAfter("video/")
                val info = client.get(
                    "https://api.bilibili.com/x/web-interface/view?bvid=$bvid"
                ) {
                    headers[HttpHeaders.UserAgent] = buildString {
                        append("Mozilla/5.0 (Windows NT 10.0; Win64; x64) ")
                        append("AppleWebKit/537.36 (KHTML, like Gecko) ")
                        append("Chrome/75.0.3770.100 Safari/537.36")
                    }
                }.body<BilibiliResponse<BilibiliVideoInfo>>()
                info.data?.let { data ->
                    video = data
                    return@repeat
                }
                delay(500L.milliseconds)
            }
            video ?: run {
                reply("咦，获取失败了(ó﹏ò｡)")
                return@startsWith
            }
            reply(Markdown.create {
                line(bold(video.title))
                line()
                line(image(
                    url = video.pic + "@672w_378h_1c.webp",
                    alt = "img #672px #378px"
                ))
                line()
                text(buildString {
                    appendLine("UP主：${video.owner.name}")
                    appendLine("数据来自otaMAD⋅top")
                }.trim())
                keyboard {
                    row {
                        link(
                            label="查看音MAD",
                            url = "https://bilibili.com/video/${video.bvid}",
                            style = RenderData.FILLED_BLUE,
                            id = "1"
                        )
                    }
                    row {
                        at("再抽一个", "随机音mad", enter = true, id = "2")
                        link(
                            label="otaMAD⋅top",
                            url = "https://otmdb.cn/jump/otamad_top",
                            id = "3"
                        )
                    }
                }
            })
        }
    }

    /**
     * 检测消息中的图片是否为金发动漫少女
     *
     * @param images 消息中的图片
     */
    private suspend fun MessageEvent.handleBlondeDetect(
        images: List<Image>
    ) {
        val detected = images.any { img -> llmDetect(img) }
        if (!detected)
            return
        val now = System.currentTimeMillis()
        val allowed = lastDetect.compute(sender.id) { _, last ->
            if (last == null || now - last >= 5000) now else last
        } == now
        if (allowed)
            reply(Image(randomImage.random()))
    }

    /**
     * 金发识别的开关面板
     *
     * @param disable 是否禁用
     */
    private fun GroupMessageEvent.showBlondePanel(
        disable: Boolean
    ): Markdown = Markdown.create {
        line(bold("金发识别设置"))
        line()
        line("请管理员点击下方按钮确认" + (if (disable) "禁用" else "启用") + "金发识别：")
        keyboard {
            row {
                val display = if (disable) "⚠禁用金发识别" else "✅启用金发识别"
                button(
                    id = "random/blonde",
                    action = Action(
                        type = Action.CALLBACK,
                        data = (if (disable) "0" else "1") + ",${group.id}",
                        permission = Permission(Permission.OPERATORS)
                    ),
                    renderData = RenderData(
                        label = display,
                        visitedLabel = display,
                        style = RenderData.BLUE
                    )
                )
            }
        }
    }

    /**
     * 检测图片是否为金发动漫女孩
     *
     * @return 判断结果
     */
    private suspend fun llmDetect(img: Image): Boolean {
        val remote = img.remote ?: return false
        val longEdge = maxOf(remote.width, remote.height)
        val shortEdge = minOf(remote.width, remote.height)
        if (longEdge > 1600 || shortEdge > 900)
            return false
        val bytes = img.file.readAll()
        val md5 = MessageDigest.getInstance("MD5").digest(bytes)
            .joinToString("") { "%02x".format(it) }
        BlondeDetectionCache.get(md5)?.let { return it }
        val client = pluginLoader.llmClient ?: return false
        val result = runCatching {
            val content = client.chat(scene = "blonde") {
                thinking(false)
                system(buildString {
                    append("你是一名动漫角色金发识别助手。")
                    append("请判断图中的角色是否为金发")
                    append("（blonde hair，金黄色/淡金色/金色头发），")
                    append("同时角色应为女性或性别不明显的角色。")
                    append("只识别彩色图片，禁止识别黑白/灰度图片")
                    append("（黑白图片请回答false）。")
                    append("只回答true或false，不要输出任何其他内容。")
                })
                user {
                    image(img.url)
                }
            }
            content.toBooleanStrictOrNull() ?: false
        }.getOrElse { false }
        BlondeDetectionCache.put(md5, result, System.currentTimeMillis())
        return result
    }
}