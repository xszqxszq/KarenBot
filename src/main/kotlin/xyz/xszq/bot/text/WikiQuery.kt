package xyz.xszq.bot.text

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import korlibs.io.net.URL
import kotlinx.serialization.json.Json
import xyz.xszq.bot.payload.MWExtractPage
import xyz.xszq.bot.payload.MWExtractResult
import xyz.xszq.bot.payload.MWSearchResult
import xyz.xszq.bot.payload.MWSearchResultItem
import xyz.xszq.nereides.message.ark.ListArk

object WikiQuery {
    val json = Json {
        ignoreUnknownKeys = true
    }
    val client = HttpClient(OkHttp)
    private val sites = buildMap {
        put("otmwiki", "otomad.wiki")
        put("thbwiki", "thwiki.cc")
        put("moegirl", "zh.moegirl.org.cn")
    }

    private suspend fun searchRequest(site: String, keyword: String) = json.decodeFromString<MWExtractResult>(
        client.get {
            url {
                protocol = URLProtocol.HTTPS
                host = site
                path("api.php")
                parameters.append("action", "query")
                parameters.append("format", "json")
                parameters.append("prop", "extracts")
                parameters.append("titles", keyword)
                parameters.append("utf8", "1")
                parameters.append("ascii", "1")
                parameters.append("redirects", "1")
                parameters.append("exsentences", "3")
                parameters.append("explaintext", "1")
                parameters.append("converttitles", "1")
                parameters.append("exsectionformat", "plain")
            }
        }.bodyAsText()
    )

    suspend fun query(keyword: String): ListArk? {
        if (keyword.isBlank())
            return null
        var result: List<MWExtractPage>? = null
        var resultSite = ""
        sites.run {
            forEach { (siteName, host) ->
                kotlin.runCatching {
                    searchRequest(host, keyword)
                }.getOrNull()?.query?.pages?.let {
                    if (it.isNotEmpty() && it["-1"]?.missing != "") {
                        result = it.values.take(10)
                        resultSite = siteName
                        return@run
                    }
                }
            }
        }
        return result?.let {
            ListArk.build {
                desc { "Wiki 查询结果" }
                prompt { "查询结果" }
                val item = it.first()
                text { item.title }
                text { item.extract!! }

                if (it.size > 1) {
                    text { "其他结果：" }
                    it.forEach { page ->
                        link(
                            "https://otmdb.cn/jump/$resultSite/" +
                                    URL.encodeComponent(page.title.replace(" ", "_"))
                        ) {
                            page.title
                        }
                    }
                }
            }
        }
    }
}