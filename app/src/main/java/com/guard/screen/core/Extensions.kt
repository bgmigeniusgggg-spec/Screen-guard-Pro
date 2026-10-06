package com.guard.screen.core

import android.content.Context
import android.os.Build
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================
// CONTEXT EXTENSIONS
// ============================================

fun Context.toast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

fun Context.toastLong(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}

// ============================================
// TIME EXTENSIONS
// ============================================

fun Long.toTimeAgo(): String {
    val diff = System.currentTimeMillis() - this
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 2_592_000_000 -> "${diff / 86_400_000}d ago"
        else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(this))
    }
}

fun Long.toFormattedTime(): String {
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(this))
}

fun Long.toFormattedDateTime(): String {
    return SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(this))
}

// ============================================
// STRING EXTENSIONS
// ============================================

fun String.isValidDeviceKey(): Boolean {
    return matches(Regex("^[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$"))
}

fun String.isValidUrl(): Boolean {
    return startsWith("http://") || startsWith("https://")
}

fun String.truncate(maxLength: Int): String {
    return if (length <= maxLength) this else "${substring(0, maxLength - 3)}..."
}

// ============================================
// FILE EXTENSIONS
// ============================================

fun String.getFileExtension(): String {
    return substringAfterLast(".", "")
}

fun String.getMimeType(): String {
    return when (getFileExtension().lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "mp4" -> "video/mp4"
        "m4a" -> "audio/mp4"
        "mp3" -> "audio/mpeg"
        "aac" -> "audio/aac"
        "wav" -> "audio/wav"
        "3gp" -> "video/3gpp"
        "webm" -> "video/webm"
        else -> "application/octet-stream"
    }
}

fun Long.formatFileSize(): String {
    return when {
        this < 1024 -> "$this B"
        this < 1024 * 1024 -> "${this / 1024} KB"
        this < 1024 * 1024 * 1024 -> String.format("%.1f MB", this / (1024.0 * 1024.0))
        else -> String.format("%.2f GB", this / (1024.0 * 1024.0 * 1024.0))
    }
}

// ============================================
// DEVICE EXTENSIONS
// ============================================

fun getDeviceInfo(): String {
    return "${Build.MANUFACTURER} ${Build.MODEL}"
}

fun getAndroidVersion(): String {
    return "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
}

// ============================================
// COMMAND PARSER
// ============================================

/**
 * Command parse karo — "REC:60" → (REC, 60)
 */
fun String.parseCommand(): Pair<String, String?> {
    val parts = split(":", limit = 2)
    val base = parts[0].uppercase().trim()
    val param = if (parts.size > 1) parts[1].trim() else null
    return Pair(base, param)
}

/**
 * Duration safely parse karo.
 */
fun String?.parseDuration(default: Int): Int {
    if (this.isNullOrBlank()) return default
    return toIntOrNull()?.coerceIn(5, Constants.MAX_DURATION) ?: default
}
