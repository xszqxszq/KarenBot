package xyz.xszq.bot.maimai.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import xyz.xszq.bot.database.newSuspendedTransaction
import xyz.xszq.bot.database.suspendedTransactionAsync
import java.util.concurrent.ConcurrentHashMap

/**#DataSourceSettings#
#LocalDataSource: database.h2
#BEGIN#
<data-source source="LOCAL" name="database.h2" uuid="a2e2dbaf-d8b1-4e7e-903a-ba903a84360a"><database-info product="H2" version="2.3.232 (2024-08-11)" jdbc-version="4.3" driver-name="H2 JDBC Driver" driver-version="2.3.232 (2024-08-11)" dbms="H2" exact-version="2.3.232" exact-driver-version="2.3"><identifier-quote-string>&quot;</identifier-quote-string></database-info><case-sensitivity plain-identifiers="upper" quoted-identifiers="exact"/><driver-ref>h2.unified</driver-ref><synchronize>true</synchronize><jdbc-driver>org.h2.Driver</jdbc-driver><jdbc-url>jdbc:h2:E:/Workspace/KarenBot-runtime/database/database.h2</jdbc-url><jdbc-additional-properties><property name="com.intellij.clouds.kubernetes.db.host.port"/><property name="com.intellij.clouds.kubernetes.db.enabled" value="false"/><property name="com.intellij.clouds.kubernetes.db.container.port"/></jdbc-additional-properties><auth-provider>no-auth</auth-provider><schema-mapping><introspection-scope><node kind="database" qname="@"><node kind="schema" qname="@"/></node></introspection-scope></schema-mapping><working-dir>$ProjectFileDir$</working-dir></data-source>
#END#


 * 谱面预览渲染设置
 */
object MaimaiPreviewSettingsTable: Table() {
    val id = varchar("id", 32)
    val noteSpeed = float("note_speed").default(8.0f)
    val touchSpeed = float("touch_speed").default(8.0f)
    val slideSpeed = integer("slide_speed").default(10)
    val starType = integer("star_type").default(0)
    val judgeStyle = integer("judge_style").default(14)
    val tapStyle = integer("tap_style").default(0)
    val holdStyle = integer("hold_style").default(0)
    val slideStyle = integer("slide_style").default(0)
    val outlineStyle = integer("outline_style").default(2)
    val brightness = integer("brightness").default(0)
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(id)

    /**
     * 渲染设置取值
     */
    data class Settings(
        val noteSpeed: Float = 8.0f,
        val touchSpeed: Float = 8.0f,
        val slideSpeed: Int = 10,
        val starType: Int = 0,
        val judgeStyle: Int = 14,
        val tapStyle: Int = 0,
        val holdStyle: Int = 0,
        val slideStyle: Int = 0,
        val outlineStyle: Int = 2,
        val brightness: Int = 0
    )

    private val cache = ConcurrentHashMap<String, Settings>()

    suspend fun load() = newSuspendedTransaction {
        selectAll().forEach { row ->
            cache[row[MaimaiPreviewSettingsTable.id]] = Settings(
                noteSpeed = row[noteSpeed],
                touchSpeed = row[touchSpeed],
                slideSpeed = row[slideSpeed],
                starType = row[starType],
                judgeStyle = row[judgeStyle],
                tapStyle = row[tapStyle],
                holdStyle = row[holdStyle],
                slideStyle = row[slideStyle],
                outlineStyle = row[outlineStyle],
                brightness = row[brightness]
            )
        }
    }

    suspend fun settings(openId: String): Settings {
        cache[openId] ?.let { return it }
        val loaded = suspendedTransactionAsync {
            val match = MaimaiPreviewSettingsTable.id eq openId
            selectAll().where { match }.firstOrNull() ?.let { row ->
                Settings(
                    noteSpeed = row[noteSpeed],
                    touchSpeed = row[touchSpeed],
                    slideSpeed = row[slideSpeed],
                    starType = row[starType],
                    judgeStyle = row[judgeStyle],
                    tapStyle = row[tapStyle],
                    holdStyle = row[holdStyle],
                    slideStyle = row[slideStyle],
                    outlineStyle = row[outlineStyle],
                    brightness = row[brightness]
                )
            }
        }.await() ?: Settings()
        cache[openId] = loaded
        return loaded
    }

    suspend fun put(openId: String, settings: Settings) {
        cache[openId] = settings
        newSuspendedTransaction {
            val match = MaimaiPreviewSettingsTable.id eq openId
            if (selectAll().where { match }.count() != 0L)
                update({ match }) { it.assign(settings) }
            else
                insert {
                    it[id] = openId
                    it.assign(settings)
                }
        }
    }

    private fun UpdateBuilder<*>.assign(settings: Settings) {
        this[noteSpeed] = settings.noteSpeed
        this[touchSpeed] = settings.touchSpeed
        this[slideSpeed] = settings.slideSpeed
        this[starType] = settings.starType
        this[judgeStyle] = settings.judgeStyle
        this[tapStyle] = settings.tapStyle
        this[holdStyle] = settings.holdStyle
        this[slideStyle] = settings.slideStyle
        this[outlineStyle] = settings.outlineStyle
        this[brightness] = settings.brightness
        this[updatedAt] = System.currentTimeMillis()
    }
}