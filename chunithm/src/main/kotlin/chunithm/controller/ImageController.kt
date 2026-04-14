package xyz.xszq.bot.chunithm.controller

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import xyz.xszq.bot.*
import xyz.xszq.bot.Chunithm.Companion.textMode
import xyz.xszq.bot.chunithm.exception.NoDataException
import xyz.xszq.bot.chunithm.music.UserQueryParams
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.message.Markdown
import xyz.xszq.bot.payload.markdown.Keyboard
import xyz.xszq.bot.payload.markdown.MarkdownData


@Suppress("unused")
class ImageController(
    override val chunithm: Chunithm
): Controller(chunithm) {

    override suspend fun setRoute() = rhythm {
        // b50 及扩展功能
        listOf(30, 50).forEach { total ->
            commandEndsWith(total.toString()) { raw ->
                val args = raw.split(" ")
                val command = args.first()

                val queryArgs = args.getOrNull(1) ?: ""
                var user: UserQueryParams? = null
                runCatching {
                    when (command) {
                        "b" -> {
                            user = chunithm.query.getQueryParams(this, queryArgs)
                            handleRating(user)
                        }
                    }
                }.onFailure { e ->
                    e.printStackTrace()
                    handleError(this, e, user)
                }
            }
        }
    }
    suspend fun Image?.sendResultImage(
        command: String,
        event: MessageEvent,
        text: String ?= null,
        page: Int ?= null,
        totalPages: Int ?= null
    ) = event.run {
        this@sendResultImage ?: return@run
        if (textMode()) {
            send(this, text)
            return
        }
        upload(event) { url ->
            reply(Markdown(MarkdownData(buildString {
                appendLine("**查询结果**")
                appendLine()
                appendLine("![img #${width}px #${height}px]($url)")
                appendLine()
                append(text ?: "")
            }), Keyboard.create {
                row {
                    at("💯我也要查", "/chu " + command.trim(), id = "1")
//                    link("随心配", "https://otmdb.cn/bot/maimai/combo", enter = true, id = "2")
//                    at("🎨修改设置", "设置mai", enter = true, id = "3")
                }
                page?.let {
                    if (totalPages == null || totalPages <= 1)
                        return@let
                    row {
                        if (page > 1)
                            at("⬅️上一页", "$command ${page - 1}", enter = true, id = "4")
                        if (page < totalPages)
                            at("➡️下一页", "$command ${page + 1}", enter = true, id = "5")
                    }
                }
            }))
        }
    }

    suspend fun MessageEvent.handleRating(
        user: UserQueryParams
    ) {
        val (response, api) = chunithm.query.rating(user)
        if (response.oldRatingList.isEmpty() && response.newRatingList.isEmpty()) {
            throw NoDataException(api = api)
        }
        val (elapsed, result) = countTime {
            chunithm.image.rating.bests(response, api.name)
        }
        result.sendResultImage("b50", this, "生成时间：${elapsed}ms")
    }
    suspend fun Image.send(
        event: MessageEvent,
        message: String ?= null
    ): Unit = useTempFile { file ->
        val bytes = this.encodeToData(EncodedImageFormat.JPEG, 95)!!.bytes
        file.writeBytes(bytes)
        message ?.let {
            event.reply(xyz.xszq.bot.message.Image(file) + it.toPlainText())
        } ?: run {
            event.reply(xyz.xszq.bot.message.Image(file))
        }
    }
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun Image.upload(
        event: MessageEvent,
        handle: suspend MessageEvent.(String) -> Unit
    ): Unit = useTempFile(suffix = ".jpg") { file ->
        val bytes = this.encodeToData(EncodedImageFormat.JPEG, 95)!!.bytes
        val uploaded = event.bot.cos.uploadBinary(bytes, suffix = ".jpg")
        handle.invoke(event, uploaded.url)
        chunithm.scope.launch {
            delay(10000L)
            event.bot.cos.deleteFromCos(uploaded.filename)
        }
    }

    companion object {
        suspend fun <T> countTime(block: suspend () -> T): Pair<Long, T> {
            val start = System.currentTimeMillis()
            val result = block()
            return Pair(System.currentTimeMillis() - start, result)
        }
    }
}