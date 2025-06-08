package com.game.reschange

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppListAdapter(
    private var apps: List<AppInfo>,
    private val onClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    inner class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.appIcon)
        val name: TextView = view.findViewById(R.id.appName)
        val packageName: TextView = itemView.findViewById(R.id.packageName)
        val modifiedIcon: ImageView = itemView.findViewById(R.id.modifiedIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    fun submitList(newList: List<AppInfo>) {
        apps = newList
        notifyDataSetChanged()
    }


    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.icon.setImageDrawable(app.icon)
        holder.name.text = app.name
        holder.packageName.text = app.packageName

        // Show gear icon if app's scale is modified
        val savedScale = ScalePrefs.getScale(holder.itemView.context, app.packageName)
        holder.modifiedIcon.visibility = if (savedScale != 1.0f) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener { onClick(app) }
    }

    override fun getItemCount(): Int = apps.size
}
