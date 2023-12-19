package xyz.xszq.bot.text

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import korlibs.io.net.URL
import kotlinx.serialization.json.Json
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

    private suspend fun searchRequest(site: String, keyword: String) = json.decodeFromString<MWSearchResult>(
        client.get {
            url {
                protocol = URLProtocol.HTTPS
                host = site
                path("api.php")
                parameters.append("action", "query")
                parameters.append("list", "search")
                parameters.append("srnamespace", "0")
                parameters.append("format", "json")
                parameters.append("srsearch", keyword)
            }
        }.bodyAsText()
    )

    suspend fun query(keyword: String): ListArk? {
        if (keyword.isBlank())
            return null
        var result: List<MWSearchResultItem>? = null
        var resultSite = ""
        sites.run {
            forEach { (siteName, host) ->
                kotlin.runCatching {
                    searchRequest(host, keyword)
                }.getOrNull()?.query?.search?.let {
                    if (it.isNotEmpty()) {
                        result = it.take(10)
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
                text { "查询结果：" }

                result!!.forEach {
                    link(
                        "https://otmdb.cn/jump/$resultSite/" +
                                URL.encodeComponent(it.title.replace(" ", "_"))
                    ) {
                        it.title
                    }
                }
            }
        }
    }
}