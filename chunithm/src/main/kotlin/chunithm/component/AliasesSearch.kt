package xyz.xszq.bot.chunithm.component

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.TokenStream
import org.apache.lucene.analysis.Tokenizer
import org.apache.lucene.analysis.core.LowerCaseFilter
import org.apache.lucene.analysis.ngram.NGramTokenFilter
import org.apache.lucene.analysis.standard.StandardTokenizer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.search.*
import org.apache.lucene.store.FSDirectory
import xyz.xszq.bot.Chunithm
import xyz.xszq.bot.chunithm.database.ChunithmMusicAliasesTable
import xyz.xszq.bot.chunithm.music.MusicInfo
import xyz.xszq.bot.chunithm.music.MusicNameAlias
import java.nio.file.Path
import java.security.MessageDigest

class AliasesSearch(
    val chunithm: Chunithm
) {
    private val index = "chunithm_music_name"
    private val indexPath: Path = Path.of("${chunithm.dataPath}/../database/lucene/$index")
    private val directory = FSDirectory.open(indexPath)
    private val analyzer: Analyzer = object : Analyzer() {
        override fun createComponents(fieldName: String): Analyzer.TokenStreamComponents {
            val tokenizer: Tokenizer = StandardTokenizer()
            var stream: TokenStream = LowerCaseFilter(tokenizer)
            stream = NGramTokenFilter(stream, 1, 2, false)
            return Analyzer.TokenStreamComponents(tokenizer, stream)
        }
    }
    private val writer: IndexWriter = IndexWriter(
        directory,
        IndexWriterConfig(analyzer).apply {
            openMode = IndexWriterConfig.OpenMode.CREATE_OR_APPEND
        }
    )
    private val refreshMutex = Mutex()
    private var indexedMusicSignature: String? = loadIndexedMusicSignature()

    suspend fun init() {
        refreshIndex()
    }

    fun close() {
        writer.commit()
        writer.close()
        directory.close()
    }

    private fun uid(
        musicId: Int,
        alias: String
    ): String = "$musicId|$alias"

    fun insertDocument(
        data: MusicNameAlias
    ) {
        val doc = Document().apply {
            add(StringField("uid", uid(data.musicId, data.alias), Field.Store.NO))
            add(StringField("musicId", data.musicId.toString(), Field.Store.YES))
            add(TextField("alias", data.alias, Field.Store.YES))
        }
        writer.updateDocument(Term("uid", uid(data.musicId, data.alias)), doc)
    }

    fun insert(
        id: Int,
        alias: String
    ) {
        val data = MusicNameAlias(
            musicId = id,
            alias = alias
        )
        insertDocument(data)
    }
    fun delete(id: Int, alias: String) {
        writer.deleteDocuments(Term("uid", uid(id, alias)))
        writer.commit()
    }

    private suspend fun refreshIndex(
        force: Boolean = false
    ) {
        val currentSnapshot = chunithm.musics().associate { it.id to it.name }
        val currentSignature = snapshotSignature(currentSnapshot)
        if (!force && currentSignature == indexedMusicSignature)
            return

        refreshMutex.withLock {
            val latestSnapshot = chunithm.musics().associate { it.id to it.name }
            val latestSignature = snapshotSignature(latestSnapshot)
            if (!force && latestSignature == indexedMusicSignature)
                return

            val aliases = ChunithmMusicAliasesTable.all().map { (id, alias) ->
                MusicNameAlias(id, alias)
            }
            val currentNames = chunithm.musics().map { music ->
                MusicNameAlias(music.id, music.name)
            }
            val toIndex = (currentNames + aliases)
                .filter { it.alias.isNotBlank() }
                .distinctBy { it.musicId to it.alias.trim().lowercase() }

            writer.deleteAll()
            toIndex.forEach { entry ->
                insertDocument(entry)
            }
            writer.liveCommitData = mapOf(MUSIC_SNAPSHOT_SIGNATURE to latestSignature).entries
            writer.commit()
            indexedMusicSignature = latestSignature
        }
    }

    private fun loadIndexedMusicSignature(): String? {
        if (!DirectoryReader.indexExists(directory))
            return null
        return runCatching {
            DirectoryReader.open(directory).use { reader ->
                reader.indexCommit.userData[MUSIC_SNAPSHOT_SIGNATURE]
            }
        }.getOrNull()
    }

    private fun snapshotSignature(
        snapshot: Map<Int, String>
    ): String {
        val payload = snapshot.entries
            .sortedBy { it.key }
            .joinToString("\n") { (id, name) -> "$id\t$name" }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(payload.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val MUSIC_SNAPSHOT_SIGNATURE = "musicSnapshotSignature"
    }

    private fun analyzeTerms(
        text: String
    ): List<String> {
        val out = mutableListOf<String>()
        analyzer.tokenStream("alias", text).use { ts ->
            val termAttr = ts.addAttribute(CharTermAttribute::class.java)
            ts.reset()
            while (ts.incrementToken())
                out += termAttr.toString()
            ts.end()
        }
        return out
    }

    fun fuzzy(
        query: String
    ): List<MusicNameAlias> {
        val terms = analyzeTerms(query)
            .asSequence()
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .distinct()
            .toList()
        if (terms.isEmpty())
            return emptyList()

        val bool = BooleanQuery.Builder().apply {
            setMinimumNumberShouldMatch(maxOf(1, (terms.size * 0.4).toInt()))
        }
        terms.forEach { value ->
            bool.add(TermQuery(Term("alias", value)), BooleanClause.Occur.SHOULD)
        }
        val luceneQuery = bool.build()

        DirectoryReader.open(writer).use { reader ->
            val searcher = IndexSearcher(reader)
            val hits = searcher.search(luceneQuery, 100).scoreDocs
            if (hits.isEmpty())
                return emptyList()

            val maxScore = hits.maxOf { it.score.toDouble() }
            val threshold = maxScore * 0.45

            return hits.asSequence()
                .filter { it.score.toDouble() >= threshold }
                .mapNotNull { scoreDoc ->
                    val doc = searcher.storedFields().document(scoreDoc.doc)
                    val musicId = doc.get("musicId")?.toIntOrNull()
                    val alias = doc.get("alias")
                    if (musicId == null || alias == null)
                        null
                    else
                        MusicNameAlias(musicId, alias)
                }
                .filter { fuzzyMatch(query, it.alias) }
                .distinctBy { it.musicId }
                .toList()
        }
    }
    private fun fuzzyMatch(query: String, alias: String): Boolean {
        if (similarity(query, alias) >= 0.5) return true
        val x = query.lowercase()
        val y = alias.lowercase()
        if (x.length < 3 || y.length < 3) return false
        val window = y.length
        for (i in 0..x.length - window) {
            if (similarity(x.substring(i, i + window), y) >= 0.75) return true
        }
        return false
    }
    private fun similarity(a: String, b: String): Double {
        val x = a.lowercase()
        val y = b.lowercase()
        if (x == y) return 1.0
        if (x.isEmpty() || y.isEmpty()) return 0.0
        val n = x.length
        val m = y.length
        val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in 0..n) dp[i][0] = i
        for (j in 0..m) dp[0][j] = j
        for (i in 1..n) {
            for (j in 1..m) {
                val cost = if (x[i - 1] == y[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost,
                )
                if (i > 1 && j > 1 && x[i - 1] == y[j - 2] && x[i - 2] == y[j - 1]) {
                    dp[i][j] = minOf(dp[i][j], dp[i - 2][j - 2] + 1)
                }
            }
        }
        return 1.0 - dp[n][m].toDouble() / maxOf(n, m)
    }

    suspend fun search(
        name: String
    ): List<MusicInfo> {
        refreshIndex()

        var result: List<MusicInfo> = listOf()
        if (name.startsWith("id") && name.substringAfter("id").trim().toIntOrNull() != null) {
            val id = name.substringAfter("id").trim().toInt()
            result = listOfNotNull(chunithm.music(id))
        }
        if (name.trim().toIntOrNull() != null) {
            val id = name.toInt()
            result = listOfNotNull(chunithm.music(id))
        }
        if (result.isNotEmpty())
            return result

        val nameMatch = chunithm.musics().filter {
            it.name.equals(name, ignoreCase = true)
        }
        if (nameMatch.isNotEmpty())
            return nameMatch

        val exact = ChunithmMusicAliasesTable.exact(name)
        if (exact.isNotEmpty())
            return exact.mapNotNull { chunithm.music(it) }

        val fuzzy = fuzzy(name.lowercase()).mapNotNull { chunithm.music(it.musicId) }
        if (fuzzy.isNotEmpty())
            return fuzzy

        return chunithm.musics().filter { name.lowercase() in it.name.lowercase() }
    }
}