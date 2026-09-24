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
import xyz.xszq.bot.chunithm.payload.LXNSCharacterInfo
import xyz.xszq.bot.chunithm.payload.LXNSPlateInfo
import xyz.xszq.bot.chunithm.payload.LXNSTrophyInfo
import xyz.xszq.bot.util.json
import java.io.File

/**
 * 中二游戏数据
 */
class ChunithmData(
    val dataPath: String = "./data/chunithm"
) {
    val versions = mutableMapOf<String, GameVersion>()
    val musics = mutableMapOf<Int, MusicInfo>()
    val trophies = mutableMapOf<Int, LXNSTrophyInfo>()
    val characters = mutableMapOf<Int, LXNSCharacterInfo>()
    val plates = mutableMapOf<Int, LXNSPlateInfo>()
    val avatarIds = mutableSetOf<Int>()
    val plateIds = mutableSetOf<Int>()
    lateinit var newestVersion: GameVersion
    lateinit var designer: DesignerConfig

    /**
     * 初始化载入
     *
     * @param api 落雪查分器
     */
    @OptIn(ExperimentalHoplite::class)
    suspend fun load(api: LXNS) {
        versions.clear()
        musics.clear()

        designer = ConfigLoaderBuilder.default()
            .addFileSource("$dataPath/designer.yml")
            .withExplicitSealedTypes()
            .build()
            .loadConfigOrThrow<DesignerConfig>()

        // 拉取歌曲列表并组装歌曲，网络请求失败时改读缓存
        val songsRaw = fetchWithCacheFallback(
            fetch = { api.fetchSongs() },
            path = "$dataPath/lxns-songs.json"
        )
        musics.putAll(api.getMusicList(cached = songsRaw))

        versions.putAll(musics.values.map { it.version }
            .distinctBy { it.name }
            .associateBy { it.name })
        newestVersion = versions.values.maxByOrNull { it.version } ?: GameVersion(0, "", 0)
        val aliases = api.getAliases().flatMap { (id, aliases) ->
            if (musics.containsKey(id)) aliases.map { alias -> id to alias }
            else emptyList()
        }
        ChunithmMusicAliasesTable.addAll(aliases)

        // 拉取称号列表并组装可用称号，网络请求失败时改读缓存
        val trophiesRaw = fetchWithCacheFallback(
            fetch = { api.fetchTrophies() },
            path = "$dataPath/lxns-trophies.json"
        )
        trophies.putAll(api.getTrophyList(cached = trophiesRaw))

        val charactersRaw = fetchWithCacheFallback(
            fetch = { api.fetchCharacters() },
            path = "$dataPath/lxns-characters.json"
        )
        characters.putAll(api.getCharacterList(cached = charactersRaw))

        val platesRaw = fetchWithCacheFallback(
            fetch = { api.fetchPlates() },
            path = "$dataPath/lxns-plates.json"
        )
        plates.putAll(api.getPlateList(cached = platesRaw))

        avatarIds.addAll(localIds("$dataPath/avatars"))
        plateIds.addAll(localIds("$dataPath/plates"))
    }

    private fun localIds(path: String): List<Int> = File(path).listFiles()
        .orEmpty()
        .mapNotNull { it.name.substringBefore(".png").toIntOrNull() }

    /**
     * 从落雪拉取数据
     *
     * 网络连接失败时从缓存中读取
     *
     * @param fetch 请求代码块
     * @param path 缓存文件路径
     * @return 结果数据
     */
    private suspend inline fun <reified T> fetchWithCacheFallback(
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