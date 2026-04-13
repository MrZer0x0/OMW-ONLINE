/*
    Anko SQLite compatibility layer.
    Replaces org.jetbrains.anko:anko-sqlite which was deprecated and removed.
*/

package mods

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.preference.PreferenceManager

// Column type constants (used in createTable)
val INTEGER: String = "INTEGER"
val TEXT: String = "TEXT"

// ManagedSQLiteOpenHelper replacement
abstract class ManagedSQLiteOpenHelper(
    ctx: Context,
    name: String,
    factory: SQLiteDatabase.CursorFactory? = null,
    version: Int = 1
) : SQLiteOpenHelper(ctx, name, factory, version) {

    fun <T> use(f: SQLiteDatabase.() -> T): T {
        val db = writableDatabase
        try {
            return db.f()
        } finally {
            db.close()
        }
    }
}

// RowParser interface
interface RowParser<T> {
    fun parseRow(columns: Array<Any?>): T
}

// IntParser singleton
object IntParser {
    fun parseRow(columns: Array<Any?>): Int {
        return (columns[0] as Long).toInt()
    }
}

// Extension: createTable
fun SQLiteDatabase.createTable(
    tableName: String,
    ifNotExists: Boolean,
    vararg columns: Pair<String, String>
) {
    val ifNotExistsStr = if (ifNotExists) "IF NOT EXISTS" else ""
    val cols = columns.joinToString(", ") { "${it.first} ${it.second}" }
    execSQL("CREATE TABLE $ifNotExistsStr $tableName ($cols)")
}

// Extension: createIndex
fun SQLiteDatabase.createIndex(
    indexName: String,
    tableName: String,
    unique: Boolean,
    ifNotExists: Boolean,
    vararg columns: String
) {
    val uniqueStr = if (unique) "UNIQUE" else ""
    val ifNotExistsStr = if (ifNotExists) "IF NOT EXISTS" else ""
    val cols = columns.joinToString(", ")
    execSQL("CREATE $uniqueStr INDEX $ifNotExistsStr $indexName ON $tableName ($cols)")
}

// Extension: select
fun SQLiteDatabase.select(
    tableName: String,
    vararg columns: String
): SelectQueryBuilder {
    return SelectQueryBuilder(this, tableName, columns.toList())
}

// Extension: insert
fun SQLiteDatabase.insert(
    tableName: String,
    vararg values: Pair<String, Any?>
): Long {
    val cv = ContentValues()
    for ((key, value) in values) {
        when (value) {
            is Int -> cv.put(key, value)
            is Long -> cv.put(key, value)
            is String -> cv.put(key, value)
            is Boolean -> cv.put(key, if (value) 1 else 0)
            null -> cv.putNull(key)
            else -> cv.put(key, value.toString())
        }
    }
    return insert(tableName, null, cv)
}

// Extension: update
fun SQLiteDatabase.update(
    tableName: String,
    vararg values: Pair<String, Any?>
): UpdateQueryBuilder {
    return UpdateQueryBuilder(this, tableName, values.toList())
}

// Extension: delete
fun SQLiteDatabase.delete(
    tableName: String,
    whereClause: String,
    vararg args: Pair<String, Any?>
): Int {
    val (resolvedWhere, resolvedArgs) = resolveArgs(whereClause, args.toList())
    return delete(tableName, resolvedWhere, resolvedArgs)
}

// Extension: transaction
fun SQLiteDatabase.transaction(action: SQLiteDatabase.() -> Unit) {
    beginTransaction()
    try {
        action()
        setTransactionSuccessful()
    } finally {
        endTransaction()
    }
}

// SelectQueryBuilder
class SelectQueryBuilder(
    private val db: SQLiteDatabase,
    private val table: String,
    private val columns: List<String>
) {
    private var whereClause: String? = null
    private var whereArgs: Array<String>? = null

    fun whereArgs(where: String, vararg args: Pair<String, Any?>): SelectQueryBuilder {
        val (resolvedWhere, resolvedArgs) = resolveArgs(where, args.toList())
        this.whereClause = resolvedWhere
        this.whereArgs = resolvedArgs
        return this
    }

    fun <T> exec(action: Cursor.() -> T): T {
        val cursor = db.query(table, columns.toTypedArray(), whereClause, whereArgs, null, null, null)
        try {
            return cursor.action()
        } finally {
            cursor.close()
        }
    }
}

// UpdateQueryBuilder
class UpdateQueryBuilder(
    private val db: SQLiteDatabase,
    private val table: String,
    private val values: List<Pair<String, Any?>>
) {
    private var whereClause: String? = null
    private var whereArgs: Array<String>? = null

    fun whereArgs(where: String, vararg args: Pair<String, Any?>): UpdateQueryBuilder {
        val (resolvedWhere, resolvedArgs) = resolveArgs(where, args.toList())
        this.whereClause = resolvedWhere
        this.whereArgs = resolvedArgs
        return this
    }

    fun exec() {
        val cv = ContentValues()
        for ((key, value) in values) {
            when (value) {
                is Int -> cv.put(key, value)
                is Long -> cv.put(key, value)
                is String -> cv.put(key, value)
                is Boolean -> cv.put(key, if (value) 1 else 0)
                null -> cv.putNull(key)
                else -> cv.put(key, value.toString())
            }
        }
        db.update(table, cv, whereClause, whereArgs)
    }
}

// Cursor extensions: parseSingle and parseList
fun Cursor.parseSingle(parser: IntParser): Int {
    moveToFirst()
    val cols = Array<Any?>(columnCount) { i ->
        when (getType(i)) {
            Cursor.FIELD_TYPE_INTEGER -> getLong(i)
            Cursor.FIELD_TYPE_STRING -> getString(i)
            Cursor.FIELD_TYPE_FLOAT -> getDouble(i)
            Cursor.FIELD_TYPE_NULL -> null
            else -> getString(i)
        }
    }
    return parser.parseRow(cols)
}

fun <T> Cursor.parseList(parser: RowParser<T>): List<T> {
    val result = mutableListOf<T>()
    while (moveToNext()) {
        val cols = Array<Any?>(columnCount) { i ->
            when (getType(i)) {
                Cursor.FIELD_TYPE_INTEGER -> getLong(i)
                Cursor.FIELD_TYPE_STRING -> getString(i)
                Cursor.FIELD_TYPE_FLOAT -> getDouble(i)
                Cursor.FIELD_TYPE_NULL -> null
                else -> getString(i)
            }
        }
        result.add(parser.parseRow(cols))
    }
    return result
}

// Helper: resolve {name} placeholders in where clauses
private fun resolveArgs(where: String, args: List<Pair<String, Any?>>): Pair<String, Array<String>> {
    var resolvedWhere = where
    val resolvedArgs = mutableListOf<String>()
    for ((key, value) in args) {
        resolvedWhere = resolvedWhere.replace("{$key}", "?")
        resolvedArgs.add(value.toString())
    }
    return Pair(resolvedWhere, resolvedArgs.toTypedArray())
}

// Extension: defaultSharedPreferences (replaces org.jetbrains.anko.defaultSharedPreferences)
val Context.defaultSharedPreferences
    get() = PreferenceManager.getDefaultSharedPreferences(this)
