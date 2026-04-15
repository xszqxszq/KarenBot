package xyz.xszq.bot.chunithm.component.image.templates

import korlibs.io.util.toStringDecimal
import org.jetbrains.skia.Image
import xyz.xszq.bot.chunithm.component.image.RatingRenderParams
import xyz.xszq.bot.chunithm.music.*
import xyz.xszq.shinobu.dom.Div
import xyz.xszq.shinobu.style.BackgroundPosition
import xyz.xszq.shinobu.style.BackgroundSize
import xyz.xszq.shinobu.template.Template
import xyz.xszq.shinobu.template.TemplateManager
import kotlin.math.floor

class RatingTemplate(
    private val manager: TemplateManager,
    private val resourcePath: String,
    private val newestVersion: GameVersion
) {
    fun title(
        backend: String,
        oldRating: Double,
        newRating: Double
    ) = buildString {
        append("[${backend}] ")
        append("B30: $oldRating, ")
        append("N20: $newRating")
    }
    /**
     * 最佳成绩列表模板
     * @param total 取最佳多少项成绩进行统计
     * @param info 查询信息
     * @param backend 数据源名称
     */
    suspend fun bests(
        info: RatingResponse,
        backend: String
    ): Image {
        val newCount = 20
        val oldCount = 30

        val oldRatingSum = info.oldRatingList.sumOf { it.rating }
        val newRatingSum = info.newRatingList.sumOf { it.rating }
        val oldRating = floor(oldRatingSum / oldCount * 100) / 100.0
        val newRating = floor(newRatingSum / newCount * 100) / 100.0
        val ratingSum = (oldRatingSum + newRatingSum) / 50
        val rating = floor(ratingSum * 100) / 100.0

        val title = title(backend, oldRating, newRating)

        return template(
            RatingRenderParams(
                nickname = info.player.nickname,
                rating = rating,
                ratingColor = Rating.color(rating),
                level = info.player.level,
                avatar = info.settings?.avatar ?: 0,
                plate = info.settings?.plate ?: 140,
                title = title,
                oldCount = oldCount,
                newCount = newCount,
                oldRecords = info.oldRatingList.take(oldCount),
                newRecords = info.newRatingList.take(newCount),
            )
        )
    }

    /**
     * 生成模板
     * @param params 渲染参数
     */
    private suspend fun template(
        params: RatingRenderParams
    ): Image {
        val template = manager["rating"]!!

        val main = template["main"]!!.modify {
            div("upper/header") {
                header(params)
                text("name-title") {
                    text = params.title
                }
            }

            div("upper/scores") {
                params.oldRecords.forEachIndexed { index, record ->
                    add(template.score(index, record, params))
                }
                repeat(params.oldCount - params.oldRecords.size) {
                    add(template["music"]!!)
                }
            }
            div("upper/scores-new") {
                params.newRecords.forEachIndexed { index, record ->
                    add(template.score(index, record, params))
                }
                repeat(params.newCount - params.newRecords.size) {
                    add(template["music"]!!)
                }
            }

            if (params.isScoreList) {
                style.backgroundSize = BackgroundSize.COVER
                style.backgroundPosition = BackgroundPosition.TOP_CENTER
                style.minHeight = style.height
                style.height = null
            }
        }

        return template.render(main)
    }

    /**
     * 顶栏展示个人信息
     * @param params 渲染模板
     */
    private fun Div.header(
        params: RatingRenderParams
    ) {
        background = "$resourcePath/plates/${params.plate}.png"
        div("info/rating-line") {
            image("rating") {
                src = "rating_${params.ratingColor}.png"
            }
            div("rating-container") {
                image("rating-dot") {
                    src = "rating_${params.ratingColor}_dot.png"
                }
                val ratingString = Rating.stringWithoutDot(params.rating)
                ratingString.forEachIndexed { index, digit ->
                    image("rating-${index + 1}") {
                        src = "rating_${params.ratingColor}_${digit}.png"
                    }
                }
            }
        }
        text("level") {
            text = params.level.toString()
        }
        text("name") {
            text = params.nickname
        }
        image("avatar") {
            src = "$resourcePath/avatars/${params.avatar}.png"
        }
    }

    /**
     * 展示单个成绩
     * @param index 序号
     * @param record 成绩记录
     * @param params 渲染参数
     */
    private fun Template.score(
        index: Int,
        record: Record,
        params: RatingRenderParams
    ) = this["music"]!!.modify {
        background = "base_${record.chart.difficulty.name}.png"
        div("cover") {
            background = "$resourcePath/covers/${record.music.resourceId}_s.jpg"
        }
        text("index-id") {
            text = "#${index + 1} ${record.music.id}"
        }
        text("level-rating") {
            val levelValue = record.chart.levelValue.toStringDecimal(1)
            val ratingValue = (floor(record.rating * 100) / 100.0).toStringDecimal(2)
            text = "$levelValue→$ratingValue"
        }
        text("title") {
            text = record.music.name
        }
        val (acc1, acc2) = Rate.formatted(record.achievement)
        text("achievement-1") {
            text = acc1
        }
        text("achievement-2") {
            text = acc2
        }
        image("status") {
            src = when {
                record.comboStatus != ComboStatus.None -> "icon_${record.comboStatus.resourceId}.png"
                record.chainStatus != ChainStatus.None -> "icon_${record.chainStatus.resourceId}.png"
                else -> "icon_${record.clear.ifBlank { "clear" }}.png"
            }
        }
        image("rank") {
            src = "rank_${record.rate}.png"
        }
    }
}