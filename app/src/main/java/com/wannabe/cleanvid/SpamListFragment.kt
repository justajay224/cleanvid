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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.BufferedWriter
import java.io.OutputStreamWriter

class SpamListFragment : Fragment() {

    private val args: SpamListFragmentArgs by navArgs()
    private var spamDataList: List<String> = emptyList()

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

        // Ambil data
        spamDataList = args.spamList?.toList() ?: emptyList()
        val totalComments = args.totalComments

        recyclerView.adapter = ReadOnlySpamAdapter(spamDataList)

        // Update judul toolbar
        (activity as? AppCompatActivity)?.supportActionBar?.title =
            "${spamDataList.size} Spam Ditemukan dari $totalComments Komentar"

        // Setup Tombol Download CSV
        val downloadButton = view.findViewById<FloatingActionButton>(R.id.downloadCsvButton)
        downloadButton.setOnClickListener {
            if (spamDataList.isNotEmpty()) {
                startCreateFileIntent()
            } else {
                Toast.makeText(requireContext(), "Tidak ada data untuk diunduh.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 1. Memulai Intent untuk membuat file
    private fun startCreateFileIntent() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
            // Nama file default
            putExtra(Intent.EXTRA_TITLE, "Laporan_Spam_CleanVid_${System.currentTimeMillis()}.csv")
        }
        createFileLauncher.launch(intent)
    }


    // 2. Menulis data ke file CSV dengan HEADER INFORMASI
    private fun writeCsvToUri(uri: Uri) {
        try {
            val contentResolver = requireContext().contentResolver
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                // Gunakan Charsets.UTF_8
                BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8)).use { writer ->

                    // 1. Tulis BOM (agar Emoji terbaca di Excel)
                    writer.write("\uFEFF")

                    // ---------------------------------------------------------
                    // 2. TULIS INFORMASI RINGKASAN (METADATA)
                    // ---------------------------------------------------------
                    // Ambil data dari arguments
                    val totalScan = args.totalComments
                    val totalSpam = spamDataList.size

                    // Baris 1: Judul Laporan
                    writer.write("Laporan Scan Spam CleanVid\n")

                    // Baris 2: Total Diperiksa (Format: Label,Nilai)
                    writer.write("Total Komentar Diperiksa,$totalScan\n")

                    // Baris 3: Total Spam
                    writer.write("Total Spam Ditemukan,$totalSpam\n")

                    // Baris 4: Kosong (Jarak)
                    writer.write("\n")
                    // ---------------------------------------------------------

                    // 3. Tulis Header Tabel
                    writer.write("Username,Komentar\n")

                    // 4. Proses setiap item (Looping Data)
                    for (item in spamDataList) {
                        val (username, comment) = parseSpamItem(item)

                        val safeUsername = escapeCsv(username)
                        val safeComment = escapeCsv(comment)

                        writer.write("\"$safeUsername\",\"$safeComment\"\n")
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
        // Format data kita: "👤 Username:\nKomentar"
        try {
            // Hapus prefix icon "👤 " (2 karakter + spasi)
            val cleanText = combinedText.removePrefix("👤 ")

            // Cari posisi pemisah pertama ":\n"
            val delimiterIndex = cleanText.indexOf(":\n")

            if (delimiterIndex != -1) {
                val username = cleanText.substring(0, delimiterIndex)
                val comment = cleanText.substring(delimiterIndex + 2) // +2 untuk melewati :\n
                return Pair(username, comment)
            }
        } catch (e: Exception) {
            // Fallback jika format error
        }
        return Pair("Unknown", combinedText)
    }

    // 4. Helper: Membersihkan teks untuk format CSV
    private fun escapeCsv(text: String): String {
        // Ganti satu kutip (") menjadi dua kutip ("") agar tidak merusak format CSV
        // Ganti baris baru (\n) menjadi spasi agar tetap satu baris di Excel
        return text.replace("\"", "\"\"").replace("\n", " ")
    }
}