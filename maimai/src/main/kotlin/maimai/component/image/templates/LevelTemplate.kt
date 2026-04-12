package xyz.xszq.bot.maimai.component.image.templates

import korlibs.io.util.toStringDecimal
import org.jetbrains.skia.Image
import xyz.xszq.bot.maimai.component.image.FilterParams
import xyz.xszq.bot.maimai.component.image.LevelRenderParams
import xyz.xszq.bot.maimai.component.image.MaimaiImage.Companion.color
import xyz.xszq.bot.image.parse.StyleParser
import xyz.xszq.bot.image.template.Template
import xyz.xszq.bot.image.template.TemplateManager
import xyz.xszq.bot.maimai.music.ChartInfo
import xyz.xszq.bot.maimai.music.Level
import xyz.xszq.bot.maimai.music.Record
import xyz.xszq.bot.maimai.music.RequiresType

class LevelTemplate(
    private val manager: TemplateManager,
    private val resourcePath: String
) {
    /**
     * 生成等级完成表
     * @param charts 谱面信息
     * @param records 所有成绩信息
     * @param title 完成表标题
     * @param filterParams 条件过滤参数
     */
    suspend fun level(
        charts: List<ChartInfo>,
        records: List<Record>? = null,
        title: String,
        filterParams: FilterParams,
    ): Image {
        val groups = if (filterParams.isDetailed) {
            charts.groupBy {
                if (filterParams.isFitLevelValue)
                    it.fitLevelValue.toStringDecimal(1)
                else
                    it.levelValue.toString()
            }
        } else {
            charts.groupBy {
                if (filterParams.isFitLevelValue)
                    Level.toLevel(it.fitLevelValue)
                else
                    it.level
            }
        }.toSortedMap(Level.comparator).toList().reversed()

        val matched = charts.associateWith { chart ->
            records ?.firstOrNull {
                it.music.id == chart.music.id && it.chart.difficulty == chart.difficulty
            }
        }
        val completed = charts.associateWith { chart ->
            records ?.firstOrNull {
                it.music.id == chart.music.id && it.chart.difficulty == chart.difficulty
            }
        }
        return template(LevelRenderParams(
            title = title,
            filter = filterParams,
            groups = groups,
            matched = matched,
            completed = completed,
        ))
    }

    /**
     * 生成模板
     * @param params 渲染参数
     */
    private suspend fun template(
        params: LevelRenderParams
    ): Image {
        val template = manager["level"]!!
        val main = template["main"]!!.modify {
            div("upper/header") {
                text("title") {
                    text = params.title
                }
                image("all") {
                    if (params.completed.values.any { it == null })
                        return@image
                    src = when (params.filter.requiresType) {
                        RequiresType.Achievement -> {
                            val min = params.completed.values
                                .filterNotNull()
                                .minBy { it.achievement }
                            when {
                                min.achievement >= 970000 -> min.rate
                                min.achievement >= 800000 -> "clear"
                                else -> null
                            }
                        }
                        RequiresType.Combo -> {
                            val min = params.completed.values
                                .filterNotNull()
                                .filter { it.comboStatus.isFC() }
                                .minByOrNull { it.comboStatus }
                            min?.comboStatus?.value
                        }
                        RequiresType.Sync -> {
                            val min = params.completed.values
                                .filterNotNull()
                                .filter { it.syncStatus.isFS() }
                                .minByOrNull { it.syncStatus }
                            min?.syncStatus?.value
                        }
                    } ?.let {
                        "all_$it.png"
                    }
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
                                chart = chart,
                                requiresType = params.filter.requiresType,
                                record = params.matched[chart]
                            ))
                        }
                    }
                })}
            }
        }
        return template.render(main)
    }

    /**
     * 展示单个谱面成绩
     * @param chart 谱面信息
     * @param requiresType 达成要求类型
     * @param record 游玩成绩
     */
    fun Template.levelChart(
        chart: ChartInfo,
        requiresType: RequiresType = RequiresType.Achievement,
        record: Record? = null,
    ) = this["music"]!!.modify {
        style.backgroundColor = chart.color()
        div("cover") {
            val id = chart.music.id.toString()
            background = "$resourcePath/covers/${chart.music.resourceId}_s.jpg"
            div("overlay") {
                div("info") {
                    image("type") {
                        src = "type_${chart.music.type.value}.png"
                    }
                    div("id-container") {
                        style.backgroundColor = chart.color()
                        style.width = (25 + (id.length - 3) * 5).toFloat()
                        text("id") {
                            text = id
                        }
                    }
                }

                record ?.let {
                    when (requiresType) {
                        RequiresType.Achievement -> {
                            if (record.achievement >= 800000) {
                                "rank_${record.rate}.png"
                            } else {
                                null
                            }
                        }
                        RequiresType.Combo -> {
                            if (record.comboStatus.isFC()) {
                                "icon_${record.comboStatus.value}.png"
                            } else {
                                null
                            }
                        }
                        RequiresType.Sync -> {
                            if (record.syncStatus.isFS()) {
                                "icon_${record.syncStatus.value}.png"
                            } else {
                                null
                            }
                        }
                    } ?.let { icon ->
                        style.backgroundColor = StyleParser.rgba(0, 0, 0, 0.5f)
                        image("score") {
                            src = icon
                        }
                    }
                }
            }

        }
    }

}