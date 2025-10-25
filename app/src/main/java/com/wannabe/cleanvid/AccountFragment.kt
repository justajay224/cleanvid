package com.wannabe.cleanvid

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController

class AccountFragment : Fragment() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private var loggedInAccount: GoogleSignInAccount? = null

    // UI Elements
    private lateinit var authButton: Button
    private lateinit var userInfoTextView: TextView
    private lateinit var infoButton: Button

    // Launcher untuk menangani hasil dari proses login
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!

                val youtubeScope = Scope("https://www.googleapis.com/auth/youtube.force-ssl")
                if (account.grantedScopes.contains(youtubeScope)) {
                    loggedInAccount = account
                    Toast.makeText(requireContext(), "Login Berhasil", Toast.LENGTH_SHORT).show()
                    updateUI(account)
                } else {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Izin Diperlukan")
                        .setMessage("Untuk menggunakan fitur pembersihan, Anda harus mencentang kotak izin akses ke data YouTube. Silakan coba login kembali.")
                        .setPositiveButton("Coba Lagi") { _, _ ->
                            signIn()
                        }
                        .setNegativeButton("Batal", null)
                        .show()

                    signOut()
                }

            } catch (e: ApiException) {
                loggedInAccount = null
                Toast.makeText(requireContext(), "Login Gagal", Toast.LENGTH_SHORT).show()
                updateUI(null)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Temukan UI element di layout fragment
        authButton = view.findViewById(R.id.authButton)
        userInfoTextView = view.findViewById(R.id.userInfoTextView)
        infoButton = view.findViewById(R.id.infoButton)

        // Konfigurasi Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/youtube.force-ssl"))
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        authButton.setOnClickListener {
            if (loggedInAccount == null) {
                signIn()
            } else {
                AlertDialog.Builder(requireContext())
                    .setTitle("Konfirmasi Logout")
                    .setMessage("Apakah Anda yakin ingin logout?")
                    .setPositiveButton("Ya, Logout") { _, _ ->
                        signOut()
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }
        }
        // Di dalam onViewCreated di AccountFragment.kt

        infoButton.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_account_to_infoFragment)
        }
    }

    override fun onStart() {
        super.onStart()
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        if (account != null) {
            loggedInAccount = account
            updateUI(account)
        } else {
            updateUI(null)
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private fun signOut() {
        googleSignInClient.signOut().addOnCompleteListener(requireActivity()) {
            loggedInAccount = null
            updateUI(null)
            Toast.makeText(requireContext(), "Logout Successfull", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI(account: GoogleSignInAccount?) {
        // <-- 3. PERBARUI FUNGSI updateUI
        if (account != null) {
            // Kondisi Saat LOGIN
            authButton.text = "Logout"
            userInfoTextView.visibility = View.VISIBLE
            // Format teks untuk menampilkan nama dan email
            userInfoTextView.text = "Login sebagai:\n${account.displayName}\n(${account.email})"

        } else {
            // Kondisi Saat LOGOUT
            authButton.text = "Login dengan Google"
            userInfoTextView.visibility = View.GONE // Sembunyikan saat logout
            userInfoTextView.text = "" // Kosongkan teks
        }
    }

}