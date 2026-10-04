package com.venom.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.content.ContextCompat
import java.text.DateFormat
import java.util.Date

/**
 * Comprehensive utility extensions for Android development
 */
object Extensions {

    /**
     * UI Extensions
     */
    fun Context.showToast(message: String?, duration: Int = Toast.LENGTH_SHORT) {
        message?.takeIf { it.isNotBlank() }?.let {
            Toast.makeText(this, it, duration).show()
        }
    }

    fun TextFieldValue.getSelectedOrFullText(): String {
        return if (selection.start != selection.end) {
            text.substring(selection.start, selection.end)
        } else {
            text
        }
    }

    /**
     * Clipboard Operations
     */

    fun Context.copyToClipboard(text: String, label: String = "Copied Text") {
        val clipboard = getClipboardManager()
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        this.showToast("$text: Copied")
    }

    fun Context.pasteFromClipboard(): String? = ContextCompat.getSystemService(
        this, ClipboardManager::class.java
    )?.primaryClip?.getItemAt(0)?.text?.toString()

    /*
    * Text Validation
    * */

    fun isValidWordForSentences(text: String): Boolean {
        val trimmedText = text.trim()
        return trimmedText.isNotBlank() && !trimmedText.contains(Regex("\\s+")) && trimmedText.length >= 2 && trimmedText.matches(Regex("[a-zA-Z]+"))
    }

    /**
     * Text Processing
     */
    fun String.splitCamelCase(): String = buildString(this.length + 4) {
        this@splitCamelCase.forEachIndexed { index, char ->
            if (index > 0 && char.isUpperCase() && this@splitCamelCase[index - 1] != ' ') {
                append(' ')
            }
            append(char)
        }
    }

    private fun calculateFontSizeBasedOnBoxHeight(context: Context, boxHeight: Double): Float {
        // Convert bounding box height to appropriate text size in pixels
        val metrics = context.resources.displayMetrics
        return (boxHeight * metrics.density).toFloat()
    }

    fun String.splitCamelCaseRegex(): String =
        this.split("(?<!^)(?<!\\s)(?=[A-Z])".toRegex()).joinToString(" ")

    /**
     * Simple function to clean HTML and prepare text for TTS
     * Removes all HTML tags, styles, and special characters
     */
    fun String.cleanForTTS1(): String {
        return this
            // Remove style tags
            .replace(Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL), "")
            // Remove script tags
            .replace(Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL), "")
            // Remove meta tags
            .replace(Regex("<meta[^>]*>", RegexOption.IGNORE_CASE), "")
            // Remove all HTML tags
            .replace(Regex("<[^>]+>"), "")
            // Decode HTML entities
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            // Replace underscores with "blank"
            .replace(Regex("[_]{2,}"), " blank ")
            .replace("_", " blank ")
            // Remove special characters
            .replace(Regex("[*#@\\[\\]{}()]"), "")
            // Clean whitespace
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun String.sanitize(): String = replace("\n", "").replace(".", "")

    /**
     * Extension function to clean text for TTS by removing or replacing special characters
     * such as underscores, hyphens, apostrophes, and other symbols that may affect TTS quality.
     *
     * @return Cleaned text optimized for TTS
     */
    fun String.cleanForTTS(): String {
        return this
            .replace("_", " ")
            .replace("-", " ")
            .replace("'", "")
            .replace("\"", "")
            .replace("/", " ")
            .replace("\\", " ")
            .replace("(", "")
            .replace(")", "")
            .replace("[", "")
            .replace("]", "")
            .replace("{", "")
            .replace("}", "")
            .replace("*", "")
            .replace("+", " plus ")
            .replace("=", " equals ")
            .replace("&", " and ")
            .replace("#", "")
            .replace("@", " at ")
            .replace("  ", " ")
            .trim()
    }

    /**
     * Keyboard Management
     */
    fun View.showKeyboard() {
        requestFocus()
        context.getInputMethodManager().showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }

    fun View.hideKeyboard() {
        context.getInputMethodManager().hideSoftInputFromWindow(windowToken, 0)
    }

    /**
     * System Services
     */
    fun Context.getVibrator(): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun Context.getClipboardManager(): ClipboardManager =
        getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    fun Context.getInputMethodManager(): InputMethodManager =
        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    /**
     * Intent & Navigation
     */
    fun Context.shareText(text: String?, chooserTitle: String? = null) {
        text?.let {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, it)
            }
            startActivity(Intent.createChooser(shareIntent, chooserTitle))
        }
    }

    fun Context.openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        })
    }

    fun Context.openTTSSettings() {
        startActivity(Intent().apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            action = "com.android.settings.TTS_SETTINGS"
        })
    }

    fun <T> Context.openActivity(it: Class<T>, flags: Int? = null) {
        startActivity(Intent(this, it).apply {
            flags?.let { f -> this.flags = f }
        })
    }

    /**
     * Time Conversions
     */

    fun Long.formatTimestamp(): String =
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(this))

    fun Int.formatTime(): String = "%02d:%02d".format(this / 60, this % 60)

    fun Long.millisToSeconds(): Long = this / 1000

    fun Long.secondsToMillis(): Long = this * 1000

    /*
     * Image Processing
     */
    fun ByteArray.toBitmap(): ImageBitmap =
        BitmapFactory.decodeByteArray(this, 0, this.size).asImageBitmap()
}