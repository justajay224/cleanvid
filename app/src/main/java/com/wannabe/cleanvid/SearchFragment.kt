package com.wannabe.cleanvid

//import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
//import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
//import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
//import com.google.android.gms.auth.api.signin.GoogleSignIn
//import com.google.android.gms.auth.api.signin.GoogleSignInAccount
//import com.google.android.gms.common.api.Scope
//import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport // Import baru
import com.google.api.client.googleapis.json.GoogleJsonResponseException
//import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.YouTubeRequestInitializer // Import baru
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Pattern
import androidx.navigation.fragment.findNavController

private const val APP_NAME = "CleanVid YouTube Cleaner"
private const val MY_API_KEY = BuildConfig.YOUTUBE_API_KEY

class SearchFragment : Fragment() {

    // --- Deklarasi Variabel UI ---
    private lateinit var notificationTextView: TextView
    private lateinit var urlEditText: EditText
    private lateinit var pasteButton: Button
//    private lateinit var cleanVideoButton: Button
    private lateinit var checkSpamButton: Button
//    private lateinit var loginPromptTextView: TextView

//    private var foundSpamList: List<SpamComment> = listOf()

    // Launcher untuk ConfirmationActivity
//    private val confirmationLauncher = registerForActivityResult(
//        ActivityResultContracts.StartActivityForResult()
//    ) { result ->
//        if (result.resultCode == Activity.RESULT_OK) {
//            val idsToDelete = result.data?.getStringArrayListExtra("DELETION_LIST")
//            if (idsToDelete != null && idsToDelete.isNotEmpty()) {
//                val commentsToDelete = foundSpamList.filter { idsToDelete.contains(it.id) }
//                deleteSelectedComments(commentsToDelete)
//            } else {
//                notificationTextView.text = "Tidak ada komentar yang dipilih untuk dihapus."
//            }
//        }
//    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        notificationTextView = view.findViewById(R.id.notificationTextView)
        urlEditText = view.findViewById(R.id.urlEditText)
        pasteButton = view.findViewById(R.id.pasteButton)
//        cleanVideoButton = view.findViewById(R.id.cleanVideoButton)
        checkSpamButton = view.findViewById(R.id.checkSpamButton)
//        loginPromptTextView = view.findViewById(R.id.loginPromptTextView) // Hubungkan teks prompt baru

        pasteButton.setOnClickListener { pasteFromClipboard() }

        // Atur listener untuk tombol BARU (Periksa Spam)
        checkSpamButton.setOnClickListener {
            handlePublicScan()
        }

        // Atur listener for tombol LAMA (Bersihkan Video)
//        cleanVideoButton.setOnClickListener {
//            handleCleanUrl()
//        }
    }

    override fun onStart() {
        super.onStart()
        // Cek status login setiap kali fragment ini ditampilkan
//        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
//        updateUI(account)
        // Reset notifikasi saat kembali ke halaman ini
        notificationTextView.text = ""
    }

    /**
     * Mengatur visibilitas tombol berdasarkan status login
     */
