package com.example.glpimobile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserSearchAdapter(private val onUserSelected: (Map<String, Any>) -> Unit) :
    RecyclerView.Adapter<UserSearchAdapter.UserViewHolder>() {

    private var users: List<Map<String, Any>> = emptyList()

    fun updateList(newList: List<Map<String, Any>>) {
        users = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        val fullName = "${user["34"] ?: ""} ${user["9"] ?: ""}".trim().ifEmpty { user["1"]?.toString() ?: "Utilizador" }
        holder.tvName.text = fullName
        holder.itemView.setOnClickListener { onUserSelected(user) }
    }

    override fun getItemCount() = users.size

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(android.R.id.text1)
    }
}
