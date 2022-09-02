package neo.whatsapp_status_saver.model

import android.net.Uri

data class IVModel(
    val path: String,
    val fileName: String,
    val uri: Uri,
    val lastModified: Long
)