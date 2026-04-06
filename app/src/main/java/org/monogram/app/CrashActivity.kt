package org.monogram.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.monogram.app.components.CrashScreen
import org.monogram.app.ui.theme.MonoGramTheme
import kotlin.system.exitProcess

class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val crashLog = intent.getStringExtra("EXTRA_CRASH_LOG") ?: "No log available"

        setContent {
            MonoGramTheme {
                CrashScreen(
                    log = crashLog,
                    onCopy = { copyToClipboard(crashLog) },
                    onShare = { shareLog(crashLog) },
                    onRestart = { restartApp() },
                )
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(getString(R.string.crash_log_label), text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, getString(R.string.crash_copied_to_clipboard), Toast.LENGTH_SHORT).show()
    }

    private fun shareLog(text: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, getString(R.string.crash_share_title))
        startActivity(shareIntent)
    }

    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
        exitProcess(0)
    }
}
