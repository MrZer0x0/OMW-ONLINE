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

import java.io.File
import java.util.Locale

class ModsCollection(private val type: ModType,
                     private val dataPaths: List<String>,
                     private val db: ModsDatabaseOpenHelper) {

    constructor(type: ModType, dataFiles: String, db: ModsDatabaseOpenHelper) : this(type, listOf(dataFiles), db)

    val mods = arrayListOf<Mod>()
    private var extensions: Array<String> = when (type) {
        ModType.Resource -> arrayOf("bsa")
        ModType.Groundcover -> arrayOf("esp", "omwaddon")
        ModType.Plugin -> arrayOf("esm", "esp", "omwaddon", "omwgame")
    }

    companion object {
        private val PRIORITY_BSA = listOf("Morrowind.bsa", "Tribunal.bsa", "Bloodmoon.bsa")
        private val AUTO_ENABLED_PLUGINS = listOf(
            "Morrowind.esm",
            "Tribunal.esm",
            "Bloodmoon.esm",
            "GFM.esm",
            "Rebirth_Main.esm",
            "OAAB_Data.esm",
            "Tamriel_Data.esm",
            "TR_Mainland.esm",
            "Cyr_Main.esm",
            "Sky_Main.esm",
            "Wares-base.esm",
            "NOD_Core.esm",
            "TDoO_Main.esm",
            "Nirn_Core.esp"
        )
        private val GROUNDCOVER_KEYWORDS = listOf("grass", "groundcover")
    }

    init {
        if (isEmptyForType()) initDb()
        syncWithFs()
        if (isEmptyForType()) initDb()
    }

    private fun normalizedDataPaths(): List<String> {
        return dataPaths.filter { it.isNotBlank() }.distinct()
    }

    private fun isEmptyForType(): Boolean {
        var count = 0
        db.use {
            count = select("mod", "count(1)")
                .whereArgs("type = {type}", "type" to type.v)
                .exec { parseSingle(IntParser) }
        }
        return count == 0
    }

    private fun initDb() {
        when (type) {
            ModType.Plugin -> initDbMods(AUTO_ENABLED_PLUGINS, ModType.Plugin)
            ModType.Resource -> {
                initDbMods(PRIORITY_BSA, ModType.Resource)
                initDbAllBsaFromDisk()
            }
            ModType.Groundcover -> initDbGroundcoverFromDisk()
        }
    }

    private fun initDbAllBsaFromDisk() {
        val prioritySet = PRIORITY_BSA.map { it.toLowerCase(Locale.ROOT) }.toSet()

        val allBsa = normalizedDataPaths()
            .asSequence()
            .map { File(it) }
            .filter { it.exists() && it.isDirectory }
            .flatMap { dir -> dir.listFiles()?.asSequence() ?: emptySequence() }
            .filter { it.extension.toLowerCase(Locale.ROOT) == "bsa" }
            .map { it }
            .distinctBy { it.absolutePath }
            .filter { it.name.toLowerCase(Locale.ROOT) !in prioritySet }
            .sortedBy { it.name.toLowerCase(Locale.ROOT) }
            .toList()

        if (allBsa.isEmpty()) return

        var maxOrder = 0
        db.use {
            maxOrder = try {
                select("mod", "MAX(load_order)")
                    .whereArgs("type = {type}", "type" to ModType.Resource.v)
                    .exec { parseSingle(IntParser) }
            } catch (e: Exception) { 0 }
        }

        db.use {
            allBsa.forEach { file ->
                maxOrder += 1
                Mod(ModType.Resource, file.name, file.parent ?: "", file.absolutePath, maxOrder, true).insert(this)
            }
        }
    }

    private fun initDbGroundcoverFromDisk() {
        val files = normalizedDataPaths()
            .asSequence()
            .map { File(it) }
            .filter { it.exists() && it.isDirectory }
            .flatMap { dir -> dir.listFiles()?.asSequence() ?: emptySequence() }
            .filter { extensions.contains(it.extension.toLowerCase(Locale.ROOT)) }
            .distinctBy { it.absolutePath }
            .sortedBy { it.name.toLowerCase(Locale.ROOT) }
            .toList()

        if (files.isEmpty()) return

        db.use {
            var order = 0
            files.forEach { file ->
                order += 1
                val nameLower = file.name.toLowerCase(Locale.ROOT)
                val isGroundcover = GROUNDCOVER_KEYWORDS.any { kw -> nameLower.contains(kw) }
                Mod(ModType.Groundcover, file.name, file.parent ?: "", file.absolutePath, order, isGroundcover).insert(this)
            }
        }
    }

    private fun initDbMods(files: List<String>, type: ModType) {
        db.use {
            var order = 0
            files
                .mapNotNull { filename ->
                    normalizedDataPaths()
                        .map { File(it, filename) }
                        .firstOrNull { it.exists() }
                }
                .map { order += 1; Mod(type, it.name, it.parent ?: "", it.absolutePath, order, true) }
                .forEach { it.insert(this) }
        }
    }

    private fun syncWithFs() {
        var dbMods = listOf<Mod>()

        db.use {
            select("mod", "type", "filename", "source_path", "full_path", "load_order", "enabled")
                .whereArgs("type = {type}", "type" to type.v).exec {
                    dbMods = parseList(ModRowParser())
                }
        }

        val fsMods = normalizedDataPaths()
            .asSequence()
            .map { File(it) }
            .filter { it.exists() && it.isDirectory }
            .flatMap { dir -> dir.listFiles()?.asSequence() ?: emptySequence() }
            .filter { extensions.contains(it.extension.toLowerCase(Locale.ROOT)) }
            .map { file ->
                Mod(type, file.name, file.parent ?: "", file.absolutePath, 0, false)
            }
            .distinctBy { it.fullPath }
            .toList()

        val fsKeys = fsMods.map { it.sourcePath + "|" + it.filename }.toSet()
        val dbKeys = dbMods.map { it.sourcePath + "|" + it.filename }.toSet()

        dbMods.filter { fsKeys.contains(it.sourcePath + "|" + it.filename) }.forEach { mods.add(it) }

        var maxOrder = mods.maxBy { it.order }?.order ?: 0
        val newMods = arrayListOf<Mod>()
        fsMods.filter { !dbKeys.contains(it.sourcePath + "|" + it.filename) }
            .sortedWith(compareBy<Mod>({ pathOrder(it.sourcePath) }, { it.filename.toLowerCase(Locale.ROOT) }))
            .forEach { mod ->
                maxOrder += 1
                val enabledByDefault = when (type) {
                    ModType.Resource -> mod.filename in PRIORITY_BSA
                    ModType.Plugin -> mod.filename in AUTO_ENABLED_PLUGINS
                    ModType.Groundcover -> GROUNDCOVER_KEYWORDS.any { mod.filename.toLowerCase(Locale.ROOT).contains(it) }
                }
                val newMod = Mod(type, mod.filename, mod.sourcePath, mod.fullPath, maxOrder, enabledByDefault)
                newMods.add(newMod)
                mods.add(newMod)
            }

        db.use {
            transaction {
                dbMods.filter { !fsKeys.contains(it.sourcePath + "|" + it.filename) }.forEach {
                    delete("mod",
                        "type = {type} AND filename = {filename} AND source_path = {source_path}",
                        "type" to type.v,
                        "filename" to it.filename,
                        "source_path" to it.sourcePath)
                }
                newMods.forEach { it.insert(this) }
            }
        }

        mods.sortWith(compareBy<Mod>({ pathOrder(it.sourcePath) }, { it.order }, { it.filename.toLowerCase(Locale.ROOT) }))
    }

    private fun pathOrder(sourcePath: String): Int {
        val index = normalizedDataPaths().indexOf(sourcePath)
        return if (index >= 0) index else Int.MAX_VALUE
    }

    fun update() {
        db.use {
            mods.filter { it.dirty }.forEach {
                it.update(this)
                it.dirty = false
            }
        }
    }
}
