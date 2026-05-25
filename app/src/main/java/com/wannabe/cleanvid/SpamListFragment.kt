package com.wannabe.cleanvid

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.BufferedWriter
import java.io.OutputStreamWriter

class SpamListFragment : Fragment() {

    // VARIABEL UNTUK MENYIMPAN DATA DARI GUDANG MEMORI
    private var spamDataList: List<String> = emptyList()
    private var normalDataList: List<String> = emptyList()
    private var totalComments: Int = 0

    // Launcher untuk membuat file baru (SAF)
    private val createFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                writeCsvToUri(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_spam_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup Toolbar
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        // Setup RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.spamRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // 1. AMBIL DATA DARI DATA HOLDER (Mencegah TransactionTooLargeException)
        spamDataList = CommentDataHolder.spamList
        normalDataList = CommentDataHolder.normalList
        totalComments = CommentDataHolder.totalComments

        // Adapter tetap hanya menampilkan SPAM di layar HP
        recyclerView.adapter = ReadOnlySpamAdapter(spamDataList)

        // Update judul toolbar
        (activity as? AppCompatActivity)?.supportActionBar?.title =
            "${spamDataList.size} Spam Ditemukan dari $totalComments Komentar"

        // Setup Tombol Download CSV
        val downloadButton = view.findViewById<FloatingActionButton>(R.id.downloadCsvButton)
        downloadButton.setOnClickListener {
            if (spamDataList.isNotEmpty() || normalDataList.isNotEmpty()) {
                startCreateFileIntent()
            } else {
                Toast.makeText(requireContext(), "Tidak ada data untuk diunduh.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Membersihkan memori saat pengguna keluar dari halaman (tombol back)
    override fun onDestroyView() {
        super.onDestroyView()
        CommentDataHolder.clearData()
    }

    // 1. Memulai Intent untuk membuat file
    private fun startCreateFileIntent() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
            putExtra(Intent.EXTRA_TITLE, "Laporan_Komentar_CleanVid_${System.currentTimeMillis()}.csv")
        }
        createFileLauncher.launch(intent)
    }

    // 2. Menulis data ke file CSV dengan HEADER INFORMASI
    private fun writeCsvToUri(uri: Uri) {
        try {
            val contentResolver = requireContext().contentResolver
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8)).use { writer ->

                    // 1. Tulis BOM (agar Emoji terbaca di Excel)
                    writer.write("\uFEFF")

                    // ---------------------------------------------------------
                    // 2. TULIS INFORMASI RINGKASAN (METADATA)
                    // ---------------------------------------------------------
                    val totalScan = totalComments // Mengambil dari variabel lokal, bukan args
                    val totalSpam = spamDataList.size
                    val totalNormal = normalDataList.size

                    writer.write("Laporan Scan Komentar CleanVid\n")
                    writer.write("Total Komentar Diperiksa,$totalScan\n")
                    writer.write("Total Spam Terdeteksi,$totalSpam\n")
                    writer.write("Total Komentar Normal,$totalNormal\n")
                    writer.write("\n")
                    // ---------------------------------------------------------

                    // 3. Tulis Header Tabel (TAMBAH KOLOM STATUS)
                    writer.write("Status,Username,Komentar\n")

                    // 4. Looping Data SPAM
                    for (item in spamDataList) {
                        val (username, comment) = parseSpamItem(item)
                        val safeUsername = escapeCsv(username)
                        val safeComment = escapeCsv(comment)

                        // Beri label [SPAM] di awal
                        writer.write("\"[SPAM]\",\"$safeUsername\",\"$safeComment\"\n")
                    }

                    // 5. Looping Data NORMAL
                    for (item in normalDataList) {
                        val (username, comment) = parseSpamItem(item)
                        val safeUsername = escapeCsv(username)
                        val safeComment = escapeCsv(comment)

                        // Beri label [NORMAL] di awal
                        writer.write("\"[NORMAL]\",\"$safeUsername\",\"$safeComment\"\n")
                    }
                }
            }
            Toast.makeText(requireContext(), "Laporan CSV berhasil disimpan!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Gagal menyimpan file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // 3. Helper: Memecah String Gabungan kembali menjadi Username & Komentar
    private fun parseSpamItem(combinedText: String): Pair<String, String> {
        try {
            val cleanText = combinedText.removePrefix("👤 ")
            val delimiterIndex = cleanText.indexOf(":\n")

            if (delimiterIndex != -1) {
                val username = cleanText.substring(0, delimiterIndex)
                val comment = cleanText.substring(delimiterIndex + 2)
                return Pair(username, comment)
            }
        } catch (e: Exception) {
            // Fallback
        }
        return Pair("Unknown", combinedText)
    }

    // 4. Helper: Membersihkan teks untuk format CSV
    private fun escapeCsv(text: String): String {
        return text.replace("\"", "\"\"").replace("\n", " ")
    }
}