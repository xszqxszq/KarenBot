package xyz.xszq.bot.chunithm.component

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import xyz.xszq.bot.chunithm.api.LXNS
import xyz.xszq.bot.chunithm.config.DesignerConfig
import xyz.xszq.bot.chunithm.database.ChunithmMusicAliasesTable
import xyz.xszq.bot.chunithm.music.GameVersion
import xyz.xszq.bot.chunithm.music.MusicInfo

class ChunithmData {
    val versions = mutableMapOf<String, GameVersion>()
    val musics = mutableMapOf<Int, MusicInfo>()
    lateinit var newestVersion: GameVersion
    lateinit var designer: DesignerConfig

    @OptIn(ExperimentalHoplite::class)
    suspend fun load(
        api: LXNS
    ) {
        versions.clear()
        musics.clear()
        designer = ConfigLoaderBuilder.default()
            .addFileSource("./data/chunithm/designer.yml")
            .withExplicitSealedTypes()
            .build()
            .loadConfigOrThrow<DesignerConfig>()

        musics.putAll(api.getMusicList())
        versions.putAll(musics.values.map { music ->
            music.version
        }.distinctBy { version ->
            version.name
        }.associateBy { version ->
            version.name
        })
        newestVersion = versions.values.maxByOrNull { version ->
            version.version
        } ?: GameVersion(0, "", 0)
        val aliases = api.getAliases().flatMap { (id, aliases) ->
            if (musics.containsKey(id)) aliases.map { alias -> id to alias }
            else emptyList()
        }
        ChunithmMusicAliasesTable.addAll(aliases)
    }
}