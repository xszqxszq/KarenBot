package xyz.xszq.bot.chunithm.component.image.templates

import org.jetbrains.skia.Image
import xyz.xszq.bot.chunithm.component.ChunithmImage.Companion.color
import xyz.xszq.bot.chunithm.component.image.FilterParams
import xyz.xszq.bot.chunithm.component.image.LevelRenderParams
import xyz.xszq.bot.chunithm.music.ChartInfo
import xyz.xszq.bot.chunithm.music.Level
import xyz.xszq.bot.chunithm.music.Record
import xyz.xszq.shinobu.template.Template
import xyz.xszq.shinobu.template.TemplateManager

class LevelTemplate(
    private val manager: TemplateManager,
    private val resourcePath: String
) {
    /**
     * 生成等级定数表
     * @param charts 谱面信息
     * @param title 完成表标题
     * @param filterParams 条件过滤参数
     */
    suspend fun level(
        charts: List<ChartInfo>,
        records: List<Record> ?= null,
        title: String,
        filterParams: FilterParams,
    ): Image {
        val groups = if (filterParams.isDetailed) {
            charts.groupBy {
                it.levelValue.toString()
            }
        } else {
            charts.groupBy {
                it.level
            }
        }.toSortedMap(Level.comparator).toList().reversed()

        return template(
            LevelRenderParams(
                title = title,
                filter = filterParams,
                groups = groups,
                matched = emptyMap(),
                completed = emptyMap(),
                showProgress = false,
                progressData = emptyMap(),
            )
        )
    }

    /**
     * 生成模板
     * @param params 渲染参数
     */
    private fun template(
        params: LevelRenderParams
    ): Image {
        val template = manager["level"]!!
        val main = template["main"]!!.modify {
            div("upper/header") {
                text("title") {
                    text = params.title
                }
            }

            div("upper/list") {
                params.groups.forEach { (level, charts) -> add(template["group"]!!.modify {
                    text("level") {
                        text = level
                    }
                    div("musics") {
                        charts.forEach { chart ->
                            add(template.levelChart(
                                chart = chart
                            ))
                        }
                    }
                })}
            }
        }
        return template.render(main)
    }

    /**
     * 展示单个谱面信息
     * @param chart 谱面信息
     */
    fun Template.levelChart(
        chart: ChartInfo,
    ) = this["music"]!!.modify {
        style.backgroundColor = chart.color()
        div("cover") {
            val id = chart.music.id.toString()
            background = "$resourcePath/covers/${chart.music.resourceId}_s.jpg"
            div("overlay") {
                div("info") {
                    div("id-container") {
                        style.backgroundColor = chart.color()
                        style.width = (25 + (id.length - 3) * 5).toFloat()
                        text("id") {
                            text = id
                        }
                    }
                }
            }

        }
    }
}