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

package ui.activity

import com.libopenmw.openmw.R

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.tabs.TabLayout
import android.preference.PreferenceManager
import java.io.File
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import file.GameInstaller
import kotlinx.android.synthetic.main.activity_mods.*
import mods.*
import android.view.MenuItem


class ModsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mods)

        setSupportActionBar(findViewById(R.id.mods_toolbar))

        // Enable the "back" icon in the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        updateToolbarSubtitle(0)

        // Switch tabs between plugins/resources
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                flipper.displayedChild = tab.position
                updateToolbarSubtitle(tab.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
            }
        })

        // Set up adapters for the lists
        setupModList(findViewById(R.id.list_mods), ModType.Plugin)
        setupModList(findViewById(R.id.list_resources), ModType.Resource)
        setupModList(findViewById(R.id.list_groundcovers), ModType.Groundcover)
    }


    private fun getAdditionalModDataDirs(): List<String> {
        val modsDir = PreferenceManager.getDefaultSharedPreferences(this)
            .getString("mods_dir", "")!!

        if (modsDir.isBlank()) {
            return emptyList()
        }

        return File(modsDir).listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.absolutePath }
            ?.sorted()
            ?: emptyList()
    }

    private fun updateToolbarSubtitle(tabPosition: Int) {
        val subtitle = when (tabPosition) {
            0 -> getString(R.string.tab_plugins)
            1 -> getString(R.string.tab_resources)
            else -> getString(R.string.tab_groundcover)
        }
        supportActionBar?.subtitle = subtitle
    }

    /**
     * Connects a user-interface RecyclerView to underlying mod data on the disk
     * @param list The list displayed to the user
     * @param type Type of the mods this list will contain
     */
    private fun setupModList(list: RecyclerView, type: ModType) {
        val dataPaths = mutableListOf(GameInstaller.getDataFiles(this))
        dataPaths.addAll(getAdditionalModDataDirs())

        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.orientation = RecyclerView.VERTICAL
        list.layoutManager = linearLayoutManager

        // Set up the adapter using all configured mod directories
        val adapter = ModsAdapter(ModsCollection(type, dataPaths.distinct(), database))

        // Set up the drag-and-drop callback
        val callback = ModMoveCallback(adapter)
        val touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(list)

        adapter.touchHelper = touchHelper

        list.adapter = adapter
    }

    /**
     * Makes the "back" icon in the actionbar perform the back operation
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}
