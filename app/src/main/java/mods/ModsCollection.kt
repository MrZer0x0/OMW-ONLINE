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

// Anko removed - using AnkoCompat.kt in same package
import java.io.File

/**
 * Represents an ordered list of mods of a specific type
 * @param type Type of the mods represented by this collection, Plugin or Resource
 * @param dataFiles Path to the directory of the mods (the Data Files directory)
 */
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
        /** Vanilla BSAs that must always load first, in this exact order */
        private val PRIORITY_BSA = listOf("Morrowind.bsa", "Tribunal.bsa", "Bloodmoon.bsa")

        /** Vanilla ESMs that must always load first, in this exact order */
        private val PRIORITY_ESM = listOf("Morrowind.esm", "Tribunal.esm", "Bloodmoon.esm")

        /** Substrings (lowercase) that mark a plugin as groundcover */
        private val GROUNDCOVER_KEYWORDS = listOf("grass", "groundcover")
    }

    init {
        if (isEmptyForType())
            initDb()
        syncWithFs()
        // The database might have become empty (e.g. if user deletes all mods) after the FS sync
        if (isEmptyForType())
            initDb()
    }

    /**
     * Checks if the mod DB is empty FOR THIS TYPE.
     * Previous version checked the entire table which caused Resource/Groundcover
     * init to be skipped if Plugins already existed.
     */
    private fun isEmptyForType(): Boolean {
        var count = 0
        db.use {
            count = select("mod", "count(1)")
                .whereArgs("type = {type}", "type" to type.v)
                .exec {
                    parseSingle(IntParser)
                }
        }
        return count == 0
    }

    /**
     * Inserts mods into the database on first run:
     * - Plugins: Morrowind/Tribunal/Bloodmoon ESMs (enabled)
     * - Resources: priority BSAs first, then ALL other BSAs alphabetically (all enabled)
     * - Groundcover: all files with grass/groundcover in name auto-enabled
     */
    private fun initDb() {
        when (type) {
            ModType.Plugin -> {
                initDbMods(PRIORITY_ESM, ModType.Plugin)
            }
            ModType.Resource -> {
                initDbMods(PRIORITY_BSA, ModType.Resource)
                initDbAllBsaFromDisk()
            }
            ModType.Groundcover -> {
                initDbGroundcoverFromDisk()
            }
        }
    }

    /**
     * Discovers all .bsa files on disk NOT in the priority list,
     * sorts alphabetically, inserts all as enabled.
     */
    private fun initDbAllBsaFromDisk() {
        val prioritySet = PRIORITY_BSA.map { it.lowercase() }.toSet()

        val allBsa = dataPaths
            .asSequence()
            .filter { it.isNotBlank() }
            .map { File(it) }
            .filter { it.exists() && it.isDirectory }
            .flatMap { dir -> dir.listFiles()?.asSequence() ?: emptySequence() }
            .filter { it.extension.lowercase() == "bsa" }
            .map { it.name }
            .distinct()
            .filter { it.lowercase() !in prioritySet }
            .sorted()
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
            allBsa.forEach { bsaName ->
                maxOrder += 1
                Mod(ModType.Resource, bsaName, maxOrder, true).insert(this)
            }
        }
    }

    /**
     * Discovers all groundcover-eligible files (.esp, .omwaddon) from disk.
     * Files containing "grass" or "groundcover" (case-insensitive) auto-enabled.
     */
    private fun initDbGroundcoverFromDisk() {
        val groundcoverFiles = dataPaths
            .asSequence()
            .filter { it.isNotBlank() }
            .map { File(it) }
            .filter { it.exists() && it.isDirectory }
            .flatMap { dir -> dir.listFiles()?.asSequence() ?: emptySequence() }
            .filter { extensions.contains(it.extension.lowercase()) }
            .map { it.name }
            .distinct()
            .sorted()
            .toList()

        if (groundcoverFiles.isEmpty()) return

        db.use {
            var order = 0
            groundcoverFiles.forEach { filename ->
                order += 1
                val nameLower = filename.lowercase()
                val isGroundcover = GROUNDCOVER_KEYWORDS.any { kw -> nameLower.contains(kw) }
                Mod(ModType.Groundcover, filename, order, isGroundcover).insert(this)
            }
        }
    }

    /**
     * Inserts specific mods by filename. Only inserts mods that exist on disk.
     */
    private fun initDbMods(files: List<String>, type: ModType) {
        db.use {
            var order = 0
            files
                .map { File(primaryDataPath(), it) }
                .filter { it.exists() }
                .map { order += 1; Mod(type, it.name, order, true) }
                .forEach { it.insert(this) }
        }
    }

    /**
     * Synchronizes state of mods in database with the actual mod files on disk.
     */
    private fun syncWithFs() {
        var dbMods = listOf<Mod>()

        db.use {
            select("mod", "type", "filename", "load_order", "enabled")
                .whereArgs("type = {type}", "type" to type.v).exec {
                    dbMods = parseList(ModRowParser())
                }
        }

        val modFiles = dataPaths
            .asSequence()
            .filter { it.isNotBlank() }
            .map { File(it) }
            .filter { it.exists() && it.isDirectory }
            .flatMap { dir -> dir.listFiles()?.asSequence() ?: emptySequence() }
            .filter { extensions.contains(it.extension.lowercase()) }
            .toList()

        val fsNames = mutableSetOf<String>()
        modFiles.forEach { fsNames.add(it.name) }

        val dbNames = mutableSetOf<String>()
        dbMods.forEach { dbNames.add(it.filename) }

        dbMods.filter { fsNames.contains(it.filename) }.forEach { mods.add(it) }

        var maxOrder = mods.maxByOrNull { it.order }?.order ?: 0

        val newMods = arrayListOf<Mod>()
        (fsNames - dbNames).sorted().forEach {
            maxOrder += 1
            // New BSAs auto-enable; new groundcovers auto-enable if name matches keywords
            val autoEnable = when (type) {
                ModType.Resource -> true
                ModType.Groundcover -> {
                    val nameLower = it.lowercase()
                    GROUNDCOVER_KEYWORDS.any { kw -> nameLower.contains(kw) }
                }
                else -> false
            }
            val mod = Mod(type, it, maxOrder, autoEnable)
            newMods.add(mod)
            mods.add(mod)
        }

        db.use {
            transaction {
                (dbNames - fsNames).forEach {
                    delete("mod",
                        "type = {type} AND filename = {filename}",
                        "type" to type.v,
                        "filename" to it)
                }
                newMods.forEach { it.insert(this) }
            }
        }

        mods.sortBy { it.order }
    }

    private fun primaryDataPath(): String {
        return dataPaths.firstOrNull { it.isNotBlank() } ?: ""
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
