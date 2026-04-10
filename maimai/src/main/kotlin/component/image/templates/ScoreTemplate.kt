package xyz.xszq.bot.component.image.templates

import org.jetbrains.skia.Image
import xyz.xszq.bot.component.image.MaimaiImage.Companion.color
import xyz.xszq.bot.image.dom.Div
import xyz.xszq.bot.image.dom.Element
import xyz.xszq.bot.image.template.Template
import xyz.xszq.bot.image.template.TemplateManager
import xyz.xszq.bot.music.*

class ScoreTemplate(
    private val manager: TemplateManager,
    private val resourcePath: String
) {
    /**
     * 生成歌曲及成绩信息模板
     * @param music 曲目信息
     * @param records 各难度成绩记录
     */
    suspend fun template(
        music: MusicInfo,
        records: List<Record>? = null
    ): Image {
        val template = manager["score"]!!
        val main = template["main"]!!.modify {
            div("upper") {
                div("header-cover") header@ {
                    background = "$resourcePath/covers/${music.resourceId}.png"
                    musicInfo(music)
                }
                if (music.genre == MusicGenre.Utage)
                    music.charts.first().let { chart ->
                        add(template.score(chart, records?.firstOrNull()))
                    }
                else
                    music.charts.forEach { chart ->
                        val record = records?.firstOrNull { it.chart.difficulty == chart.difficulty }
                        add(template.score(chart, record))
                    }
                if (music.genre != MusicGenre.Utage && music.charts.none { it.difficulty == MusicDifficulty.ReMaster }) {
                    add(template.score(music.fakeReMaster, null))
                }
            }
        }
        return template.render(main)
    }

    /**
     * 展示曲目信息
     * @param music 曲目信息
     */
    private fun Div.musicInfo(
        music: MusicInfo
    ) {
        image("cover") {
            src = "$resourcePath/covers/${music.resourceId}.png"
        }
        div("right") {
            div("top") {
                image("type") {
                    src = "type_${music.type.value}.png"
                }
                text("id") {
                    text = "ID ${music.id}"
                }
            }
            div("title-container") {
                text("artist") {
                    text = music.artist
                }
                text("title") {
                    text = music.name
                }
            }
            div("bottom") {
                image("genre") {
                    src = "genre_${music.genre.id}.png"
                }
                image("version") {
                    src = "version_${music.version.version}.png"
                }
                text("bpm") {
                    text = "BPM ${music.bpm}"
                }
            }
        }
    }

    /**
     * 展示单难度谱面信息及成绩记录
     * @param chart 谱面信息
     * @param record 成绩记录
     */
    private fun Template.score(
        chart: ChartInfo,
        record: Record?
    ): Element = this["chart"]!!.modify {
        background = "base_${chart.difficulty.name}.png"
        div("level-container") {
            val nowColor = chart.color()
            text("icon") {
                style.textColor = nowColor
            }
            text("level") {
                style.textColor = nowColor
                if (chart.levelValue != 0.0)
                    text = chart.levelValue.toString()
            }
        }
        record ?.let {
            div("fixed-acc") {
                text("achievement") {
                    text = Rate.toString(record.achievement)
                }
            }
            div("fixed-rank") {
                image("rank") {
                    src = "rank_${record.rate}.png"
                }
            }
            image("combo") {
                src = "icon_${record.comboStatus.value}.png"
            }
            image("sync") {
                src = "icon_${record.syncStatus.value}.png"
            }
            div("dxstar-container") {
                text("value") {
                    text = "${record.deluxeScore}/${chart.maxDeluxeScore}"
                }
                image("icon") {
                    val stars = DeluxeScore.stars(record.deluxeScore, record.chart.maxDeluxeScore)
                    src = "icon_dxstar_$stars.png"
                }
            }
        }
        div("note-designer") {
            text("designer") {
                text = chart.notesDesigner
            }
        }
        if (chart.levelValue == 0.0) {
            div("fixed-acc") {
                text("achievement") {
                    text = "无此难度"
                }
            }
        }
    }
}