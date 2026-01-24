package com.wannabe.cleanvid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ReadOnlySpamAdapter(private val spamComments: List<String>) :
    RecyclerView.Adapter<ReadOnlySpamAdapter.ViewHolder>() {

    // Ubah ViewHolder untuk merujuk ke ID baru
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.spamCommentTextView) // Gunakan ID baru
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflate layout custom kita
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_spam_readonly, parent, false) // Gunakan layout baru
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = spamComments[position]
    }

    override fun getItemCount() = spamComments.size
}