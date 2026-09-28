package com.needed.books

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.needed.books.pdf.PdfViewerScreen
import com.needed.books.ui.theme.NeededBooksTheme

/**
 * Native PDF Viewer Activity rewritten in Kotlin with Jetpack Compose.
 */
class NativePdfActivity : ComponentActivity() {

    companion object {
        const val EXTRA_FILE_PATH = "extra_file_path"
        const val EXTRA_BOOK_TITLE = "extra_book_title"
    }

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
        enableEdgeToEdge()

        val filePath = intent.getStringExtra(EXTRA_FILE_PATH) ?: ""
        val bookTitle = intent.getStringExtra(EXTRA_BOOK_TITLE) ?: "Document Viewer"

        setContent {
            NeededBooksTheme {
                PdfViewerScreen(
                    filePath = filePath,
                    bookTitle = bookTitle,
                    onBack = { finish() }
                )
            }
        }
    }
}
