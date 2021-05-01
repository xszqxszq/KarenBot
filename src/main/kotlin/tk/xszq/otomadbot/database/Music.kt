@file:Suppress("unused")
package tk.xszq.otomadbot.database

import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.baseName
import com.soywiz.korio.file.std.rootLocalVfs
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.`java-time`.timestamp
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import tk.xszq.otomadbot.bot
import tk.xszq.otomadbot.media.AudioEncodeUtils
import tk.xszq.otomadbot.media.getAudioDuration
import tk.xszq.otomadbot.pathPrefix
import tk.xszq.otomadbot.toFile
import java.io.File
import kotlin.random.Random

object Musics: IntIdTable() {
    override val tableName = "music"
    val filename = varchar("filename", 128)
    val category = varchar("category", 128)
    val name = text("name")
    val namePinyin = text("name_pinyin")
    val nameEnglish = text("name_english")
    val nameAlias = text("name_alias")
    val createTime = timestamp("createtime")
}

class Music(id: EntityID<Int>) : IntEntity(id) {
    var filename by Musics.filename
    var category by Musics.category
    var name by Musics.name
    var namePinyin by Musics.namePinyin
    var nameEnglish by Musics.nameEnglish
    var nameAlias by Musics.nameAlias
    var createTime by Musics.createTime
    companion object : IntEntityClass<Music>(Musics)
}
const val defaultMinDuration = 15.0

class RandomMusic(private val category: String) {
    @SuppressWarnings("WeakerAccess")
    lateinit var music: Music
    lateinit var file: VfsFile
    suspend fun init(): RandomMusic {
        val files = rootLocalVfs["$pathPrefix/music/$category"].listSimple()
        file = files[Random.nextInt(files.size)]
        music = newSuspendedTransaction(db = Databases.mysql) {
            Music.find {
                Musics.filename eq file.baseName
            }.first()
        }
        return this
    }
    suspend fun getRandomPeriod(duration: Double = defaultMinDuration): File? = AudioEncodeUtils.cropPeriod(
            file.toFile(), Random.nextDouble(0.0, file.getAudioDuration() - duration), duration)
    fun parsePossibleAnswers(): MutableList<String> {
        bot!!.logger.debug("当前猜东方原曲曲名：${music.name} " +
                "(${music.nameEnglish} / ${music.nameAlias} / ${music.namePinyin})")
        val possibleAnswers = mutableListOf(music.name, music.nameEnglish)
        music.namePinyin.replace('，',',').split(',').forEach {
            possibleAnswers.add(it.toLowerCase())
        }
        music.nameAlias.replace('，',',').split(',').forEach {
            possibleAnswers.add(it.toLowerCase())
        }
        return possibleAnswers
    }
}