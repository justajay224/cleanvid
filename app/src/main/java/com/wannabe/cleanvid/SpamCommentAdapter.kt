package com.wannabe.cleanvid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SpamCommentAdapter(private val comments: List<SpamComment>) :
    RecyclerView.Adapter<SpamCommentAdapter.CommentViewHolder>() {

    class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.commentTextView)
        val checkBox: CheckBox = view.findViewById(R.id.commentCheckBox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.textView.text = comment.text
        holder.checkBox.isChecked = comment.isChecked

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            comment.isChecked = isChecked
        }
    }

    override fun getItemCount() = comments.size
}