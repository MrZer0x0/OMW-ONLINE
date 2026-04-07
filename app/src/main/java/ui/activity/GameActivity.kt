/*
    Copyright (C) 2015-2017 sandstranger
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

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Process
import android.preference.PreferenceManager
import android.system.ErrnoException
import android.system.Os
import android.util.Log
import android.view.WindowManager
import android.widget.RelativeLayout
import com.libopenmw.openmw.R

import org.libsdl.app.SDLActivity

import constants.Constants
import cursor.MouseCursor
import parser.CommandlineParser
import ui.controls.Osc

import utils.Utils.hideAndroidControls

import android.util.DisplayMetrics
import android.os.AsyncTask
import android.widget.ImageView
import android.widget.TextView
import android.graphics.Typeface
import android.graphics.Rect

import java.io.File

enum class MouseMode {
    Hybrid,
    Joystick,
    Touch;

    companion object {
        fun get(s: String): MouseMode {
            return when (s) {
                "joystick" -> Joystick
                "touch" -> Touch
                else -> Hybrid
            }
        }
    }
}

private fun patchShaders() {
    fun patch(path: String, transform: (String) -> String) {
        val f = File(path)
        if (!f.exists()) return
        var content = f.readText()
        if (content.contains("#pragma CONVERTED")) return
        f.writeText(transform(content) + "\n#pragma CONVERTED\n")
    }

    val base = Constants.USER_FILE_STORAGE + "/resources/shaders/compatibility/"
    val lib  = Constants.USER_FILE_STORAGE + "/resources/shaders/lib/light/"

    patch(base + "groundcover.frag") { c -> c
        .replace("#define GROUNDCOVER", "#define GROUNDCOVER\n#pragma import_defines(WRITE_NORMALS, CLASSIC_FALLOFF, MAX_LIGHTS)\n")
        .replace("#if !@disableNormals", "#if defined(WRITE_NORMALS) && WRITE_NORMALS") }
    patch(base + "groundcover.vert") { c -> c
        .replace("#version 120\n", "#version 120\n#pragma import_defines(CLASSIC_FALLOFF, MAX_LIGHTS)\n") }
    patch(base + "objects.frag") { c -> c
        .replace("(FORCE_OPAQUE, DISTORTION)", "(FORCE_OPAQUE, DISTORTION, FORCE_PPL, WRITE_NORMALS, CLASSIC_FALLOFF, MAX_LIGHTS)")
        .replace("#define PER_PIXEL_LIGHTING (@normalMap || @specularMap || @forcePPL)", "#if defined(FORCE_PPL)\n#define PER_PIXEL_LIGHTING (@normalMap || @specularMap || FORCE_PPL)\n#else\n#define PER_PIXEL_LIGHTING (@normalMap || @specularMap || @forcePPL)\n#endif")
        .replace("#if !defined(FORCE_OPAQUE) && !@disableNormals", "#if !defined(FORCE_OPAQUE) && defined(WRITE_NORMALS) && WRITE_NORMALS") }
    patch(base + "objects.vert") { c -> c
        .replace("#version 120", "#version 120\n#pragma import_defines(FORCE_PPL, CLASSIC_FALLOFF, MAX_LIGHTS)\n")
        .replace("#define PER_PIXEL_LIGHTING (@normalMap || @specularMap || @forcePPL)", "#if defined(FORCE_PPL)\n#define PER_PIXEL_LIGHTING (@normalMap || @specularMap || FORCE_PPL)\n#else\n#define PER_PIXEL_LIGHTING (@normalMap || @specularMap || @forcePPL)\n#endif") }
    patch(base + "terrain.frag") { c -> c
        .replace("#version 120", "#version 120\n#pragma import_defines(WRITE_NORMALS, FORCE_PPL, CLASSIC_FALLOFF, MAX_LIGHTS)\n")
        .replace("#define PER_PIXEL_LIGHTING (@normalMap || @specularMap || @forcePPL)", "#if defined(FORCE_PPL)\n#define PER_PIXEL_LIGHTING (@normalMap || @specularMap || FORCE_PPL)\n#else\n#define PER_PIXEL_LIGHTING (@normalMap || @specularMap || @forcePPL)\n#endif")
        .replace("#if !@disableNormals && @writeNormals", "#if defined(WRITE_NORMALS) && WRITE_NORMALS && @writeNormals") }
    patch(base + "terrain.vert") { c -> c
        .replace("#version 120", "#version 120\n#pragma import_defines(FORCE_PPL, CLASSIC_FALLOFF, MAX_LIGHTS)\n")
        .replace("#define PER_PIXEL_LIGHTING (@normalMap || @specularMap || @forcePPL)", "#if defined(FORCE_PPL)\n#define PER_PIXEL_LIGHTING (@normalMap || @specularMap || FORCE_PPL)\n#else\n#define PER_PIXEL_LIGHTING (@normalMap || @specularMap || @forcePPL)\n#endif") }
    patch(base + "water.frag") { c -> c
        .replace("#version 120", "#version 120\n#pragma import_defines(WRITE_NORMALS, CLASSIC_FALLOFF, MAX_LIGHTS)\n")
        .replace("#if !@disableNormals", "#if defined(WRITE_NORMALS) && WRITE_NORMALS") }
    patch(lib + "lighting.glsl") { c -> c
        .replace("#if !@classicFalloff && !@lightingMethodFFP", "#if defined(CLASSIC_FALLOFF) && !CLASSIC_FALLOFF && !@lightingMethodFFP") }
    patch(lib + "lighting_util.glsl") { c -> c
        .replace("#define LIB_LIGHTING_UTIL", "#define LIB_LIGHTING_UTIL\n#pragma import_defines(CLAMP_LIGHTING)")
        .replace("#if @clamp", "#if defined(CLAMP_LIGHTING) && CLAMP_LIGHTING")
        .replace("uniform int PointLightIndex[@maxLights];", "#if defined(MAX_LIGHTS)\nuniform int PointLightIndex[MAX_LIGHTS];\n#else\nuniform int PointLightIndex[@maxLights];\n#endif")
        .replace("uniform mat4 LightBuffer[@maxLights];", "#if defined(MAX_LIGHTS)\nuniform mat4 LightBuffer[MAX_LIGHTS];\n#else\nuniform mat4 LightBuffer[@maxLights];\n#endif")
        .replace("#if !@classicFalloff && !@lightingMethodFFP", "#if defined(CLASSIC_FALLOFF) && !CLASSIC_FALLOFF && !@lightingMethodFFP") }
}

class GameActivity : SDLActivity() {

    private var prefs: SharedPreferences? = null

    val layout: RelativeLayout
        get() = SDLActivity.mLayout as RelativeLayout

    override fun loadLibraries() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        val physicsFPS = prefs!!.getString("pref_physicsFPS2", "")
        if (!physicsFPS.isNullOrEmpty()) {
            try { Os.setenv("OPENMW_PHYSICS_FPS", physicsFPS, true) }
            catch (e: ErrnoException) { Log.e("OpenMW", "Failed setting OPENMW_PHYSICS_FPS") }
        }

        System.loadLibrary("c++_shared")
        System.loadLibrary("openal")
        System.loadLibrary("SDL2")

        // NG-GL4ES: GLES 3.2 mode
        try {
            Os.setenv("OPENMW_GLES_VERSION", "32", true)
            Os.setenv("LIBGL_ES", "3", true)
        } catch (e: ErrnoException) { Log.e("OpenMW", "Failed setting GLES vars") }

        // NG-GL4ES performance flags
        Os.setenv("OSG_VERTEX_BUFFER_HINT", "VBO", true)
        Os.setenv("OSG_GL_TEXTURE_STORAGE", "OFF", true)
        Os.setenv("OSG_TEXT_SHADER_TECHNIQUE", "ALL", true)
        Os.setenv("LIBGL_SIMPLE_SHADERCONV", "1", true)
        Os.setenv("LIBGL_INSTANCING", "1", true)
        Os.setenv("LIBGL_DXTMIPMAP", "1", true)

        // TES3MP-specific settings
        val omwDebugLevel = prefs!!.getString("pref_debug_level", "")
        if (!omwDebugLevel.isNullOrEmpty()) Os.setenv("OPENMW_DEBUG_LEVEL", omwDebugLevel, true)

        val omwMyGui = prefs!!.getString("pref_mygui", "")
        if (omwMyGui == "preset_01") Os.setenv("OPENMW_MYGUI", "preset_01", true)

        val omwWaterPreset = prefs!!.getString("pref_water_preset", "")
        if (omwWaterPreset == "1") {
            Os.setenv("OPENMW_WATER_VERTEX", "water_vertex.glsl", true)
            Os.setenv("OPENMW_WATER_FRAGMENT", "water_fragment.glsl", true)
        } else {
            Os.setenv("OPENMW_WATER_VERTEX", "water_vertex2.glsl", true)
            Os.setenv("OPENMW_WATER_FRAGMENT", "water_fragment2.glsl", true)
        }

        val omwVfsSl = prefs!!.getString("pref_vfs_selector", "")
        if (omwVfsSl == "1") Os.setenv("OPENMW_VFS_SELECTOR", "vfs", true)
        if (omwVfsSl == "2") Os.setenv("OPENMW_VFS_SELECTOR", "vfs2", true)

        // User-defined env vars (space/newline separated KEY=VALUE pairs)
        val envline = prefs!!.getString("envLine", "") ?: ""
        if (envline.isNotEmpty()) {
            envline.split(" ", "\n").forEach { token ->
                val parts = token.split("=")
                if (parts.size == 2) Os.setenv(parts[0], parts[1], true)
            }
        }

        patchShaders()

        // Load NG-GL4ES (replaces legacy gl4es libGL.so)
        System.loadLibrary("ng_gl4es")
        System.loadLibrary("openmw")
    }

    override fun getMainSharedObject(): String = "libopenmw.so"

    private fun showProgressBar() {
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(dm)

        val bg = ImageView(layout.context).apply {
            setImageResource(R.drawable.progressbarbackground)
            scaleType = ImageView.ScaleType.FIT_XY
            x = ((dm.widthPixels / 2) - 405).toFloat()
            y = ((dm.heightPixels / 2) - 105).toFloat()
        }
        layout.addView(bg)
        bg.layoutParams.width = 810; bg.layoutParams.height = 60

        val bar = ImageView(layout.context).apply {
            setImageResource(R.drawable.progressbar)
            scaleType = ImageView.ScaleType.FIT_XY
            x = ((dm.widthPixels / 2) - 400).toFloat()
            y = ((dm.heightPixels / 2) - 100).toFloat()
        }
        layout.addView(bar)
        bar.layoutParams.width = 0; bar.layoutParams.height = 50

        val msg = "GENERATING NAVMESH CACHE"
        val title = TextView(this).apply {
            text = msg
            val b = Rect(); paint.getTextBounds(msg, 0, msg.length, b)
            x = ((dm.widthPixels / 2) - (b.width() / 2)).toFloat()
            y = ((dm.heightPixels / 2) - 200).toFloat()
            setTypeface(null, Typeface.BOLD)
        }
        layout.addView(title)

        val pct = TextView(this).apply {
            x = (dm.widthPixels / 2).toFloat()
            y = ((dm.heightPixels / 2) + 50).toFloat()
        }
        layout.addView(pct)

        Os.setenv("NAVMESHTOOL_MESSAGE", "0.0", true)
        ProgressBarUpdater(pct, bar, dm.widthPixels, dm.heightPixels).execute()
    }

    class ProgressBarUpdater(
        private val pct: TextView, private val bar: ImageView,
        private val sw: Int, private val sh: Int
    ) : AsyncTask<Void, String, String>() {
        override fun doInBackground(vararg p: Void?): String {
            while (Os.getenv("NAVMESHTOOL_MESSAGE") != "Done") {
                publishProgress(Os.getenv("NAVMESHTOOL_MESSAGE")); Thread.sleep(50)
            }
            return "DONE"
        }
        override fun onProgressUpdate(vararg progress: String?) {
            bar.requestLayout(); bar.layoutParams.width = (8.0 * progress[0]!!.toFloat()).toInt()
            val b = Rect(); pct.paint.getTextBounds(progress[0]!!, 0, progress[0]!!.length, b)
            pct.x = ((sw / 2) - (b.width() / 2)).toFloat(); pct.text = progress[0]
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cutout = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean("pref_display_cutout_area", true)
        if (cutout || android.os.Build.VERSION.SDK_INT < 29)
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

        KeepScreenOn()
        getPathToJni(filesDir.parent, Constants.USER_FILE_STORAGE)

        if (Os.getenv("OPENMW_GENERATE_NAVMESH_CACHE") == "1") showProgressBar()
        else showControls()
    }

    private fun showControls() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        mouseMode = MouseMode.get(prefs.getString("pref_mouse_mode",
            getString(R.string.pref_mouse_mode_default))!!)

        val pref_hide_controls = prefs.getBoolean(Constants.HIDE_CONTROLS, false)
        var osc: Osc? = null
        if (!pref_hide_controls) {
            osc = Osc()
            osc.placeElements(layout)
        }
        MouseCursor(this, osc)
    }

    private fun KeepScreenOn() {
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_screen_keeper", false))
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    public override fun onDestroy() {
        finish(); Process.killProcess(Process.myPid()); super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus) hideAndroidControls(this)
    }

    override fun getArguments(): Array<String> {
        val cmd = PreferenceManager.getDefaultSharedPreferences(this).getString("commandLine", "")
        return CommandlineParser("--resources " + Constants.USER_FILE_STORAGE + "/resources " + cmd!!).argv
    }

    private external fun getPathToJni(path_global: String, path_user: String)

    companion object {
        var mouseMode = MouseMode.Hybrid
    }
}
