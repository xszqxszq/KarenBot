@file:Suppress("unused")
package tk.xszq.otomadbot.api

import com.google.gson.Gson
import com.soywiz.korio.net.URL
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import net.mamoe.mirai.event.events.MessageEvent
import org.jsoup.Jsoup
import tk.xszq.otomadbot.*

object MediaWikiUtils: CommandUtils("wiki") {
    val client = HttpClient(CIO)
    @CommandFinding("otomadwiki", "search", false)
    suspend fun doHandleOtomadWikiSearch(args: Args, event: MessageEvent) = event.run {
        args.firstOrNull()?.let { keyword ->
            if (keyword.trim() == "") return@let
            val searchUrl = "https://otomad.wiki/api.php?action=query&list=search&srwhat=title" +
                    "&srnamespace=0&format=json&srsearch=" + URL.encodeComponent(keyword)
            val parseUrl = "https://otomad.wiki/api.php?action=parse&format=json&page=" + URL.encodeComponent(keyword)
            val responseExist = client.get<HttpResponse>(parseUrl).readText()
            if ("\"code\":\"missingtitle\"" in responseExist || "\"totalhits\":0" in responseExist) {
                val responseSearch = client.get<HttpResponse>(searchUrl)
                if (responseSearch.isSuccessful()) {
                    val result = Gson().fromJson(responseSearch.readText(), MWSearchResult::class.java)
                    if (result.query.searchinfo.totalhits > 10) {
                        quoteReply("https://otomad.wiki/index.php?title=Special:%E6%90%9C%E7%B4%A2" +
                                "&profile=advanced&fulltext=1&search=" + URL.encodeComponent(keyword)
                        )
                    } else if (result.query.searchinfo.totalhits > 0) {
                        quoteReply("https://otomad.wiki/" +
                                URL.encodeComponent(result.query.search[0]["title"].toString()
                                    .replace(" ", "_"))
                        )
                    }
                }
            } else {
                quoteReply("https://otomad.wiki/" + URL.encodeComponent(keyword.replace(" ", "_")))
            }
        }
    }
}

data class MWSearchResultInfo(val totalhits: Int)
data class MWSearchResultQuery(val searchinfo: MWSearchResultInfo, val search: List<HashMap<String, Any>>)
data class MWSearchResult(val query: MWSearchResultQuery)