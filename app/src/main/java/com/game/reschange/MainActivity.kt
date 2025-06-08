package com.game.reschange

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.slider.Slider
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import java.io.DataOutputStream
import java.util.Locale
import androidx.core.content.edit
import androidx.appcompat.widget.SearchView
import androidx.core.view.WindowCompat

class MainActivity : AppCompatActivity() {


    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppListAdapter
    private lateinit var toggleModified: SwitchMaterial

    // Full list of user-installed apps
    private lateinit var allApps: List<AppInfo>

    // Current Filter states
    private var showOnlyModified = false
    private var currentQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.myToolbar))

        recyclerView = findViewById(R.id.appList)
        toggleModified = findViewById(R.id.toggleModified)
        recyclerView.layoutManager = LinearLayoutManager(this)


        allApps = getUserInstalledApps()
        adapter = AppListAdapter(allApps) { appInfo ->
            showResolutionDialog(appInfo.packageName)
        }
        recyclerView.adapter = adapter

        toggleModified.setOnCheckedChangeListener { _, isChecked ->
            showOnlyModified = isChecked
            filterAppList()
        }


        // 🟩 Hook up the "Reset All Resolutions" button
        val resetButton = findViewById<Button>(R.id.resetButton)

        resetButton.setOnClickListener {
            val packages = ScalePrefs.getAllPackages(this)

            for (packageName in packages) {
                val command = "cmd game downscale disable $packageName || cmd game set --downscale disable $packageName"
                runAsRoot(command)
                runAsRoot("am force-stop $packageName") // Kill the app
            }

            // Clear saved preferences
            getSharedPreferences("scale_prefs", Context.MODE_PRIVATE)
                .edit { clear() }

            Toast.makeText(this, "All resolutions reset to default", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Optional: you could hide the keyboard here if you want
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                // 1) update your query string
                currentQuery = newText.orEmpty()
                // 2) re-run the filter
                filterAppList()
                return true
            }
        })
        return true
    }


    private fun filterAppList() {
        var tempList = allApps

        // 1) Filter “modified” if the toggle is on
        if (showOnlyModified) {
            tempList = tempList.filter { app ->
                ScalePrefs.getScale(this, app.packageName) != 1.0f
            }
        }

        // 2) Filter by currentQuery (name or package)
        if (currentQuery.isNotEmpty()) {
            val lowerQ = currentQuery.lowercase(Locale.getDefault())
            tempList = tempList.filter { app ->
                app.name.lowercase(Locale.getDefault()).contains(lowerQ)
                        || app.packageName.lowercase(Locale.getDefault()).contains(lowerQ)
            }
        }

        // 3) Hand the filtered list to the adapter
        adapter.submitList(tempList)
    }



    private fun getUserInstalledApps(): List<AppInfo> {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val launchableApps = pm.queryIntentActivities(intent, 0)
            .mapNotNull { it.activityInfo?.applicationInfo }
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .distinctBy { it.packageName } // avoid duplicates

        return launchableApps.map {
            AppInfo(
                name = pm.getApplicationLabel(it).toString(),
                packageName = it.packageName,
                icon = pm.getApplicationIcon(it)
            )
        }.sortedBy { it.name.lowercase() }
    }

    private fun showResolutionDialog(packageName: String) {


        val savedScale = ScalePrefs.getScale(this, packageName)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val scaleText = TextView(this).apply {
            textSize = 18f
            setPadding(0, 0, 0, 20)
            // Show percentage (preserving 2 decimal precision internally)
            text = "Scale: ${(savedScale * 100).toInt()}%"
        }

        val slider = Slider(this).apply {
            valueFrom = 0.3f
            valueTo = 1.0f
            stepSize = 0.05f  // Allows 0.95, 0.85, etc.
            value = savedScale
            isTickVisible = true
        }

        slider.addOnChangeListener { _, value, _ ->
            val percentage = (value * 100).toInt()
            scaleText.text = "Scale: $percentage%"
        }

        layout.addView(scaleText)
        layout.addView(slider)

        MaterialAlertDialogBuilder(this, R.style.MyRoundedDialog)
            .setTitle("Set Resolution Scale")
            .setView(layout)
            .setPositiveButton("Apply") { _, _ ->
                var scale = String.format(Locale.US, "%.2f", slider.value).toFloat()

                // If user selected 0.95 (95%), round it down to 0.9 and notify
                if (scale == 0.95f) {
                    scale = 0.9f
                    Toast.makeText(
                        this,
                        "95% is not supported. Using 90% instead.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                val percentage = (scale * 100).toInt()

                val command = if (scale == 1.0f) {
                    "cmd game downscale disable $packageName || cmd game set --downscale disable $packageName"
                } else {
                    "cmd game downscale $scale $packageName || cmd game set --downscale $scale $packageName"
                }

                val message = if (scale == 1.0f) {
                    "Resolution reset to default (100%)"
                } else {
                    "Resolution changed to $percentage%"
                }

                ScalePrefs.saveScale(this, packageName, scale)
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "Executing: $command", Toast.LENGTH_SHORT).show()
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                runAsRoot(command)
                runAsRoot("am force-stop $packageName") // Kill the app
                val appName = allApps.find { it.packageName == packageName }?.name ?: packageName
                Toast.makeText(this, "$appName has been force-stopped. Please relaunch it manually.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Reset") { _, _ ->
                val command = "cmd game downscale disable $packageName || cmd game set --downscale disable $packageName"

                ScalePrefs.saveScale(this, packageName, 1.0f)
                Toast.makeText(this, "Resolution reset to default (100%)", Toast.LENGTH_SHORT).show()
                runAsRoot(command)
                runAsRoot("am force-stop $packageName") // Kill the app
                val appName = allApps.find { it.packageName == packageName }?.name ?: packageName
                Toast.makeText(this, "$appName has been force-stopped. Please relaunch it manually.", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun runAsRoot(command: String) {
        try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()
            os.close()
            process.waitFor()
        } catch (e: Exception) {
            Toast.makeText(this, "Root failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
