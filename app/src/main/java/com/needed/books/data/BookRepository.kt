package com.needed.books.data

import android.content.Context
import android.content.SharedPreferences
import com.needed.books.BookModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

private const val PREFS_NAME = "needed_books_prefs"
private const val KEY_CACHED_FIREBASE_JSON = "cached_firebase_json"
private const val KEY_HAS_SEEN_INSTRUCTIONS = "has_seen_instructions_v2"
const val DEFAULT_FIREBASE_URL = "https://questionbankcontrol1-default-rtdb.firebaseio.com/files.json"

class BookRepository(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val pdfDirectory: File
        get() {
            val dir = File(context.filesDir, "downloaded_pdfs")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    fun hasSeenInstructions(): Boolean {
        return prefs.getBoolean(KEY_HAS_SEEN_INSTRUCTIONS, false)
    }

    fun setHasSeenInstructions(seen: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_SEEN_INSTRUCTIONS, seen).apply()
    }

    fun getCachedBooks(): List<BookModel>? {
        val json = prefs.getString(KEY_CACHED_FIREBASE_JSON, null) ?: return null
        if (json.isBlank()) return null
        val parsed = BookModel.parseFirebaseJson(json)
        return if (parsed.isNotEmpty()) parsed else null
    }

    suspend fun fetchFirebaseBooks(urlStr: String = DEFAULT_FIREBASE_URL): Result<List<BookModel>> =
        withContext(Dispatchers.IO) {
            try {
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/json")

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val stream = conn.inputStream
                    val jsonStr = stream.bufferedReader().use { it.readText() }
                    conn.disconnect()

                    val books = BookModel.parseFirebaseJson(jsonStr)
                    // Persist valid response
                    prefs.edit().putString(KEY_CACHED_FIREBASE_JSON, jsonStr).apply()
                    Result.success(books)
                } else {
                    conn.disconnect()
                    Result.failure(Exception("HTTP Error: $responseCode"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private val KEY_BOOK_TITLES_MAP = "cached_book_titles_map"

    fun sanitizeFileName(title: String): String {
        var clean = title.trim().replace("[\\\\/:*?\"<>|]".toRegex(), " ")
        clean = clean.replace("\\s+".toRegex(), " ").trim()
        if (clean.isEmpty()) clean = "Untitled_Book"
        return "$clean.pdf"
    }

    fun getLocalPdfFile(book: BookModel): File {
        val fileName = sanitizeFileName(book.name)
        val file = File(pdfDirectory, fileName)
        if (file.exists() && file.length() > 0) {
            return file
        }
        // Fallback check for legacy ascii-only sanitized name if it exists
        val legacySafeName = book.name.replace("[^a-zA-Z0-9.-]".toRegex(), "_") + ".pdf"
        val legacyFile = File(pdfDirectory, legacySafeName)
        if (legacyFile.exists() && legacyFile.length() > 0) {
            return legacyFile
        }
        return file
    }

    fun saveBookTitleMapping(fileName: String, title: String) {
        val prefsObj = prefs.getString(KEY_BOOK_TITLES_MAP, null)
        val json = if (prefsObj != null) {
            try { org.json.JSONObject(prefsObj) } catch (_: Exception) { org.json.JSONObject() }
        } else {
            org.json.JSONObject()
        }
        json.put(fileName, title)
        prefs.edit().putString(KEY_BOOK_TITLES_MAP, json.toString()).apply()
    }

    fun getBookTitleForFile(file: File): String {
        val prefsObj = prefs.getString(KEY_BOOK_TITLES_MAP, null)
        if (prefsObj != null) {
            try {
                val json = org.json.JSONObject(prefsObj)
                if (json.has(file.name)) {
                    val savedTitle = json.optString(file.name, "")
                    if (savedTitle.isNotEmpty()) return savedTitle
                }
            } catch (_: Exception) {}
        }
        return file.nameWithoutExtension
    }

    fun isBookDownloaded(book: BookModel): Boolean {
        val file = getLocalPdfFile(book)
        return file.exists() && file.length() > 0
    }

    fun getDownloadedPdfs(): List<File> {
        val files = pdfDirectory.listFiles() ?: return emptyList()
        return files.filter { it.isFile && it.name.endsWith(".pdf") }
            .sortedByDescending { it.lastModified() }
    }

    fun deleteDownloadedPdf(file: File): Boolean {
        return try {
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            false
        }
    }

    fun clearAllPdfCache(): Int {
        var count = 0
        val files = pdfDirectory.listFiles() ?: return 0
        for (f in files) {
            if (f.isFile && f.delete()) {
                count++
            }
        }
        prefs.edit().remove(KEY_BOOK_TITLES_MAP).apply()
        return count
    }

    companion object {
        fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "0 KB"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(java.util.Locale.US, "%.2f GB", gb)
                mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
                else -> String.format(java.util.Locale.US, "%.0f KB", kb)
            }
        }
    }

    suspend fun downloadBook(
        book: BookModel,
        onProgress: (
            progress: Int,
            speed: String,
            downloadedSize: String,
            actualSize: String,
            remainingSize: String
        ) -> Unit,
        isCancelled: () -> Boolean
    ): Result<File> = withContext(Dispatchers.IO) {
        val targetFile = File(pdfDirectory, sanitizeFileName(book.name))
        saveBookTitleMapping(targetFile.name, book.name)
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            val url = URL(book.downloadUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.instanceFollowRedirects = true
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK &&
                connection.responseCode != HttpURLConnection.HTTP_PARTIAL
            ) {
                return@withContext Result.failure(Exception("HTTP ${connection.responseCode}"))
            }

            val headerLength = connection.getHeaderField("Content-Length")?.toLongOrNull()
                ?: connection.contentLength.toLong()
            val fileLength = if (headerLength > 0) headerLength else -1L

            val actualSizeStr = if (fileLength > 0) {
                formatFileSize(fileLength)
            } else if (book.size.isNotBlank()) {
                book.size
            } else {
                "Unknown"
            }

            inputStream = connection.inputStream
            outputStream = FileOutputStream(targetFile)

            val data = ByteArray(8192)
            var total: Long = 0
            var count: Int
            val startTime = System.currentTimeMillis()
            var lastUpdateTime = startTime

            onProgress(0, "Starting...", "0 KB", actualSizeStr, actualSizeStr)

            while (inputStream.read(data).also { count = it } != -1) {
                if (isCancelled() || !isActive) {
                    outputStream.close()
                    targetFile.delete()
                    return@withContext Result.failure(Exception("Download cancelled"))
                }

                total += count.toLong()
                outputStream.write(data, 0, count)

                val currentTime = System.currentTimeMillis()
                if (currentTime - lastUpdateTime > 200) {
                    val progress = if (fileLength > 0) ((total * 100) / fileLength).toInt().coerceIn(0, 100) else -1
                    val elapsedSeconds = (currentTime - startTime) / 1000.0
                    val speed = if (elapsedSeconds > 0) {
                        val bytesPerSec = total / elapsedSeconds
                        if (bytesPerSec > 1024 * 1024) {
                            String.format(java.util.Locale.US, "%.1f MB/s", bytesPerSec / (1024 * 1024))
                        } else {
                            String.format(java.util.Locale.US, "%.0f KB/s", bytesPerSec / 1024)
                        }
                    } else "..."

                    val downloadedStr = formatFileSize(total)
                    val remainingStr = if (fileLength > 0) {
                        formatFileSize((fileLength - total).coerceAtLeast(0L))
                    } else {
                        "..."
                    }

                    onProgress(progress, speed, downloadedStr, actualSizeStr, remainingStr)
                    lastUpdateTime = currentTime
                }
            }

            outputStream.flush()
            val finalDownloadedStr = formatFileSize(total)
            onProgress(100, "Completed", finalDownloadedStr, finalDownloadedStr, "0 KB")
            Result.success(targetFile)
        } catch (e: Exception) {
            targetFile.delete()
            Result.failure(e)
        } finally {
            try {
                outputStream?.close()
                inputStream?.close()
                connection?.disconnect()
            } catch (_: Exception) {}
        }
    }
}
