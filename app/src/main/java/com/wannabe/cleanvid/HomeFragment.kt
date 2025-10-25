package com.wannabe.cleanvid

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
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
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.android.gms.common.api.Scope

private const val APP_NAME = "CleanVid YouTube Cleaner"

class HomeFragment : Fragment() {

    private lateinit var notificationTextView: TextView
    private lateinit var fastCleanButton: ImageButton

    // Variabel untuk menyimpan daftar spam yang ditemukan untuk proses konfirmasi
    private var foundSpamList: List<SpamComment> = listOf()

    // Launcher untuk menangani hasil dari ConfirmationActivity
    private val confirmationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val idsToDelete = result.data?.getStringArrayListExtra("DELETION_LIST")
            if (idsToDelete != null && idsToDelete.isNotEmpty()) {
                // Filter daftar lengkap berdasarkan ID yang dipilih pengguna
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
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        notificationTextView = view.findViewById(R.id.notificationTextView)
        fastCleanButton = view.findViewById(R.id.fastCleanButton)

        fastCleanButton.setOnClickListener {
            val account = GoogleSignIn.getLastSignedInAccount(requireContext())
            if (account != null) {
                val youtubeScope = Scope("https://www.googleapis.com/auth/youtube.force-ssl")
                if (account.grantedScopes.contains(youtubeScope)) {
                    startQuickClean(account)
                } else {
                    Toast.makeText(requireContext(), "Izin akses YouTube tidak ditemukan. Silakan login ulang di tab Account.", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(requireContext(), "Silakan login di tab Account terlebih dahulu", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startQuickClean(account: GoogleSignInAccount) {
        notificationTextView.text = "Memeriksa 3 video terbaru..."
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

                val channelsResponse = youtube.channels().list(listOf("contentDetails")).setMine(true).execute()

                if (channelsResponse.items.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        notificationTextView.text = "Aksi dibatalkan."
                        Toast.makeText(
                            requireContext(),
                            "Aku ini tidak memiliki channel YouTube.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }


                val uploadPlaylistId = channelsResponse.items.firstOrNull()?.contentDetails?.relatedPlaylists?.uploads
                    ?: throw Exception("Tidak bisa menemukan channel.")

                // LANGKAH 1: Minta data 'status' selain 'snippet'
                val playlistItemsResponse = youtube.playlistItems().list(listOf("snippet,status"))
                    .setPlaylistId(uploadPlaylistId)
                    .setMaxResults(3)
                    .execute()

                // LANGKAH 2: Periksa status privasi setiap video
                val videoItems = playlistItemsResponse.items
                val hasPrivateVideo = videoItems.any { it.status?.privacyStatus == "private" }

                if (hasPrivateVideo) {
                    // LANGKAH 3: Jika ada video privat, hentikan proses dan beri tahu pengguna
                    withContext(Dispatchers.Main) {
                        notificationTextView.text = "Ditemukan video private/terjadwalkan."
                        Toast.makeText(
                            requireContext(),
                            "Pembersihan dibatalkan. hanya video publik & tidak publik",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch // Hentikan eksekusi coroutine
                }

                // LANGKAH 4: Jika semua aman, lanjutkan seperti biasa
                val videosToScan = videoItems.mapNotNull { it.snippet?.resourceId?.videoId }

                withContext(Dispatchers.Main) {
                    notificationTextView.text = "Memindai komentar spam..."
                }

                val collectedSpam = mutableListOf<SpamComment>()
                for (videoId in videosToScan) {
                    collectedSpam.addAll(findSpamCommentsInVideo(youtube, videoId))
                }

                withContext(Dispatchers.Main) {
                    if (collectedSpam.isNotEmpty()) {
                        foundSpamList = collectedSpam
                        val intent = Intent(requireContext(), ConfirmationActivity::class.java)
                        intent.putParcelableArrayListExtra("SPAM_COMMENTS", ArrayList(collectedSpam))
                        confirmationLauncher.launch(intent)
                    } else {
                        notificationTextView.text = "Tidak ada komentar spam ditemukan."
                        Toast.makeText(requireContext(), "Channel Anda bersih!", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                // Blok catch ini tetap penting untuk menangani error tak terduga lainnya
                FirebaseCrashlytics.getInstance().recordException(e)
                e.printStackTrace()
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
            // Panggilan API #1: Published
            val publishedThreads = youtube.commentThreads().list(listOf("snippet"))
                .setVideoId(videoId).setTextFormat("plainText").execute()
            for (commentThread in publishedThreads.items) {
                val comment = commentThread.snippet.topLevelComment
                val commentText = comment.snippet.textDisplay
                if (SpamDetector.isSpam(commentText)) {
                    foundSpam.add(SpamComment(id = comment.id, text = commentText, authorChannelId = comment.snippet.authorChannelId.value))
                }
            }

            // Panggilan API #2: Held for review
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
                    notificationTextView.text = "Proses dibatalkan: Ada komentar video yang dinonaktifkan."
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
                        // Jika ini komentar kita sendiri, HAPUS PERMANEN
                        youtube.comments().delete(comment.id).execute()
                        println("Menghapus (delete) komentar sendiri: ${comment.text}")
                    } else {
                        // Jika ini komentar orang lain, TOLAK (moderasi)
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