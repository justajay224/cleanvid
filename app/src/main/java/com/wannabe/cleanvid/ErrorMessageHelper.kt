package com.wannabe.cleanvid

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import java.io.IOException

object ErrorMessageHelper {

    fun getFriendlyErrorMessage(exception: Exception): String {
        return when (exception) {
            is GoogleJsonResponseException -> {
                // Menerjemahkan error spesifik dari Google API
                when (exception.statusCode) {
                    403 -> "Masalah izin. Pastikan API YouTube diizinkan di Google Cloud Console."
                    404 -> "Data tidak ditemukan. Video atau komentar mungkin sudah dihapus."
                    500, 503 -> "Server YouTube sedang sibuk. Silakan coba lagi nanti."
                    else -> "Terjadi kesalahan saat berkomunikasi dengan YouTube (Kode: ${exception.statusCode})."
                }
            }
            is IOException -> {
                "Koneksi internet bermasalah. Silakan periksa jaringan Anda."
            }
            else -> {
                "Terjadi kesalahan saat memproses permintaan."
//                "Error (Debug): ${exception.javaClass.simpleName} - ${exception.message}"
            }
        }
    }
}