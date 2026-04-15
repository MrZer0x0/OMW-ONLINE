/*
    Copyright (C) 2019 Ilya Zhuravlev

    This file is part of OpenMW-Android.

    OpenMW-Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenMW-Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenMW-Android.  If not, see <https://www.gnu.org/licenses/>.
*/

package mods

import android.database.sqlite.SQLiteDatabase

enum class ModType(val v: Int) {
    Plugin(1),
    Resource(2),
    Groundcover(3);

    companion object {
        private val reverseValues: Map<Int, ModType> = values().associate { it.v to it }
        fun valueFrom(i: Int): ModType = reverseValues.getValue(i)
    }
}

/**
 * Representation of a single mod in the database
 */
class Mod(
    val type: ModType,
    val filename: String,
    val sourcePath: String,
    val fullPath: String,
    var order: Int,
    var enabled: Boolean
) {

    var dirty: Boolean = false

    fun update(db: SQLiteDatabase) {
        db.update("mod",
            "load_order" to order,
            "enabled" to enabled,
            "full_path" to fullPath)
            .whereArgs("filename = {filename} AND type = {type} AND source_path = {source_path}",
                "filename" to filename,
                "type" to type.v,
                "source_path" to sourcePath).exec()
    }

    fun insert(db: SQLiteDatabase) {
        db.insert("mod",
            "type" to type.v,
            "filename" to filename,
            "source_path" to sourcePath,
            "full_path" to fullPath,
            "load_order" to order,
            "enabled" to (if (enabled) 1 else 0))
    }
}

class ModRowParser : RowParser<Mod> {
    override fun parseRow(columns: Array<Any?>): Mod {
        return Mod(
            ModType.valueFrom((columns[0] as Long).toInt()),
            columns[1] as String,
            (columns[2] as? String) ?: "",
            (columns[3] as? String) ?: "",
            (columns[4] as Long).toInt(),
            (columns[5] as Long) != 0L)
    }
}
