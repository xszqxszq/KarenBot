package xyz.xszq.bot.chunithm.component

import xyz.xszq.bot.chunithm.api.LXNS
import xyz.xszq.bot.chunithm.database.MusicAliasesTable
import xyz.xszq.bot.chunithm.music.GameVersion
import xyz.xszq.bot.chunithm.music.MusicInfo

class ChunithmData {
    val versions = mutableMapOf<String, GameVersion>()
    val musics = mutableMapOf<Int, MusicInfo>()
    lateinit var newestVersion: GameVersion

    suspend fun load(
        api: LXNS
    ) {
        versions.clear()
        musics.clear()

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
        MusicAliasesTable.addAll(aliases)
    }
}
