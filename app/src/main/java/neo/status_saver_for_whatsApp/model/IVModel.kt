package neo.status_saver_for_whatsApp.model

import android.net.Uri

data class IVModel(
    val path: String,
    val fileName: String,
    val uri: Uri,
    val lastModified: Long
)