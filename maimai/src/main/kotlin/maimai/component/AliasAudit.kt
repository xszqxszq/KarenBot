package xyz.xszq.bot.maimai.component

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import xyz.xszq.bot.maimai.Maimai
import xyz.xszq.bot.maimai.music.MusicInfo

class AliasAudit(
    private val maimai: Maimai
) {
    @Serializable
    data class Result(
        val type: String
    )

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun audit(
        music: MusicInfo,
        alias: String
    ): Result {
        val llmClient = maimai.pluginLoader.llmClient ?: return Result("ok")
        val systemPrompt = buildString {
            appendLine("你是一个严格且中立的互联网内容安全审核专家。你的任务是分析用户为歌曲添加的别名，判断其是否合规。")
            appendLine("你的输出必须是 JSON 格式：{\"type\": \"<type>\"}")
            if (music.name == "Panopticon") {
                appendLine("type 只能是以下之一：ok、political、name、school")
                append(basePrompt())
                appendLine()
                appendLine("3. 学校名称审核（type: school）")
                appendLine("- 检查别名是否指代具体学校名称，包括但不限于：")
                appendLine("  * 中学/大学名（如\"XX中学\"、\"XX大学\"）")
                appendLine("  * 指代特定区域学校的名称（如\"XX市高中\"、\"XX区初中\"等）")
                appendLine("- 以下情况不算学校名：")
                appendLine("  * 单独的泛指词汇（\"大学\"、\"教室\"、\"学校\"等，只有这个词汇单独出现，前后无任何内容）")
                appendLine("- 若检测到具体的学校名称，返回 type=school")
                appendLine()
                appendLine("4. 正常内容（type: ok）")
            }
            else {
                appendLine("type 只能是以下之一：ok、political、name")
                append(basePrompt())
                appendLine("3. 正常内容（type: ok）")
            }
            appendLine("   - 未命中上述任何规则，返回 type=ok")
        }

        val content = llmClient.chat(scene = "audit") {
            system(systemPrompt)
            user(buildUserPrompt(music, alias))
            responseFormat("json_object")
        }

        return runCatching {
            json.decodeFromString<Result>(content)
        }.getOrNull() ?: Result("ok")
    }

    private fun basePrompt(): String = buildString {
        appendLine("审核规则如下：")
        appendLine()
        appendLine("1. 政治敏感（type: political）")
        appendLine("   - 反党反国家内容，包括对中国/中国人的蔑称")
        appendLine("   - 涉及中国领导人名称的内容")
        appendLine("   - 其他明显涉及政治敏感的内容")
        appendLine("   若检测到，返回 type=political")
        appendLine()
        appendLine("2. 非名人真实中文人名（type: name）")
        appendLine("   - 看起来十分确定是真实中国普通人姓名的")
        appendLine("   - 包含在长字符串中也算")
        appendLine("   - 名人/网红/历史人物的姓名不算，需要检索确认")
        appendLine("   - 二次元/游戏/虚拟角色名称不算，需要检索确认")
        appendLine("   - 明显且十分确定的歌曲名称的中文谐音不算，例如got more raves对应郭沫若")
        appendLine("   若检测到，返回 type=name")
        appendLine()
    }

    private fun buildUserPrompt(
        music: MusicInfo,
        alias: String
    ): String = buildString {
        appendLine("歌曲标题：${music.name}")
        appendLine("添加的别名：$alias")
    }
}
