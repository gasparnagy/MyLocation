package com.gasparnagy.mylocation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.location.Location
import android.os.Build
import android.widget.Toast

object Clipboard {

    /** Copies the shareable location text to the clipboard and tells the user to paste it. */
    fun copyLocation(context: Context, location: Location) {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(
            ClipData.newPlainText(context.getString(R.string.app_name), LocationText.shareText(context, location))
        )
        // Android 13+ shows its own confirmation when something is copied.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, R.string.copied_toast, Toast.LENGTH_LONG).show()
        }
    }
}
