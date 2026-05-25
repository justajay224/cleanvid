package com.wannabe.cleanvid

// Object ini berfungsi sebagai "Gudang Penyimpanan" sementara di memori HP
object CommentDataHolder {
    var spamList: List<String> = emptyList()
    var normalList: List<String> = emptyList()
    var totalComments: Int = 0

    // Fungsi untuk membersihkan memori (opsional tapi disarankan)
    fun clearData() {
        spamList = emptyList()
        normalList = emptyList()
        totalComments = 0
    }
}