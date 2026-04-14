/*
    Copyright (C) 2015, 2016 sandstranger
    Copyright (C) 2018, 2019 Ilya Zhuravlev

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

package ui.activity

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.app.ProgressDialog
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.os.Build.VERSION
import android.preference.PreferenceManager
import android.system.ErrnoException
import android.system.Os
import android.util.DisplayMetrics
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.bugsnag.android.Bugsnag

import com.libopenmw.openmw.BuildConfig
import com.libopenmw.openmw.R
import constants.Constants
import file.GameInstaller

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader

import file.utils.CopyFilesFromAssets
import mods.ModType
import mods.ModsCollection
import mods.ModsDatabaseOpenHelper
import ui.fragments.FragmentSettings
import permission.PermissionHelper
import utils.MyApp
import utils.Utils.hideAndroidControls
import java.util.*

import android.util.Base64

import android.view.DisplayCutout
import android.graphics.Rect

import android.content.res.Configuration

import android.os.Build
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MyApp.app.defaultScaling = determineScaling()

        Thread.setDefaultUncaughtExceptionHandler(CaptureCrash())

        PermissionHelper.getWriteExternalStoragePermission(this@MainActivity)
        setContentView(R.layout.main)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        val theme = prefs.getInt(getString(R.string.theme), 2)
        if(theme == 0) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        else if(theme == 1) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        fragmentManager.beginTransaction()
            .replace(R.id.content_frame, FragmentSettings()).commit()

        setSupportActionBar(findViewById(R.id.main_toolbar))

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener { checkStartGame() }

        if (prefs.getString("bugsnag_consent", "")!! == "") {
            askBugsnagConsent()
        }

        // create user dirs
        File(Constants.USER_CONFIG).mkdirs()
        File(Constants.USER_FILE_STORAGE + "/launcher/icons").mkdirs()
        if (!File(Constants.USER_OPENMW_CFG).exists())
        File(Constants.USER_OPENMW_CFG).writeText("# This is the user openmw.cfg. Feel free to modify it as you wish.\n\nfallback=LightAttenuation_UseConstant,0\nfallback=LightAttenuation_ConstantValue,0.318\nfallback=LightAttenuation_UseLinear,0\nfallback=LightAttenuation_LinearMethod,1\nfallback=LightAttenuation_LinearValue,1.0\nfallback=LightAttenuation_LinearRadiusMult,1.0\nfallback=LightAttenuation_UseQuadratic,1\nfallback=LightAttenuation_QuadraticMethod,2\nfallback=LightAttenuation_QuadraticValue,13.5\nfallback=LightAttenuation_QuadraticRadiusMult,2.0\nfallback=LightAttenuation_OutQuadInLin,0")

        // create icons files hint
        if (!File(Constants.USER_FILE_STORAGE + "/launcher/icons/paste custom icons here.txt").exists())
            File(Constants.USER_FILE_STORAGE + "/launcher/icons/paste custom icons here.txt").writeText(
"attack.png \ninventory.png \njournal.png \njump.png \nkeyboard.png \nmouse.png \npause.png \npointer_arrow.png \nrun.png \nsave.png \nsneak.png \nthird_person.png \ntoggle_magic.png \ntoggle_weapon.png \ntoggle.png \nuse.png \nwait.png")

        // create current mods dir in case someone deleted it
        val modsDir = PreferenceManager.getDefaultSharedPreferences(this).getString("mods_dir", "")!!
        if (modsDir != "" ) File(modsDir).mkdirs()
    }

    @SuppressLint("ApplySharedPref")
    private fun setBugsnagConsent(consent: String) {
        val currentConsent = prefs.getString("bugsnag_consent", "")!!
        if (currentConsent == consent)
            return

        val needRestart = currentConsent == "true" && consent == "false"

        with (prefs.edit()) {
            putString("bugsnag_consent", consent)
            commit()
        }

        if (needRestart) {
            AlertDialog.Builder(this)
                .setOnDismissListener { System.exit(0) }
                .setTitle(R.string.bugsnag_consent_restart_title)
                .setMessage(R.string.bugsnag_consent_restart_message)
                .setPositiveButton(android.R.string.ok) { _, _ -> System.exit(0) }
                .show()
        }
    }

    fun openUrl(url: String) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(browserIntent)
        } catch (e: ActivityNotFoundException) {
            AlertDialog.Builder(this)
                .setTitle(R.string.no_browser_title)
                .setMessage(getString(R.string.no_browser_message, url))
                .setPositiveButton(android.R.string.ok) { _, _ -> }
                .show()
        }
    }

    private fun askBugsnagConsent() {
        if (!MyApp.haveBugsnagApiKey)
            return

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.bugsnag_consent_title)
            .setMessage(R.string.bugsnag_consent_message)
            .setNeutralButton(R.string.bugsnag_policy) { _, _ -> }
            .setNegativeButton(R.string.bugsnag_no) { _, _ -> setBugsnagConsent("false") }
            .setPositiveButton(R.string.bugsnag_yes) { _, _ -> setBugsnagConsent("true") }
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            openUrl("https://omw.xyz.is/privacy-policy.html")
        }
    }

    private fun checkStartGame() {
        val inst = GameInstaller(prefs.getString("game_files", "")!!)
        if (!inst.check()) {
            AlertDialog.Builder(this)
                .setTitle(R.string.no_data_files_title)
                .setMessage(R.string.no_data_files_message)
                .setNeutralButton(R.string.dialog_howto) { _, _ ->
                    openUrl("https://omw.xyz.is/game.html")
                }
                .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int -> }
                .show()
            return
        }

        var dataFilesList = ArrayList<String>()
        dataFilesList.add(inst.findDataFiles())

        val modsDir = PreferenceManager.getDefaultSharedPreferences(this)
            .getString("mods_dir", "")!!

        File(Constants.USER_FILE_STORAGE + "/launcher/ModsDatabases" + modsDir).mkdirs()

        File(modsDir).listFiles()?.forEach {
            if (!it.isFile())
                dataFilesList.add(modsDir + it.getName())
        }

        val plugins = ModsCollection(ModType.Plugin, dataFilesList,
            ModsDatabaseOpenHelper.getInstance(this))
        if (plugins.mods.count { it.enabled } == 0) {
            AlertDialog.Builder(this)
                .setTitle(R.string.no_content_files_title)
                .setMessage(R.string.no_content_files_message)
                .setNeutralButton(R.string.dialog_howto) { _, _ ->
                    openUrl("https://t.me/morrowind24")
                }
                .setNegativeButton(R.string.no_content_files_dismiss) { _, _ -> startGame() }
                .setPositiveButton(R.string.configure_mods) { _, _ ->
                    this.startActivity(Intent(this, ModsActivity::class.java))
                }
                .show()

            return
        }

        startGame()
    }

    private fun deleteRecursive(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory)
            for (child in fileOrDirectory.listFiles())
                deleteRecursive(child)

        fileOrDirectory.delete()
    }

    private fun logConfig() {
    }

    private fun runGame() {
        logConfig()
        val intent = Intent(this@MainActivity,
            GameActivity::class.java)
        finish()

        this@MainActivity.startActivityForResult(intent, 1)
    }

    private fun obtainFixedScreenResolution() {
        val customResolution = prefs.getString("pref_customResolution", "")
        val sep = customResolution!!.indexOf("x")
        if (sep > 0) {
            try {
                val x = Integer.parseInt(customResolution.substring(0, sep))
                val y = Integer.parseInt(customResolution.substring(sep + 1))

                resolutionX = x
                resolutionY = y
            } catch (e: NumberFormatException) {
            }
        }
    }

    private fun generateOpenmwCfg() {
        val base: String
        val fallback: String

        try {
            base = File(Constants.OPENMW_BASE_CFG).readText()
            fallback = File(Constants.OPENMW_FALLBACK_CFG).readText()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read openmw.base.cfg or openmw.fallback.cfg", e)
            return
        }

        val modsDir = PreferenceManager.getDefaultSharedPreferences(this)
            .getString("mods_dir", "")!!

        val db = ModsDatabaseOpenHelper.getInstance(this)

        var dataFilesList = ArrayList<String>()
        var dataDirsPath = ArrayList<String>()
        dataFilesList.add(GameInstaller.getDataFiles(this))
        dataDirsPath.add(modsDir)

        File(modsDir).listFiles()?.forEach {
            if (!it.isFile())
                dataFilesList.add(modsDir + it.getName())
        }

        val resources = ModsCollection(ModType.Resource, dataFilesList, db)
        val dirs = ModsCollection(ModType.Dir, dataDirsPath, db)
        val plugins = ModsCollection(ModType.Plugin, dataFilesList, db)
        val groundcovers = ModsCollection(ModType.Groundcover, dataFilesList, db)

        try {
            var output = base + "\n" + fallback + "\n"

            resources.mods
                .filter { it.enabled }
                .forEach { output += "fallback-archive=${it.filename}\n" }

            dirs.mods
                .filter { it.enabled }
                .forEach { output += "data=" + '"' + modsDir + it.filename + '"' + "\n" }

            plugins.mods
                .filter { it.enabled }
                .forEach { output += "content=${it.filename}\n" }

            groundcovers.mods
                .filter { it.enabled }
                .forEach { output += "groundcover=${it.filename}\n" }

            output += "data=" + '"' + Constants.USER_FILE_STORAGE + "launcher/delta" + '"' + "\n"

            File(Constants.OPENMW_CFG).writeText(output)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to generate openmw.cfg.", e)
        }
    }

    private fun determineScaling(): Float {
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(dm)
        return maxOf(dm.heightPixels, dm.widthPixels) / 1024.0f
    }

    private fun reinstallStaticFiles() {
        removeStaticFiles()

        val assetCopier = CopyFilesFromAssets(this)
        assetCopier.copy("libopenmw/resources", Constants.RESOURCES)
        assetCopier.copy("libopenmw/openmw", Constants.GLOBAL_CONFIG)

        File(Constants.VERSION_STAMP).writeText(BuildConfig.RANDOMIZER.toString())
    }

    private fun removeStaticFiles() {
        File(Constants.VERSION_STAMP).delete()
        deleteRecursive(File(Constants.GLOBAL_CONFIG))
        deleteRecursive(File(Constants.RESOURCES))
    }

    private fun removeUserConfig() {
        deleteRecursive(File(Constants.USER_CONFIG))
        File(Constants.USER_CONFIG).mkdirs()
        File(Constants.USER_OPENMW_CFG).writeText("# This is the user openmw.cfg. Feel free to modify it as you wish.\n")
    }

    private fun removeResourceFiles() {
        reinstallStaticFiles()
        deleteRecursive(File(Constants.USER_FILE_STORAGE + "/resources/"))

        var src = File(Constants.RESOURCES)
        var dst = File(Constants.USER_FILE_STORAGE + "/resources/")
        dst.mkdirs()
        src.copyRecursively(dst, true)
    }

    private fun configureDefaultsBin(args: Map<String, String>) {
        val defaults = File(Constants.DEFAULTS_BIN).readText()
        val decoded = String(android.util.Base64.decode(defaults, android.util.Base64.DEFAULT))
        val lines = decoded.lines().map {
            for ((k, v) in args) {
                if (it.startsWith("$k ="))
                    return@map "$k = $v"
            }
            it
        }
        val data = lines.joinToString("\n")

        val encoded = android.util.Base64.encodeToString(data.toByteArray(), android.util.Base64.NO_WRAP)
        File(Constants.DEFAULTS_BIN).writeText(encoded)
    }

    private fun writeSetting(category: String, name: String, value: String) {
        var lineList = mutableListOf<String>()
        var lineNumber = 0
        var categoryFound = 0
        var categoryLine = 0
        var nameFound = 0
        var nameLine = 0
        var currentCategory = ""

        File(Constants.USER_CONFIG + "/settings.cfg").useLines {
            lines -> lines.forEach {
                lineList.add(it)
                if (it.contains("[") && it.contains("]")) currentCategory = it.replace("[", "").replace("]", "").replace(" ", "")
                if (currentCategory == category.replace(" ", "") && categoryFound == 0 ) { categoryLine = lineNumber; categoryFound = 1 }
                if (currentCategory == category.replace(" ", "") && it.substringBefore("=").replace(" ", "") == name.replace(" ", ""))
                    { nameLine = lineNumber; nameFound = 1 }

                lineNumber++
            }
        }

        if(nameFound == 1)
            lineList.set(nameLine, name + " = " + value)
        if(categoryFound == 1 && nameFound == 0)
            lineList.add(categoryLine + 1, name + " = " + value)
        if(categoryFound == 0 && nameFound == 0)
            lineList.add(lineNumber, "\n" + "[" + category + "]" + "\n" + name + " = " + value)

        var output = ""
        lineList.forEach { output += it + "\n" }

        File(Constants.USER_CONFIG + "/settings.cfg").writeText(output)
    }


    private fun writeUserSettings() {
        File(Constants.USER_CONFIG + "/settings.cfg").createNewFile()

        val displayInCutoutArea = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_display_cutout_area", true)
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(dm)
        val orientation = this.getResources().getConfiguration().orientation
        var displayWidth = 0
        var displayHeight = 0
        val cutout = if (android.os.Build.VERSION.SDK_INT < 29) null else windowManager.defaultDisplay.getCutout()

        if (cutout != null) {
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                displayWidth = if(resolutionX == 0) dm.heightPixels else resolutionX
                displayHeight = if(resolutionY == 0) dm.widthPixels else resolutionY
                if( displayInCutoutArea == false && resolutionX == 0) {
                    val cutoutRectTop = cutout.getBoundingRectTop()
                    val cutoutRectBottom = cutout.getBoundingRectBottom()
                    displayWidth = dm.heightPixels - maxOf(cutoutRectTop.bottom, cutoutRectBottom.bottom - cutoutRectBottom.top)
                }
            } else {
                displayWidth = if(resolutionX == 0) dm.widthPixels else resolutionX
                displayHeight = if(resolutionY == 0) dm.heightPixels else resolutionY
                if( displayInCutoutArea == false && resolutionY == 0) {
                    val cutoutRectLeft = cutout.getBoundingRectLeft()
                    val cutoutRectRight = cutout.getBoundingRectRight()
                    displayWidth = dm.widthPixels - maxOf(cutoutRectLeft.right, cutoutRectRight.right - cutoutRectRight.left)
                }
            }

            writeSetting("Video", "resolution x", displayWidth.toString())
            writeSetting("Video", "resolution y", displayHeight.toString())
        } else {
            if (resolutionX != 0 && resolutionY != 0) {
                writeSetting("Video", "resolution x", displayWidth.toString())
                writeSetting("Video", "resolution y", displayHeight.toString())
            } else {
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    writeSetting("Video", "resolution x", dm.heightPixels.toString())
                    writeSetting("Video", "resolution y", dm.widthPixels.toString())
                } else {
                    writeSetting("Video", "resolution x", dm.widthPixels.toString())
                    writeSetting("Video", "resolution y", dm.heightPixels.toString())
                }
            }
        }
        writeSetting("Shadows", "enable shadows", if(prefs.getBoolean("shadows_key", false)) "true" else "false")
        writeSetting("Shadows", "object shadows", if(prefs.getBoolean("shadowso_key", false)) "true" else "false")
        writeSetting("Input", "enable gyroscope", if(prefs.getBoolean("gyroscope_key", false)) "true" else "false")
        writeSetting("Cells", "preload doors", if(prefs.getBoolean("preload_key", false)) "true" else "false")
        writeSetting("Terrain", "distant terrain", if(prefs.getBoolean("terrain_key", false)) "true" else "false")
        writeSetting("Shaders", "auto use object normal maps", if(prefs.getBoolean("on_key", false)) "true" else "false")
        writeSetting("Shaders", "auto use terrain normal maps", if(prefs.getBoolean("tn_key", false)) "true" else "false")
        writeSetting("Shaders", "auto use object specular maps", if(prefs.getBoolean("os_key", false)) "true" else "false")
        writeSetting("Shaders", "auto use terrain specular", if(prefs.getBoolean("ts_key", false)) "true" else "false")
        writeSetting("Shaders", "soft particles", if(prefs.getBoolean("sp_key", false)) "true" else "false")
    }

    private fun startGame() {
        var scaling = 0f

        try {
            scaling = prefs.getString("pref_uiScaling", "")!!.toFloat()
        } catch (e: NumberFormatException) {
            with(prefs.edit()) {
                putString("pref_uiScaling", "")
                apply()
            }
        }

        if (scaling == 0f) {
            scaling = MyApp.app.defaultScaling
        }

        val dialog = ProgressDialog.show(
            this, "", "Запуск OpenMW Mobile...", true)

        val activity = this

        hideAndroidControls(this)

        val th = Thread {
            try {
                try {
                    val stamp = File(Constants.VERSION_STAMP).readText().trim()
                    if (stamp.toInt() != BuildConfig.RANDOMIZER) {
                        removeResourceFiles()
                    }
                } catch (e: Exception) {
                    removeResourceFiles()
                }

                val inst = GameInstaller(prefs.getString("game_files", "")!!)

                inst.convertIni(prefs.getString("pref_encoding", GameInstaller.DEFAULT_CHARSET_PREF)!!)

                generateOpenmwCfg()

                file.Writer.write(Constants.OPENMW_CFG, "resources", Constants.RESOURCES)
                file.Writer.write(Constants.OPENMW_CFG, "data", "\"" + inst.findDataFiles() + "\"")

                file.Writer.write(Constants.OPENMW_CFG, "encoding", prefs!!.getString("pref_encoding", GameInstaller.DEFAULT_CHARSET_PREF)!!)

                var src = File(Constants.RESOURCES)
                var dst = File(Constants.USER_FILE_STORAGE + "/resources/")
                val resourcesDirCreated :Boolean = dst.mkdirs()

                if(resourcesDirCreated)
                    src.copyRecursively(dst, false)

                obtainFixedScreenResolution()

                configureDefaultsBin(mapOf(
                        "scaling factor" to "%.2f".format(Locale.ROOT, scaling),
                        "viewing distance" to "4096.0",
                        "camera sensitivity" to "0.5",
                        "stats x" to "0.0",
                        "stats y" to "0.0",
                        "stats w" to "0.375",
                        "stats h" to "0.4275",
                        "spells x" to "0.625",
                        "spells y" to "0.5725",
                        "spells w" to "0.375",
                        "spells h" to "0.4275",
                        "map x" to "0.625",
                        "map y" to "0.0",
                        "map w" to "0.375",
                        "map h" to "0.5725",
                        "inventory y" to "0.4275",
                        "inventory w" to "0.6225",
                        "inventory h" to "0.5725",
                        "inventory container x" to "0.0",
                        "inventory container y" to "0.4275",
                        "inventory container w" to "0.6225",
                        "inventory container h" to "0.5725",
                        "inventory barter x" to "0.0",
                        "inventory barter y" to "0.4275",
                        "inventory barter w" to "0.6225",
                        "inventory barter h" to "0.5725",
                        "inventory companion x" to "0.0",
                        "inventory companion y" to "0.4275",
                        "inventory companion w" to "0.6225",
                        "inventory companion h" to "0.5725",
                        "dialogue x" to "0.095",
                        "dialogue y" to "0.095",
                        "dialogue w" to "0.810",
                        "dialogue h" to "0.890",
                        "console x" to "0.0",
                        "console y" to "0.0",
                        "container x" to "0.25",
                        "container y" to "0.0",
                        "container w" to "0.75",
                        "container h" to "0.375",
                        "barter x" to "0.25",
                        "barter y" to "0.0",
                        "barter w" to "0.75",
                        "barter h" to "0.375",
                        "companion x" to "0.25",
                        "companion y" to "0.0",
                        "companion w" to "0.75",
                        "companion h" to "0.375",
                        "stretch menu background" to "true",
                        "use additional anim sources" to "true",
                        "prevent merchant equipping" to "true",
                        "framerate limit" to "60.60",
                        "color topic enable" to "true",
                        "preferred locales" to "ru,en",
                        "shader" to "true",
                        "small feature culling pixel size" to "9.0",
                        "reverse z" to "true",
                        "preload num threads" to "1",
                        "preload doors" to "false",
                        "actor collision shape type" to "1",
                        "show owned" to "3",
                        "show projectile damage" to "true",
                        "show melee info" to "true",
                        "show enchant chance" to "true",
                        "strength influences hand to hand" to "2",
                        "weapon sheathing" to "true",
                        "shield sheathing" to "true",
                        "only appropriate ammunition bypasses resistance" to "true",
                        "max lights" to "8",
                        "enable" to "true",
                        "paging" to "true",
                        "load unsupported nif files" to "true",
                        "max quicksaves" to "3",
                        "anisotropy" to "4",
                        "turn to movement direction" to "true",
                        "smooth movement" to "true",
                        "always allow stealing from knocked out actors" to "true",
                        "lod factor" to "0.7",
                        "composite map resolution" to "512",
                        "max composite geometry size" to "4.0",
                        "object paging" to "true",
                        "object paging active grid" to "true",
                        "object paging merge factor" to "100000",
                        "object paging min size" to "1",
                        "object paging min size merge factor" to "0.039",
                        "object paging min size cost multiplier" to "1",
                        "player shadows" to "true",
                        "actor shadows" to "true",
                        "maximum shadow map distance" to "4096"
                ))

                writeUserSettings()

                runOnUiThread {
                    obtainFixedScreenResolution()
                    dialog.hide()
                    runGame()
                }
            } catch (e: IOException) {
                Log.e(TAG, "Невозможно записать файл настроек.", e)
            }
        }
        th.start()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.clear()
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_settings, menu)

        if (!MyApp.haveBugsnagApiKey)
            menu.findItem(R.id.action_bugsnag_consent).setVisible(false)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_reset_user_config -> {
                AlertDialog.Builder(this)
                    .setTitle("Требуется внимание")
                    .setMessage("Вы хотите сбросить пользовательские настройки?")
                    .setPositiveButton("Да") { _, _ ->
                        removeUserConfig()
                        Toast.makeText(this, getString(R.string.user_config_was_reset), Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Нет", null)
                    .show()
                true
            }

            R.id.action_reset_user_resources -> {
                AlertDialog.Builder(this)
                    .setTitle("Требуется внимание")
                    .setMessage("Вы хотите сбросить пользовательские ресурсы?")
                    .setPositiveButton("Да") { _, _ ->
                        removeStaticFiles()
                        removeResourceFiles()
                        Toast.makeText(this, getString(R.string.user_resources_was_reset), Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Нет", null)
                    .show()
                true
            }

            R.id.action_theme_system -> {
                with (prefs.edit()) {
                    putInt(getString(R.string.theme), 0)
                    apply()
                }
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                Toast.makeText(this, "Системная тема", Toast.LENGTH_SHORT).show()
                true
            }

            R.id.action_theme_light -> {
                with (prefs.edit()) {
                    putInt(getString(R.string.theme), 1)
                    apply()
                }
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                Toast.makeText(this, "Светлая тема", Toast.LENGTH_SHORT).show()
                true
            }

            R.id.action_theme_dark -> {
                with (prefs.edit()) {
                    putInt(getString(R.string.theme), 2)
                    apply()
                }
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                Toast.makeText(this, "Темная тема", Toast.LENGTH_SHORT).show()
                true
            }

            R.id.action_generate_navmesh -> {
                Os.setenv("OPENMW_GENERATE_NAVMESH_CACHE", "1", true)
                checkStartGame()
                true
            }

            R.id.action_about -> {
                val text = assets.open("libopenmw/3rdparty-licenses.txt")
                    .bufferedReader()
                    .use { it.readText() }

                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.about_title))
                    .setMessage(text)
                    .show()

                true
            }

            R.id.action_bugsnag_consent -> {
                askBugsnagConsent()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private const val TAG = "OpenMW-Launcher"

        var resolutionX = 0
        var resolutionY = 0
    }

    class CaptureCrash : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(thread: Thread, throwable: Throwable) {
            saveCrashLog(throwable)
            android.os.Process.killProcess(android.os.Process.myPid());
            exitProcess(1)
        }

        private fun saveCrashLog(throwable: Throwable) {
            try {
                val logFile = File(Constants.USER_CONFIG + "/" + "crash.log")
                if (!logFile.exists()) {
                    logFile.createNewFile()
                }

                FileWriter(logFile, true).use { writer ->
                    writer.append("Device: ${Build.MODEL} (API ${Build.VERSION.SDK_INT})\n")
                    writer.append("${getCurrentDateTime()}:\t")
                    printFullStackTrace(throwable, PrintWriter(writer))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun printFullStackTrace(throwable: Throwable, printWriter: PrintWriter) {
            printWriter.println(throwable.toString())
            throwable.stackTrace.forEach { element ->
                printWriter.print("\t $element \n")
            }
            val cause = throwable.cause
            if (cause != null) {
                printWriter.print("Caused by:\t")
                printFullStackTrace(cause, printWriter)
            }
            printWriter.print("\n")
        }

        fun getCurrentDateTime(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }
    }
}
