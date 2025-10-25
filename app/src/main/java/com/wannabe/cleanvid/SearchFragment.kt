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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Pattern
import com.google.android.gms.common.api.Scope

private const val APP_NAME = "CleanVid YouTube Cleaner"

class SearchFragment : Fragment() {

    // --- Deklarasi Variabel UI & Data ---
    private lateinit var notificationTextView: TextView
    private lateinit var urlEditText: EditText
    private lateinit var pasteButton: Button
    private lateinit var cleanVideoButton: Button
    private var foundSpamList: List<SpamComment> = listOf()

    // Launcher untuk menangani hasil dari ConfirmationActivity
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

        pasteButton.setOnClickListener { pasteFromClipboard() }
        cleanVideoButton.setOnClickListener { handleCleanUrl() }
    }

    private fun pasteFromClipboard() {
        val clipboard = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val textToPaste = clipboard.primaryClip?.getItemAt(0)?.text
        urlEditText.setText(textToPaste)
    }

    private fun handleCleanUrl() {
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        if (account == null) {
            Toast.makeText(requireContext(), "Silakan login di tab Account terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        val youtubeScope = Scope("https://www.googleapis.com/auth/youtube.force-ssl")
        if (!account.grantedScopes.contains(youtubeScope)) {
            // Jika izin TIDAK ada, hentikan proses dan beri tahu pengguna
            Toast.makeText(requireContext(), "Izin akses YouTube tidak ditemukan. Silakan login ulang di tab Account.", Toast.LENGTH_LONG).show()
            return // Hentikan eksekusi
        }

        val url = urlEditText.text.toString()
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

    private fun extractVideoIdFromUrl(url: String): String? {
        val patterns = arrayOf(
            "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*",
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

                // LANGKAH 1: Dapatkan ID channel pengguna yang sedang login
                val channelsResponse = youtube.channels().list(listOf("id")).setMine(true).execute()
                val myChannelId = channelsResponse.items.firstOrNull()?.id
                if (myChannelId == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Tidak dapat memverifikasi channel Anda.", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                // LANGKAH 2: Dapatkan ID channel pemilik video dari URL
                val videosResponse = youtube.videos().list(listOf("snippet")).setId(listOf(videoId)).execute()
                val videoOwnerChannelId = videosResponse.items.firstOrNull()?.snippet?.channelId
                if (videoOwnerChannelId == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Video tidak ditemukan atau tidak valid.", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                // LANGKAH 3: Bandingkan keduanya
                if (myChannelId != videoOwnerChannelId) {
                    withContext(Dispatchers.Main) {
                        notificationTextView.text = "Aksi dibatalkan. Gunakan Video sesuai akun yang sedang login pada Aplikasi"
                        Toast.makeText(requireContext(), "Error: Anda hanya dapat memindai video dari channel Anda sendiri.", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                // --- Jika verifikasi berhasil, lanjutkan proses pemindaian seperti biasa ---
                withContext(Dispatchers.Main) {
                    notificationTextView.text = "Mencari komentar spam di video..."
                }

                val collectedSpam = findSpamCommentsInVideo(youtube, videoId)

                withContext(Dispatchers.Main) {
                    if (collectedSpam.isNotEmpty()) {
                        foundSpamList = collectedSpam
                        val intent = Intent(requireContext(), ConfirmationActivity::class.java)
                        intent.putParcelableArrayListExtra("SPAM_COMMENTS", ArrayList(collectedSpam))
                        confirmationLauncher.launch(intent)
                    } else if (notificationTextView.text.contains("dinonaktifkan").not()) {
                        notificationTextView.text = "Tidak ada komentar spam ditemukan."
                        Toast.makeText(requireContext(), "Video ini bersih!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    val friendlyError = ErrorMessageHelper.getFriendlyErrorMessage(e)
                    notificationTextView.text = "Error: $friendlyError"
                }
            }
        }
    }

    private suspend fun findSpamCommentsInVideo(youtube: YouTube, videoId: String): List<SpamComment> {
        val foundSpam = mutableListOf<SpamComment>()
        try {
            val publishedThreads = youtube.commentThreads().list(listOf("snippet"))
                .setVideoId(videoId).setTextFormat("plainText").execute()
            for (commentThread in publishedThreads.items) {
                val comment = commentThread.snippet.topLevelComment
                val commentText = comment.snippet.textDisplay
                if (SpamDetector.isSpam(commentText)) {
                    foundSpam.add(SpamComment(id = comment.id, text = commentText, authorChannelId = comment.snippet.authorChannelId.value))
                }
            }

            val heldThreads = youtube.commentThreads().list(listOf("snippet"))
                .setVideoId(videoId).setModerationStatus("heldForReview").setTextFormat("plainText").execute()
            for (commentThread in heldThreads.items) {
                val comment = commentThread.snippet.topLevelComment
                val commentText = comment.snippet.textDisplay
                if (SpamDetector.isSpam(commentText)) {
                    foundSpam.add(SpamComment(id = comment.id, text = commentText, authorChannelId = comment.snippet.authorChannelId.value))
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
                        println("Menghapus (delete) komentar sendiri: ${comment.text}")
                    } else {
                        youtube.comments().setModerationStatus(listOf(comment.id), "rejected").execute()
                        println("Menolak (reject) komentar orang lain: ${comment.text}")
                    }
                }

                withContext(Dispatchers.Main) {
                    notificationTextView.text = "${commentsToDelete.size} komentar berhasil diproses."
                    Toast.makeText(requireContext(), "Pembersihan Selesai!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    notificationTextView.text = "Error saat menghapus: ${e.message}"
                }
            }
        }
    }
}