package com.wannabe.cleanvid

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.YouTubeRequestInitializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

private const val APP_NAME = "CleanVid YouTube Cleaner"
private const val MY_API_KEY = BuildConfig.YOUTUBE_API_KEY

class SearchFragment : Fragment() {

    // --- Deklarasi Variabel UI ---
    private lateinit var notificationTextView: TextView
    private lateinit var urlEditText: EditText
    private lateinit var pasteButton: Button
    private lateinit var cleanVideoButton: Button
    private lateinit var checkSpamButton: Button
    private lateinit var loginPromptTextView: TextView

    private var foundSpamList: List<SpamComment> = listOf()

    // Launcher untuk ConfirmationActivity (Khusus untuk "Bersihkan Video")
    private val confirmationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val idsToDelete = result.data?.getStringArrayListExtra("DELETION_LIST")
            if (idsToDelete != null && idsToDelete.isNotEmpty()) {
                val commentsToDelete = foundSpamList.filter { idsToDelete.contains(it.id) }
                deleteSelectedComments(commentsToDelete)
            } else {
                notificationTextView.text = "Tidak ada komentar yang dipilih untuk dihapus."
            }
        }
    }

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
        cleanVideoButton = view.findViewById(R.id.cleanVideoButton)
        checkSpamButton = view.findViewById(R.id.checkSpamButton)
        loginPromptTextView = view.findViewById(R.id.loginPromptTextView)

        pasteButton.setOnClickListener { pasteFromClipboard() }

        // Atur listener untuk tombol BARU (Periksa Spam)
        checkSpamButton.setOnClickListener {
            handlePublicScan()
        }

        // Atur listener for tombol LAMA (Bersihkan Video)
        cleanVideoButton.setOnClickListener {
            handleCleanUrl()
        }
    }

    override fun onStart() {
        super.onStart()
        // Cek status login setiap kali fragment ini ditampilkan
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        updateUI(account)
        // Reset notifikasi saat kembali ke halaman ini
        notificationTextView.text = ""
    }

    /**
     * Mengatur visibilitas tombol berdasarkan status login
     */
    private fun updateUI(account: GoogleSignInAccount?) {
        val hasYoutubeScope = account?.grantedScopes?.contains(Scope("https://www.googleapis.com/auth/youtube.force-ssl")) == true

        if (account != null && hasYoutubeScope) {
            // Pengguna LOGIN dan punya izin
            cleanVideoButton.visibility = View.VISIBLE
            loginPromptTextView.visibility = View.GONE
        } else {
            // Pengguna LOGOUT atau tidak punya izin
            cleanVideoButton.visibility = View.GONE
            loginPromptTextView.visibility = View.VISIBLE
        }
        // Tombol "Periksa Spam" selalu terlihat
        checkSpamButton.visibility = View.VISIBLE
    }

    private fun pasteFromClipboard() {
        val clipboard = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val textToPaste = clipboard.primaryClip?.getItemAt(0)?.text
        urlEditText.setText(textToPaste)
        // Kosongkan notifikasi saat paste
        notificationTextView.text = ""
    }

    /**
     * Fungsi untuk ekstrak ID Video YouTube dari berbagai format URL
     */
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

    // =========================================================================
    // BAGIAN 1: LOGIKA UNTUK TOMBOL "PERIKSA SPAM" (MENGGUNAKAN API KEY)
    // =========================================================================

    private fun handlePublicScan() {
        if (MY_API_KEY.isBlank()) {
            Toast.makeText(requireContext(), "Konfigurasi API Key belum selesai.", Toast.LENGTH_SHORT).show()
            return
        }

        val url = urlEditText.text.toString().trim()
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

    private fun startPublicScan(videoId: String) {
        notificationTextView.text = "Memeriksa komentar spam..."
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Bangun layanan YouTube menggunakan API Key
                val youtube = YouTube.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance(),
                    null
                )
                    .setApplicationName(APP_NAME)
                    .setYouTubeRequestInitializer(YouTubeRequestInitializer(MY_API_KEY))
                    .build()

                // 2. Ambil 3 variabel (Spam, Normal, Total)
                val (foundSpamTexts, foundNormalTexts, totalComments) = findPublicSpamComments(youtube, videoId)

                withContext(Dispatchers.Main) {
                    // 3. Cek apakah proses diinterupsi error komentar dinonaktifkan
                    if (!notificationTextView.text.contains("dinonaktifkan")) {

                        // 4. Jika ada komentar pada video tersebut (baik spam/normal)
                        if (foundSpamTexts.isNotEmpty() || foundNormalTexts.isNotEmpty()) {
                            notificationTextView.text = ""

                            // SIMPAN DATA KE DALAM DATA HOLDER
                            CommentDataHolder.spamList = foundSpamTexts
                            CommentDataHolder.normalList = foundNormalTexts
                            CommentDataHolder.totalComments = totalComments

                            // NAVIGASI TANPA ARGUMEN (Menghindari TransactionTooLargeException)
                            val action = SearchFragmentDirections.actionNavigationSearchToSpamListFragment()
                            findNavController().navigate(action)

                            // Jika video bersih tapi tetap bisa unduh CSV normal
                            if (foundSpamTexts.isEmpty()) {
                                Toast.makeText(requireContext(), "Video bersih! Anda tetap bisa mengunduh laporan CSV.", Toast.LENGTH_LONG).show()
                            }

                        } else {
                            // Jika video 0 komentar
                            notificationTextView.text = "Tidak ada komentar ditemukan pada video ini."
                            Toast.makeText(requireContext(), "Video ini belum memiliki komentar!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("API_KEY_DEBUG", "Error during public scan: ", e)
                withContext(Dispatchers.Main) {
                    val friendlyError = ErrorMessageHelper.getFriendlyErrorMessage(e)
                    notificationTextView.text = friendlyError
                }
            }
        }
    }

    private suspend fun findPublicSpamComments(youtube: YouTube, videoId: String): Triple<List<String>, List<String>, Int> {
        val foundSpamTexts = mutableListOf<String>()
        val foundNormalTexts = mutableListOf<String>()
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
                    val username = topComment?.authorDisplayName ?: "Tanpa Nama"

                    if (commentText != null) {
                        val formattedText = "👤 $username:\n$commentText"
                        if (SpamDetector.isSpam(commentText, username)) {
                            foundSpamTexts.add(formattedText)
                        } else {
                            foundNormalTexts.add(formattedText)
                        }
                    }
                }
                nextPageToken = response.nextPageToken

            } while (nextPageToken != null)

        } catch (e: GoogleJsonResponseException) {
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

        return Triple(foundSpamTexts, foundNormalTexts, totalCommentCount)
    }

    // =========================================================================
    // BAGIAN 2: LOGIKA UNTUK TOMBOL "BERSIHKAN VIDEO" (MENGGUNAKAN OAUTH)
    // =========================================================================

    private fun handleCleanUrl() {
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        if (account == null) {
            Toast.makeText(requireContext(), "Silakan login di tab Account terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        val youtubeScope = Scope("https://www.googleapis.com/auth/youtube.force-ssl")
        if (!account.grantedScopes.contains(youtubeScope)) {
            Toast.makeText(requireContext(), "Izin akses YouTube tidak ditemukan. Silakan login ulang.", Toast.LENGTH_LONG).show()
            return
        }

        val url = urlEditText.text.toString().trim()
        if (url.isBlank()) {
            Toast.makeText(requireContext(), "URL tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        val videoId = extractVideoIdFromUrl(url)
        if (videoId == null) {
            Toast.makeText(requireContext(), "URL YouTube tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        startCleaningProcess(account, videoId)
    }

    private fun startCleaningProcess(account: GoogleSignInAccount, videoId: String) {
        notificationTextView.text = "Memverifikasi kepemilikan video..."
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val credential = GoogleAccountCredential.usingOAuth2(
                    requireContext(),
                    setOf("https://www.googleapis.com/auth/youtube.force-ssl")
                ).setSelectedAccount(account.account)

                val youtube = YouTube.Builder(
                    NetHttpTransport(),
                    JacksonFactory.getDefaultInstance(),
                    credential
                ).setApplicationName(APP_NAME).build()

                // Cek kepemilikan video
                val channelsResponse = youtube.channels().list(listOf("id")).setMine(true).execute()
                val myChannelId = channelsResponse.items.firstOrNull()?.id
                if (myChannelId == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Tidak dapat memverifikasi channel Anda.", Toast.LENGTH_LONG).show()
                        notificationTextView.text = "Verifikasi channel gagal."
                    }
                    return@launch
                }

                val videosResponse = youtube.videos().list(listOf("snippet")).setId(listOf(videoId)).execute()
                val videoOwnerChannelId = videosResponse.items.firstOrNull()?.snippet?.channelId
                if (videoOwnerChannelId == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Video tidak ditemukan atau tidak valid.", Toast.LENGTH_LONG).show()
                        notificationTextView.text = "Video tidak ditemukan."
                    }
                    return@launch
                }

                if (myChannelId != videoOwnerChannelId) {
                    withContext(Dispatchers.Main) {
                        notificationTextView.text = "Aksi dibatalkan."
                        Toast.makeText(requireContext(), "Error: Anda hanya dapat membersihkan video dari channel Anda sendiri.", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    notificationTextView.text = "Mencari komentar spam di video..."
                }

                // Panggil fungsi pencarian untuk eksekusi
                val collectedSpam = findSpamCommentsInVideo(youtube, videoId)

                withContext(Dispatchers.Main) {
                    if (!notificationTextView.text.contains("dinonaktifkan")) {
                        if (collectedSpam.isNotEmpty()) {
                            foundSpamList = collectedSpam
                            notificationTextView.text = "Menunggu konfirmasi..."
                            val intent = Intent(requireContext(), ConfirmationActivity::class.java)
                            intent.putParcelableArrayListExtra("SPAM_COMMENTS", ArrayList(collectedSpam))
                            confirmationLauncher.launch(intent)
                        } else {
                            notificationTextView.text = "Tidak ada komentar spam ditemukan."
                            Toast.makeText(requireContext(), "Video Anda bersih!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("OAUTH_DEBUG", "Error during cleaning process: ", e)
                withContext(Dispatchers.Main) {
                    val friendlyError = ErrorMessageHelper.getFriendlyErrorMessage(e)
                    notificationTextView.text = friendlyError
                }
            }
        }
    }

    private suspend fun findSpamCommentsInVideo(youtube: YouTube, videoId: String): List<SpamComment> {
        val foundSpam = mutableListOf<SpamComment>()
        try {
            val publishedThreads = youtube.commentThreads().list(listOf("snippet"))
                .setVideoId(videoId)
                .setTextFormat("plainText")
                .execute()

            if (publishedThreads.items != null) {
                for (commentThread in publishedThreads.items) {
                    val comment = commentThread.snippet.topLevelComment
                    val commentText = comment.snippet.textDisplay
                    val username = comment.snippet.authorDisplayName ?: "Tanpa Nama"

                    if (SpamDetector.isSpam(commentText, username)) {
                        foundSpam.add(
                            SpamComment(
                                id = comment.id,
                                text = "👤 $username:\n$commentText",
                                authorChannelId = comment.snippet.authorChannelId.value
                            )
                        )
                    }
                }
            }

            // PERIKSA KOMENTAR DITAHAN (HELD FOR REVIEW)
            val heldThreads = youtube.commentThreads().list(listOf("snippet"))
                .setVideoId(videoId)
                .setModerationStatus("heldForReview")
                .setTextFormat("plainText")
                .execute()

            if (heldThreads.items != null) {
                for (commentThread in heldThreads.items) {
                    val comment = commentThread.snippet.topLevelComment
                    val commentText = comment.snippet.textDisplay
                    val username = comment.snippet.authorDisplayName ?: "Tanpa Nama"

                    if (SpamDetector.isSpam(commentText, username)) {
                        foundSpam.add(
                            SpamComment(
                                id = comment.id,
                                text = "👤 $username:\n$commentText",
                                authorChannelId = comment.snippet.authorChannelId.value
                            )
                        )
                    }
                }
            }

        } catch (e: GoogleJsonResponseException) {
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
        return foundSpam
    }

    private fun deleteSelectedComments(commentsToDelete: List<SpamComment>) {
        val account = GoogleSignIn.getLastSignedInAccount(requireContext()) ?: return

        notificationTextView.text = "Menghapus ${commentsToDelete.size} komentar..."
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val credential = GoogleAccountCredential.usingOAuth2(
                    requireContext(),
                    setOf("https://www.googleapis.com/auth/youtube.force-ssl")
                ).setSelectedAccount(account.account)
                val youtube = YouTube.Builder(
                    NetHttpTransport(),
                    JacksonFactory.getDefaultInstance(),
                    credential
                ).setApplicationName(APP_NAME).build()

                val channelsResponse = youtube.channels().list(listOf("snippet")).setMine(true).execute()
                val myChannelId = channelsResponse.items.firstOrNull()?.id

                for (comment in commentsToDelete) {
                    if (comment.authorChannelId == myChannelId) {
                        youtube.comments().delete(comment.id).execute()
                    } else {
                        youtube.comments().setModerationStatus(listOf(comment.id), "rejected").execute()
                    }
                }

                withContext(Dispatchers.Main) {
                    notificationTextView.text = "${commentsToDelete.size} komentar berhasil diproses."
                    Toast.makeText(requireContext(), "Pembersihan Selesai!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("DELETE_DEBUG", "Error during deletion: ", e)
                withContext(Dispatchers.Main) {
                    val friendlyError = ErrorMessageHelper.getFriendlyErrorMessage(e)
                    notificationTextView.text = "Error saat menghapus: $friendlyError"
                }
            }
        }
    }
}