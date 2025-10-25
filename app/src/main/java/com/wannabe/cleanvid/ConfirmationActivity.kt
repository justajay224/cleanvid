package com.wannabe.cleanvid

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ConfirmationActivity : AppCompatActivity() {

    private lateinit var comments: ArrayList<SpamComment>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmation)

        // Ambil daftar komentar dari MainActivity
        comments = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("SPAM_COMMENTS", SpamComment::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra("SPAM_COMMENTS")
        } ?: arrayListOf()

        // ===============================================
        //           PENAMBAHAN KODE DI SINI
        // ===============================================
        val titleTextView: TextView = findViewById(R.id.titleTextView)
        val spamCount = comments.size
        titleTextView.text = "$spamCount Komentar Spam Ditemukan"
        // ===============================================

        // Setup RecyclerView (kode ini sudah ada)
        val recyclerView: RecyclerView = findViewById(R.id.commentsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = SpamCommentAdapter(comments)

        // Setup Tombol Hapus (kode ini sudah ada)
        val deleteButton: Button = findViewById(R.id.deleteButton)
        deleteButton.setOnClickListener {
            // Filter hanya komentar yang masih ter-ceklis
            val commentsToDelete = comments.filter { it.isChecked }
            val commentIdsToDelete = ArrayList(commentsToDelete.map { it.id })

            // Kirim kembali daftar ID yang akan dihapus ke MainActivity
            val resultIntent = Intent()
            resultIntent.putStringArrayListExtra("DELETION_LIST", commentIdsToDelete)
            setResult(Activity.RESULT_OK, resultIntent)
            finish() // Tutup layar ini
        }
    }
}