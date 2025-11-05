package xyz.xszq.bot.component

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.FuzzyQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.store.FSDirectory
import xyz.xszq.bot.Maimai
import xyz.xszq.bot.database.MusicAliasesTable
import xyz.xszq.bot.music.MusicInfo
import xyz.xszq.bot.music.MusicNameAlias
import java.nio.file.Path

class AliasesSearch(
    val maimai: Maimai
) {
    private val index = "music_name"
    private val indexPath: Path = Path.of("database/lucene/$index")
    private val directory = FSDirectory.open(indexPath)
    private val analyzer: Analyzer = SmartChineseAnalyzer()
    private val writer: IndexWriter = IndexWriter(
        directory,
        IndexWriterConfig(analyzer).apply {
            openMode = IndexWriterConfig.OpenMode.CREATE_OR_APPEND
        }
    )

    suspend fun init() {
        loadAliases()
    }
    fun close() {
        writer.commit()
        writer.close()
        directory.close()
    }

    private fun uid(musicId: Int, alias: String): String = "$musicId|$alias"

    fun insertDocument(data: MusicNameAlias) {
        val doc = Document().apply {
            add(StringField("uid", uid(data.musicId, data.alias), Field.Store.NO))
            add(StringField("musicId", data.musicId.toString(), Field.Store.YES))
            add(TextField("alias", data.alias, Field.Store.YES))
        }

        writer.updateDocument(Term("uid", uid(data.musicId, data.alias)), doc)
    }
    fun insert(id: Int, alias: String) {
        val data = MusicNameAlias(
            musicId = id,
            alias = alias
        )
        insertDocument(data)
    }
    private fun batchInsert(data: List<MusicNameAlias>) {
        data.forEach { entry ->
            insertDocument(entry)
        }
        writer.commit()
    }
    suspend fun loadAliases() {
        maimai.musics().forEach { music ->
            MusicAliasesTable.add(music, music.name)
        }
        val toInsert = MusicAliasesTable.all().map { (id, alias) ->
            MusicNameAlias(id, alias)
        }
        batchInsert(toInsert)
    }
    private fun analyzeTerms(text: String): List<String> {
        val out = mutableListOf<String>()
        analyzer.tokenStream("alias", text).use { ts ->
            val termAttr = ts.addAttribute(CharTermAttribute::class.java)
            ts.reset()
            while (ts.incrementToken()) out += termAttr.toString()
            ts.end()
        }
        return out
    }

    fun fuzzy(query: String): List<MusicNameAlias> {
        val terms = analyzeTerms(query)
        if (terms.isEmpty()) return emptyList()

        val bool = BooleanQuery.Builder()
        terms.forEach { t ->
            bool.add(FuzzyQuery(Term("alias", t), 2), BooleanClause.Occur.SHOULD)
        }
        val luceneQuery = bool.build()

        DirectoryReader.open(writer).use { reader ->
            val searcher = IndexSearcher(reader)
            val topN = 100
            val hits = searcher.search(luceneQuery, topN).scoreDocs
            if (hits.isEmpty()) return emptyList()

            val maxScore = hits.maxOf { it.score.toDouble() }
            val threshold = maxScore * 0.45

            return hits
                .asSequence()
                .filter { it.score.toDouble() >= threshold }
                .mapNotNull { sd ->
                    val d = searcher.storedFields().document(sd.doc)
                    val musicId = d.get("musicId")?.toIntOrNull()
                    val aliasStr = d.get("alias")
                    if (musicId == null || aliasStr == null) null
                    else MusicNameAlias(musicId, aliasStr)
                }
                .distinctBy { it.musicId }
                .toList()
        }
    }
    suspend fun search(name: String): List<MusicInfo> {
        var result: List<MusicInfo> = listOf()
        if (name.startsWith("id") && name.substringAfter("id").trim().toIntOrNull() != null) {
            val id = name.substringAfter("id").trim().toInt()
            result = listOfNotNull(maimai.music(id))
        }
        if (name.trim().toIntOrNull() != null) {
            val id = name.toInt()
            result = listOfNotNull(maimai.music(id))
        }
        if (result.isNotEmpty())
            return result

        val nameMatch = maimai.musics().filter { it.name.lowercase() == name.lowercase() }
        if (nameMatch.isNotEmpty()) {
            return nameMatch
        }
        val exact = MusicAliasesTable.exact(name)
        if (exact.isNotEmpty()) {
            return exact.mapNotNull { maimai.music(it) }
        }
        val fuzzy = fuzzy(name.lowercase()).mapNotNull { maimai.music(it.musicId) }
        if (fuzzy.isNotEmpty()) {
            return fuzzy
        }
        return maimai.musics().filter { name.lowercase() in it.name.lowercase() }
    }
}