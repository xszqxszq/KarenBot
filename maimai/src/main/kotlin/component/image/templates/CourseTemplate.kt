package xyz.xszq.bot.component.image.templates

import korlibs.io.util.isDigit
import korlibs.math.toIntFloor
import org.jetbrains.skia.Image
import xyz.xszq.bot.image.dom.Img
import xyz.xszq.bot.image.style.ObjectFit
import xyz.xszq.bot.image.style.Spacing
import xyz.xszq.bot.image.template.Template
import xyz.xszq.bot.image.template.TemplateManager
import xyz.xszq.bot.music.ChartInfo
import xyz.xszq.bot.music.Record
import xyz.xszq.bot.payload.LocalCourseInfo

class CourseTemplate(
    private val manager: TemplateManager,
    private val resourcePath: String
) {
    /**
     * 生成段位表
     * @param course 段位信息
     * @param scores 成绩信息
     */
    suspend fun template(
        course: LocalCourseInfo,
        scores: List<Pair<ChartInfo, Record?>>
    ): Image {
        var life = course.life
        val damages = scores.mapIndexed { index, (chart, score) ->
            if (score ?.comboStatus ?.isAP() == true)
                0
            else
                calcMinDamage(chart, score?.achievement ?: 0, course)
        }
        val remains = damages.mapIndexed { index, damage ->
            if (life > 0 && index != 0)
                life += course.recover
            life -= damage
            if (life <= 0) {
                life = 0
            }
            life
        }
        val theme = manager["course"]!!
        val main = theme["main"]!!.modify {
            val realId = course.id % 10000
            background = when {
                realId <= 1010 -> "background_1.png"
                realId <= 1112 -> "background_2.png"
                else -> "background_3.png"
            }
            div("upper/info") {
                div("status") {
                    if (life <= 0)
                        background = "final_2.png"
                    image("course") {
                        src = "title_${course.id}.png"
                    }
                }
                div("life") {
                    val type = when {
                        life >= 100 -> 1
                        life >= 10 -> 2
                        life >= 1 -> 3
                        else -> 4
                    }
                    background = "life_$type.png"
                    life.toString().forEach { value ->
                        add(Img(src = "life_${type}_${value}.png").apply {
                            style.objectFit = ObjectFit.NONE
                        })
                    }
                }
                div("detail/header/life") {
                    text("value") {
                        text = course.life.toString()
                    }
                }
                div("detail/achievement") {
                    val total = scores.sumOf { it.second ?.achievement ?: 0 }
                    val type = when {
                        total < 800000 * scores.size -> 1
                        total < 970000 * scores.size -> 2
                        else -> 3
                    }

                    val part0 = (total / 10000).toString().padStart(3, ' ')
                    val part1 = (total % 10000).toString().padStart(4, '0')
                    part0.forEachIndexed { index, char ->
                        image("acc-0-$index") {
                            src = if (char == ' ')
                                "score_2_none.png"
                            else
                                "score_2_${type}_${char}.png"
                        }
                    }
                    image("acc-dot") {
                        src = "score_1_${type}_dot.png"
                    }
                    part1.forEachIndexed { index, char ->
                        image("acc-1-$index") {
                            src = "score_1_${type}_${char}.png"
                        }
                    }
                    image("acc-percent") {
                        src = "percent_1_${type}.png"
                    }
                }
                div("detail/damage") {
                    div("great") {
                        text("value") {
                            text = "-${course.damage.great}"
                        }
                    }
                    div("good") {
                        text("value") {
                            text = "-${course.damage.good}"
                        }
                    }
                    div("miss") {
                        text("value") {
                            text = "-${course.damage.miss}"
                        }
                    }
                }
                text("detail/heal-div/heal/value") {
                    text = "+${course.recover}"
                }
            }
            div("upper/musics") {
                scores.forEachIndexed { index, (chart, record) ->
                    add(theme.music(
                        chart, record ?.achievement ?: 0, remains[index], damages[index]
                    ))
                }
            }
        }
        return theme.render(main)
    }
    /**
     * 展示单一曲目及成绩信息
     * @param chart 谱面信息
     * @param achievement 达成率
     * @param life 生命值
     * @param damage 扣血量
     */
    fun Template.music(
        chart: ChartInfo,
        achievement: Int,
        life: Int,
        damage: Int
    ) = this["music"]!!.modify {
        image("result") {
            if (life <= 0)
                src = "result_2.png"
        }
        div("music-div") {
            background = "base_${chart.difficulty.name}.png"
            image("cover") {
                src = "$resourcePath/covers/${chart.music.resourceId}.png"
            }
            div("info") {
                div("header") {
                    image("type") {
                        src = "type_${chart.music.type.value}.png"
                    }
                    div("title-div") {
                        text("title") {
                            text = chart.music.name
                        }
                    }
                }
                div("level-div") {
                    image("level") {
                        src = "level_${chart.difficulty.value}_level.png"
                    }
                    div("value") {
                        val digits = chart.level.filter { it.isDigit() }
                        digits.forEachIndexed { index, value ->
                            add(Img(src = "level_${chart.difficulty.value}_$value.png").apply {
                                style.margin = Spacing(
                                    0f, 0f, 0f,
                                    if (index != 0) -12f else 0f
                                )
                                style.objectFit = ObjectFit.NONE
                            })
                        }
                    }
                    image("plus") {
                        src = "level_${if (chart.level.endsWith("+")) chart.difficulty.value else 5}_plus.png"
                    }
                }
                div("achievement") {
                    val part0 = (achievement / 10000).toString().padStart(3, ' ')
                    val part1 = (achievement % 10000).toString().padStart(4, '0')
                    val type = when {
                        achievement < 800000 -> 1
                        achievement < 970000 -> 2
                        else -> 3
                    }

                    part0.forEachIndexed { index, char ->
                        image("acc-0-$index") {
                            src = if (char == ' ')
                                "score_1_none.png"
                            else
                                "score_1_${type}_${char}.png"
                        }
                    }
                    image("acc-dot") {
                        src = "score_3_${type}_dot.png"
                    }
                    part1.forEachIndexed { index, char ->
                        image("acc-1-$index") {
                            src = "score_3_${type}_${char}.png"
                        }
                    }
                    image("acc-percent") {
                        src = "percent_2_${type}.png"
                    }
                }
                div("damage-div") {
                    text("damage") {
                        text = "-${damage}"
                    }
                }
            }
        }
    }

    /**
     * 计算该达成率最低会扣多少血量
     * @param chart 谱面信息
     * @param achievement 达成率
     * @param course 段位信息
     */
    private fun calcMinDamage(
        chart: ChartInfo,
        achievement: Int,
        course: LocalCourseInfo
    ): Int {
        val totalBase = chart.notes.tap + chart.notes.touch +
                2 * chart.notes.hold + 3 * chart.notes.slide +
                5 * chart.notes.`break`
        val base = 100000.0 / totalBase

        val minus = 1010000 - achievement
        val amount = minus / base

        val greats = (amount / 2).toIntFloor()
        val goods = (amount / 5).toIntFloor()
        val misses = (amount / 10).toIntFloor()

        val damages = mutableListOf<Int>()
        if (course.damage.great != 0)
            damages.add(course.damage.great * greats)
        if (course.damage.good != 0)
            damages.add(course.damage.good * goods)
        if (course.damage.miss != 0)
            damages.add(course.damage.miss * misses)
        return damages.minOrNull() ?: 0
    }
}