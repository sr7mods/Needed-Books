package com.needed.books

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.needed.books.data.BookRepository
import com.needed.books.security.SecurityVault
import com.needed.books.ui.MainScreen
import com.needed.books.ui.theme.NeededBooksTheme

/**
 * Main Activity rewritten in Kotlin with Jetpack Compose.
 * Protected with SecurityVault anti-tamper verification.
 */
class MainActivity : ComponentActivity() {

    private lateinit var repository: BookRepository

    override fun attachBaseContext(newBase: Context) {
        val wrappedContext = try {
            LocaleHelper.onAttach(newBase)
        } catch (e: Throwable) {
            newBase
        }
        super.attachBaseContext(wrappedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            LocaleHelper.applyLocale(this)
        } catch (e: Throwable) {
            // Safe fallback
        }
        super.onCreate(savedInstanceState)

        // Runtime check for package name and app integrity
        if (!SecurityVault.verifyAppIntegrity(this)) {
            Toast.makeText(this, "Security verification failed: Untrusted application package or modifications detected.", Toast.LENGTH_LONG).show()
            finishAffinity()
            return
        }

        enableEdgeToEdge()

        repository = BookRepository(this)

        setContent {
            NeededBooksTheme {
                MainScreen(
                    repository = repository,
                    onLanguageChanged = { _ ->
                        try {
                            LocaleHelper.applyLocale(this)
                        } catch (_: Throwable) {}
                        recreate()
                    }
                )
            }
        }
    }
}
