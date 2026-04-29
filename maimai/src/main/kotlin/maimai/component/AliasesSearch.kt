package xyz.xszq.bot.maimai.component

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
import org.apache.lucene.search.*
import org.apache.lucene.store.FSDirectory
import xyz.xszq.bot.Maimai
import xyz.xszq.bot.maimai.database.MusicAliasesTable
import xyz.xszq.bot.maimai.music.MusicInfo
import xyz.xszq.bot.maimai.music.MusicNameAlias
import java.nio.file.Path
import java.security.MessageDigest

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
    private val refreshMutex = Mutex()
    private var indexedMusicSnapshot: Map<Int, String> = emptyMap()
    private var indexedMusicSignature: String? = loadIndexedMusicSignature()

    suspend fun init() {
        refreshIndex()
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
    private suspend fun refreshIndex(force: Boolean = false) {
        val currentSnapshot = maimai.musics().associate { it.id to it.name }
        val currentSignature = snapshotSignature(currentSnapshot)
        if (!force && currentSignature == indexedMusicSignature) {
            return
        }

        refreshMutex.withLock {
            val latestSnapshot = maimai.musics().associate { it.id to it.name }
            val latestSignature = snapshotSignature(latestSnapshot)
            if (!force && latestSignature == indexedMusicSignature) {
                return
            }

            val aliases = MusicAliasesTable.all().map { (id, alias) ->
                MusicNameAlias(id, alias)
            }
            val currentNames = maimai.musics().map { music ->
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
            indexedMusicSnapshot = latestSnapshot
            indexedMusicSignature = latestSignature
        }
    }

    private fun loadIndexedMusicSignature(): String? {
        if (!DirectoryReader.indexExists(directory)) {
            return null
        }

        return runCatching {
            DirectoryReader.open(directory).use { reader ->
                reader.indexCommit.userData[MUSIC_SNAPSHOT_SIGNATURE]
            }
        }.getOrNull()
    }

    private fun snapshotSignature(snapshot: Map<Int, String>): String {
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
            .asSequence()
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sortedByDescending { it.length }
            .take(6)
            .toList()
        if (terms.isEmpty()) return emptyList()

        val bool = BooleanQuery.Builder().apply {
            setMinimumNumberShouldMatch(
                when {
                    terms.size >= 4 -> 2
                    else -> 1
                }
            )
        }
        terms.forEach { t ->
            val term = Term("alias", t)
            bool.add(TermQuery(term), BooleanClause.Occur.SHOULD)

            if (t.length >= 2) {
                val fuzzyQuery = when {
                    t.length <= 2 -> FuzzyQuery(term, 1, 0, 10, true)
                    t.length <= 4 -> FuzzyQuery(term, 1, 0, 20, true)
                    else -> FuzzyQuery(term, 1, 1, 50, true)
                }
                bool.add(fuzzyQuery, BooleanClause.Occur.SHOULD)
            }
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
        refreshIndex()

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

        val nameMatch = maimai.musics().filter {
            it.name.equals(name, ignoreCase = true)
        }
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