//    private fun updateUI(account: GoogleSignInAccount?) {
//        val hasYoutubeScope = account?.grantedScopes?.contains(Scope("https://www.googleapis.com/auth/youtube.force-ssl")) == true
//
//        if (account != null && hasYoutubeScope) {
//            // Pengguna LOGIN dan punya izin
//            cleanVideoButton.visibility = View.VISIBLE
//            loginPromptTextView.visibility = View.GONE
//        } else {
//            // Pengguna LOGOUT atau tidak punya izin
//            cleanVideoButton.visibility = View.GONE
//            loginPromptTextView.visibility = View.VISIBLE
//        }
//        // Tombol "Periksa Spam" selalu terlihat
//        checkSpamButton.visibility = View.VISIBLE
//    }

    private fun pasteFromClipboard() {
        val clipboard = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val textToPaste = clipboard.primaryClip?.getItemAt(0)?.text
        urlEditText.setText(textToPaste)
        // Kosongkan notifikasi saat paste
        notificationTextView.text = ""
    }

    /**
     * Fungsi baru untuk tombol "Periksa Spam" (tanpa login, pakai API Key)
     */
    private fun handlePublicScan() {
        if (MY_API_KEY.isBlank()) {
            Toast.makeText(requireContext(), "Konfigurasi API Key belum selesai.", Toast.LENGTH_SHORT).show()
            return
        }

        val url = urlEditText.text.toString().trim() // Gunakan trim()
        if (url.isBlank()) {
            Toast.makeText(requireContext(), "URL tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }
        val videoId = extractVideoIdFromUrl(url)
        if (videoId == null) {
            Toast.makeText(requireContext(), "URL YouTube tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        startPublicScan(videoId)
    }

    /**
     * Fungsi lama untuk tombol "Bersihkan Video" (perlu login, pakai OAuth)
     */
//    private fun handleCleanUrl() {
//        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
//        if (account == null) {
//            Toast.makeText(requireContext(), "Silakan login di tab Account terlebih dahulu", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        val youtubeScope = Scope("https://www.googleapis.com/auth/youtube.force-ssl")
//        if (!account.grantedScopes.contains(youtubeScope)) {
//            Toast.makeText(requireContext(), "Izin akses YouTube tidak ditemukan. Silakan login ulang.", Toast.LENGTH_LONG).show()
//            return
//        }
//
//        val url = urlEditText.text.toString().trim() // Gunakan trim()
//        if (url.isBlank()) {
//            Toast.makeText(requireContext(), "URL tidak boleh kosong", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        val videoId = extractVideoIdFromUrl(url)
//        if (videoId == null) {
//            Toast.makeText(requireContext(), "URL YouTube tidak valid", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        startCleaningProcess(account, videoId)
//    }

    private fun extractVideoIdFromUrl(url: String): String? {
        val patterns = arrayOf(
            "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|/live/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*",
            "(?<=shorts\\/)[^#\\&\\?\\n]*"
        )

        for (pattern in patterns) {
            val compiledPattern = Pattern.compile(pattern)
            val matcher = compiledPattern.matcher(url)
            if (matcher.find()) {
                return matcher.group()
            }
        }
        return null
    }


    private fun startPublicScan(videoId: String) {
        notificationTextView.text = "Memeriksa komentar spam..." // Teks loading
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Bangun layanan YouTube menggunakan API Key
                val youtube = YouTube.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(), // Transport yang benar untuk API Key
                    JacksonFactory.getDefaultInstance(),
                    null // Tidak perlu credential
                )
                    .setApplicationName(APP_NAME)
                    .setYouTubeRequestInitializer(YouTubeRequestInitializer(MY_API_KEY)) // Set API Key
                    .build()

                // Panggil fungsi findPublicSpamComments (versi Pair)
                val (foundSpamTexts, totalComments) = findPublicSpamComments(youtube, videoId) // Terima Pair

                withContext(Dispatchers.Main) {
                    // Cek dulu apakah proses diinterupsi oleh error "komentar dinonaktifkan"
                    // (Fungsi findPublicSpamComments sudah mengaturnya)
                    if (notificationTextView.text.contains("dinonaktifkan").not()) {
                        if (foundSpamTexts.isNotEmpty()) {
                            // Jika ada spam ditemukan, navigasi ke SpamListFragment
                            notificationTextView.text = "" // Kosongkan notifikasi saat berhasil

                            // Siapkan action navigasi dan kirim data spam + total komentar
                            // Pastikan ID action ini sesuai dengan yang ada di nav_graph.xml
                            val action = SearchFragmentDirections.actionNavigationSearchToSpamListFragment(
                                foundSpamTexts.toTypedArray(), // Kirim list spam sebagai String Array
                                totalComments                 // Kirim total komentar sebagai Int
                            )
                            findNavController().navigate(action)
                        } else {
                            // Jika tidak ada spam
                            notificationTextView.text = "Tidak ada komentar spam ditemukan dari $totalComments komentar."
                            Toast.makeText(requireContext(), "Video ini terlihat bersih!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    // Jika teks notifikasi sudah berisi "dinonaktifkan", tidak perlu melakukan apa-apa lagi.
                }
            } catch (e: Exception) {
                // Penanganan error umum
                e.printStackTrace()
                android.util.Log.e("API_KEY_DEBUG", "Error during public scan: ", e) // Log error lengkap
                if (e is GoogleJsonResponseException) {
                    android.util.Log.e("API_KEY_DEBUG", "Google API Error Details: ${e.details}")
                }
                withContext(Dispatchers.Main) {
                    val friendlyError = ErrorMessageHelper.getFriendlyErrorMessage(e)
                    notificationTextView.text = friendlyError
                }
            }
        }
    }

    /**
     * Proses lama: Memulai pembersihan menggunakan OAuth (perlu login)
     */
//    private fun startCleaningProcess(account: GoogleSignInAccount, videoId: String) {
//        notificationTextView.text = "Memverifikasi kepemilikan video..."
//        lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                val credential = GoogleAccountCredential.usingOAuth2(
//                    requireContext(),
//                    setOf("https://www.googleapis.com/auth/youtube.force-ssl")
//                ).setSelectedAccount(account.account)
//
//                val youtube = YouTube.Builder(
//                    NetHttpTransport(), // Transport yang benar untuk OAuth
//                    JacksonFactory.getDefaultInstance(),
//                    credential
//                ).setApplicationName(APP_NAME).build()
//
//                // Cek kepemilikan video
//                val channelsResponse = youtube.channels().list(listOf("id")).setMine(true).execute()
//                val myChannelId = channelsResponse.items.firstOrNull()?.id
//                if (myChannelId == null) {
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(requireContext(), "Tidak dapat memverifikasi channel Anda.", Toast.LENGTH_LONG).show()
//                        notificationTextView.text = "Verifikasi channel gagal." // Update notifikasi
//                    }
//                    return@launch
//                }
//
//                val videosResponse = youtube.videos().list(listOf("snippet")).setId(listOf(videoId)).execute()
//                val videoOwnerChannelId = videosResponse.items.firstOrNull()?.snippet?.channelId
//                if (videoOwnerChannelId == null) {
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(requireContext(), "Video tidak ditemukan atau tidak valid.", Toast.LENGTH_LONG).show()
//                        notificationTextView.text = "Video tidak ditemukan." // Update notifikasi
//                    }
//                    return@launch
//                }
//
//                if (myChannelId != videoOwnerChannelId) {
//                    withContext(Dispatchers.Main) {
//                        notificationTextView.text = "Aksi dibatalkan."
//                        Toast.makeText(requireContext(), "Error: Anda hanya dapat membersihkan video dari channel Anda sendiri.", Toast.LENGTH_LONG).show()
//                    }
//                    return@launch
//                }
//
//                withContext(Dispatchers.Main) {
//                    notificationTextView.text = "Mencari komentar spam di video..."
//                }
//
//                // Panggil fungsi findSpamComments (versi koleksi)
//                val collectedSpam = findSpamCommentsInVideo(youtube, videoId)
//
//                withContext(Dispatchers.Main) {
//                    if (notificationTextView.text.contains("dinonaktifkan").not()) { // Cek jika tidak ada error 'komentar dinonaktifkan'
//                        if (collectedSpam.isNotEmpty()) {
//                            foundSpamList = collectedSpam
//                            notificationTextView.text = "Menunggu konfirmasi..." // Update notifikasi sebelum pindah
//                            val intent = Intent(requireContext(), ConfirmationActivity::class.java)
//                            intent.putParcelableArrayListExtra("SPAM_COMMENTS", ArrayList(collectedSpam))
//                            confirmationLauncher.launch(intent)
//                        } else {
//                            notificationTextView.text = "Tidak ada komentar spam ditemukan."
//                            Toast.makeText(requireContext(), "Video Anda bersih!", Toast.LENGTH_SHORT).show()
//                        }
//                    }
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//                android.util.Log.e("OAUTH_DEBUG", "Error during cleaning process: ", e) // Log error lengkap
//                if (e is GoogleJsonResponseException) {
//                    android.util.Log.e("OAUTH_DEBUG", "Google API Error Details: ${e.details}")
//                }
//                withContext(Dispatchers.Main) {
//                    val friendlyError = ErrorMessageHelper.getFriendlyErrorMessage(e)
//                    notificationTextView.text = friendlyError
//                }
//            }
//        }
//    }

    /**
     * Fungsi baru: Hanya mencari dan MENGHITUNG spam (untuk API Key)
     */
    private suspend fun findPublicSpamComments(youtube: YouTube, videoId: String): Pair<List<String>, Int> {
        val foundSpamTexts = mutableListOf<String>()
        var totalCommentCount = 0
        try {
            var nextPageToken: String? = null
            do {
                val request = youtube.commentThreads().list(listOf("snippet"))
                    .setVideoId(videoId)
                    .setTextFormat("plainText")
                    .setMaxResults(100)
                if (nextPageToken != null) {
                    request.pageToken = nextPageToken
                }

                val response = request.execute()
                totalCommentCount += response.items?.size ?: 0

                response.items?.forEach { commentThread ->
                    val topComment = commentThread.snippet?.topLevelComment?.snippet
                    val commentText = topComment?.textDisplay
                    // 1. AMBIL USERNAME
                    val username = topComment?.authorDisplayName ?: "Tanpa Nama"

                    // 2. CEK SPAM
                    if (commentText != null && SpamDetector.isSpam(commentText, username)) {
                        // 3. GABUNGKAN USERNAME DAN TEKS
                        // Format: "👤 Username:\nIsi Komentar"
                        val formattedText = "👤 $username:\n$commentText"

                        // 4. MASUKKAN TEKS GABUNGAN KE DAFTAR
                        foundSpamTexts.add(formattedText)
                    }
                }
                nextPageToken = response.nextPageToken

            } while (nextPageToken != null)

        } catch (e: GoogleJsonResponseException) {
            // Tangani error jika komentar dinonaktifkan
            val isCommentsDisabled = e.details?.errors?.any { it.reason == "commentsDisabled" } == true
            if (e.statusCode == 403 && isCommentsDisabled) {
                withContext(Dispatchers.Main) {
                    notificationTextView.text = "Proses dibatalkan: Komentar untuk video ini dinonaktifkan."
                }
            } else {
                throw e
            }
        } catch (e: Exception) {
            throw e
        }
        return Pair(foundSpamTexts, totalCommentCount)
    }

    /**
     * Fungsi lama: Mencari dan MENGUMPULKAN spam (untuk OAuth)
     */
//    private suspend fun findSpamCommentsInVideo(youtube: YouTube, videoId: String): List<SpamComment> {
//        val foundSpam = mutableListOf<SpamComment>()
//        try {
//            val publishedThreads = youtube.commentThreads().list(listOf("snippet"))
//                .setVideoId(videoId)
//                .setTextFormat("plainText")
//                .execute()
//
//            if (publishedThreads.items != null) {
//                for (commentThread in publishedThreads.items) {
//                    val comment = commentThread.snippet.topLevelComment
//                    val commentText = comment.snippet.textDisplay
//                    // Ambil Username (Default "Tanpa Nama" jika null)
//                    val username = comment.snippet.authorDisplayName ?: "Tanpa Nama"
//
//                    // Cek Spam menggunakan Teks DAN Username
//                    if (SpamDetector.isSpam(commentText, username)) {
//                        // Masukkan ke daftar spam dengan format gabungan
//                        foundSpam.add(
//                            SpamComment(
//                                id = comment.id,
//                                // GABUNGKAN USERNAME KE SINI AGAR TAMPIL DI LAYAR
//                                text = "👤 $username:\n$commentText",
//                                authorChannelId = comment.snippet.authorChannelId.value
//                            )
//                        )
//                    }
//                }
//            }
//
//            // =================================================================
//            // 2. PERIKSA KOMENTAR DITAHAN (HELD FOR REVIEW)
//            // =================================================================
//            val heldThreads = youtube.commentThreads().list(listOf("snippet"))
//                .setVideoId(videoId)
//                .setModerationStatus("heldForReview")
//                .setTextFormat("plainText")
//                .execute()
//
//            if (heldThreads.items != null) {
//                for (commentThread in heldThreads.items) {
//                    val comment = commentThread.snippet.topLevelComment
//                    val commentText = comment.snippet.textDisplay
//                    val username = comment.snippet.authorDisplayName ?: "Tanpa Nama"
//
//                    if (SpamDetector.isSpam(commentText, username)) {
//                        foundSpam.add(
//                            SpamComment(
//                                id = comment.id,
//                                // GABUNGKAN USERNAME KE SINI JUGA
//                                text = "👤 $username:\n$commentText",
//                                authorChannelId = comment.snippet.authorChannelId.value
//                            )
//                        )
//                    }
//                }
//            }
//
//        } catch (e: GoogleJsonResponseException) {
//            // Penanganan khusus jika komentar dinonaktifkan
//            val isCommentsDisabled = e.details?.errors?.any { it.reason == "commentsDisabled" } == true
//            if (e.statusCode == 403 && isCommentsDisabled) {
//                withContext(Dispatchers.Main) {
//                    notificationTextView.text = "Proses dibatalkan: Komentar untuk video ini dinonaktifkan."
//                }
//            } else {
//                // Jika error lain, lempar agar ditangani fungsi pemanggil
//                throw e
//            }
//        } catch (e: Exception) {
//            throw e
//        }
//        return foundSpam
//    }

    /**
     * Fungsi lama: Menghapus komentar (tetap sama)
     */
//    private fun deleteSelectedComments(commentsToDelete: List<SpamComment>) {
//        val account = GoogleSignIn.getLastSignedInAccount(requireContext()) ?: return
//
//        notificationTextView.text = "Menghapus ${commentsToDelete.size} komentar..."
//        lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                val credential = GoogleAccountCredential.usingOAuth2(
//                    requireContext(),
//                    setOf("https://www.googleapis.com/auth/youtube.force-ssl")
//                ).setSelectedAccount(account.account)
//                val youtube = YouTube.Builder(
//                    NetHttpTransport(),
//                    JacksonFactory.getDefaultInstance(),
//                    credential
//                ).setApplicationName(APP_NAME).build()
//
//                val channelsResponse = youtube.channels().list(listOf("snippet")).setMine(true).execute()
//                val myChannelId = channelsResponse.items.firstOrNull()?.id
//
//                for (comment in commentsToDelete) {
//                    if (comment.authorChannelId == myChannelId) {
//                        youtube.comments().delete(comment.id).execute()
//                        println("Menghapus (delete) komentar sendiri: ${comment.text}")
//                    } else {
//                        youtube.comments().setModerationStatus(listOf(comment.id), "rejected").execute()
//                        println("Menolak (reject) komentar orang lain: ${comment.text}")
//                    }
//                }
//
//                withContext(Dispatchers.Main) {
//                    notificationTextView.text = "${commentsToDelete.size} komentar berhasil diproses."
//                    Toast.makeText(requireContext(), "Pembersihan Selesai!", Toast.LENGTH_LONG).show()
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//                android.util.Log.e("DELETE_DEBUG", "Error during deletion: ", e) // Log error lengkap
//                if (e is GoogleJsonResponseException) {
//                    android.util.Log.e("DELETE_DEBUG", "Google API Error Details: ${e.details}")
//                }
//                withContext(Dispatchers.Main) {
//                    // Gunakan ErrorMessageHelper untuk pesan yang lebih ramah
//                    val friendlyError = ErrorMessageHelper.getFriendlyErrorMessage(e)
//                    notificationTextView.text = "Error saat menghapus: $friendlyError"
//                }
//            }
//        }
//    }
}