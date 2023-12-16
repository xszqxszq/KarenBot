package xyz.xszq.bot.text

object WikiQuery {
//    val client = HttpClient()
//    val json = Json {
//        ignoreUnknownKeys = true
//    }
//    suspend fun query(keyword: String): String? {
//        if (keyword.trim() == "")
//            return null
//        val searchUrl = "https://otomad.wiki/api.php?action=query&list=search&srwhat=title" +
//                "&srnamespace=0&format=json&srsearch=" + URL.encodeComponent(keyword)
//        val parseUrl = "https://otomad.wiki/api.php?action=parse&format=json&page=" + URL.encodeComponent(keyword)
//        val responseExist = client.get(parseUrl).bodyAsText()
//        if ("\"code\":\"missingtitle\"" in responseExist || "\"totalhits\":0" in responseExist) {
//            val responseSearch = client.get(searchUrl)
//            if (responseSearch.status == HttpStatusCode.OK) {
//                val result = json.decodeFromString<MWSearchResult>(responseSearch.bodyAsText())
//                if (result.query.searchinfo.totalhits > 10) {
//                    quoteReply(
//                        "https://otomad.wiki/index.php?title=Special:%E6%90%9C%E7%B4%A2" +
//                                "&profile=advanced&fulltext=1&search=" + URL.encodeComponent(keyword)
//                    )
//                } else if (result.query.searchinfo.totalhits > 0) {
//                    quoteReply("https://otomad.wiki/" + URL.encodeComponent(
//                        result.query.search[0]["title"].toString().replace(" ", "_")
//                    ))
//                }
//            }
//        } else {
//            quoteReply("https://otomad.wiki/" + URL.encodeComponent(keyword.replace(" ", "_")))
//        }
//    }
}