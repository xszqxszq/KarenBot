package xyz.xszq.bot.chunithm.component

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import korlibs.io.file.std.localCurrentDirVfs
import xyz.xszq.bot.chunithm.api.LXNS
import xyz.xszq.bot.chunithm.config.DesignerConfig
import xyz.xszq.bot.chunithm.database.ChunithmMusicAliasesTable
import xyz.xszq.bot.chunithm.music.GameVersion
import xyz.xszq.bot.chunithm.music.MusicInfo
import xyz.xszq.bot.chunithm.payload.LXNSTrophyInfo
import xyz.xszq.bot.json

class ChunithmData {
    val versions = mutableMapOf<String, GameVersion>()
    val musics = mutableMapOf<Int, MusicInfo>()
    val trophies = mutableMapOf<Int, LXNSTrophyInfo>()
    lateinit var newestVersion: GameVersion
    lateinit var designer: DesignerConfig

    @OptIn(ExperimentalHoplite::class)
    suspend fun load(api: LXNS) {
        versions.clear()
        musics.clear()

        designer = ConfigLoaderBuilder.default()
            .addFileSource("./data/chunithm/designer.yml")
            .withExplicitSealedTypes()
            .build()
            .loadConfigOrThrow<DesignerConfig>()

        val songsRaw = loadFromCacheOrFetch(
            fetch = { api.fetchSongs() },
            path = "./data/chunithm/lxns-songs.json"
        )
        musics.putAll(if (songsRaw != null) api.getMusicList(cached = songsRaw) else api.getMusicList())

        versions.putAll(musics.values.map { it.version }
            .distinctBy { it.name }
            .associateBy { it.name })
        newestVersion = versions.values.maxByOrNull { it.version } ?: GameVersion(0, "", 0)
        val aliases = api.getAliases().flatMap { (id, aliases) ->
            if (musics.containsKey(id)) aliases.map { alias -> id to alias }
            else emptyList()
        }
        ChunithmMusicAliasesTable.addAll(aliases)

        val trophiesRaw = loadFromCacheOrFetch(
            fetch = { api.fetchTrophies() },
            path = "./data/chunithm/lxns-trophies.json"
        )
        trophies.putAll(if (trophiesRaw != null) api.getTrophyList(cached = trophiesRaw) else api.getTrophyList())
    }

    private suspend inline fun <reified T> loadFromCacheOrFetch(
        fetch: suspend () -> T,
        path: String
    ): T? {
        val file = localCurrentDirVfs[path]
        return runCatching { fetch() }.onSuccess { data ->
            file.writeString(json.encodeToString(data))
        }.getOrNull() ?: file.takeIf { it.exists() }?.let {
            json.decodeFromString(it.readString())
        }
    }
}