package com.needed.books.pdf

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.listener.OnErrorListener
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import com.github.barteksc.pdfviewer.listener.OnTapListener
import com.needed.books.R
import com.needed.books.ui.theme.*
import java.io.File

private const val PREFS_READER = "pdf_reader_prefs"
private const val KEY_DARK_MODE_USER = "key_dark_mode_user"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    filePath: String,
    bookTitle: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_READER, Context.MODE_PRIVATE) }
    val file = remember(filePath) { File(filePath) }

    var pdfViewRef by remember { mutableStateOf<PDFView?>(null) }
    var totalPages by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Last read page persistence
    val pagePrefKey = remember(file.name) { "last_page_${file.name}" }
    var currentPage by remember {
        mutableIntStateOf(prefs.getInt(pagePrefKey, 0))
    }

    // Dark mode reading preference (defaults to false - no auto dark mode)
    var isDarkMode by remember {
        mutableStateOf(prefs.getBoolean(KEY_DARK_MODE_USER, false))
    }

    // UI overlays
    var showControls by remember { mutableStateOf(true) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var showBookmarksDialog by remember { mutableStateOf(false) }

    // Bookmarks persistence
    val bookmarksKey = remember(file.name) { "bookmarks_${file.name}" }
    val savedBookmarks = remember {
        val set = prefs.getStringSet(bookmarksKey, emptySet()) ?: emptySet()
        mutableStateListOf<Int>().apply {
            addAll(set.mapNotNull { it.toIntOrNull() }.sorted())
        }
    }

    val invertPaint = remember {
        Paint().apply {
            colorFilter = ColorMatrixColorFilter(
                ColorMatrix(
                    floatArrayOf(
                        -1.0f, 0.0f, 0.0f, 0.0f, 255.0f,
                        0.0f, -1.0f, 0.0f, 0.0f, 255.0f,
                        0.0f, 0.0f, -1.0f, 0.0f, 255.0f,
                        0.0f, 0.0f, 0.0f, 1.0f, 0.0f
                    )
                )
            )
        }
    }

    fun toggleBookmark(page: Int) {
        if (savedBookmarks.contains(page)) {
            savedBookmarks.remove(page)
            Toast.makeText(context, "Bookmark removed for Page ${page + 1}", Toast.LENGTH_SHORT).show()
        } else {
            savedBookmarks.add(page)
            savedBookmarks.sort()
            Toast.makeText(context, "Page ${page + 1} bookmarked", Toast.LENGTH_SHORT).show()
        }
        prefs.edit().putStringSet(bookmarksKey, savedBookmarks.map { it.toString() }.toSet()).apply()
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = if (isDarkMode) BackgroundDark else Color(0xFFF1F5F9)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    if (isDarkMode) {
                        Brush.verticalGradient(
                            listOf(
                                BackgroundDark,
                                Color(0xFF09121E),
                                BackgroundDark
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFF8FAFC),
                                Color(0xFFEDF2F7),
                                Color(0xFFE2E8F0)
                            )
                        )
                    }
                )
        ) {
            // High-Performance Native Android PDF Viewer
            if (!isError) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PDFView(ctx, null).apply {
                            pdfViewRef = this
                            setBackgroundColor(if (isDarkMode) 0xFF0B131E.toInt() else 0xFFF1F5F9.toInt())
                            setLayerType(View.LAYER_TYPE_HARDWARE, if (isDarkMode) invertPaint else null)
                            fromFile(file)
                                .defaultPage(currentPage.coerceAtLeast(0))
                                .enableSwipe(true)
                                .swipeHorizontal(false)
                                .enableDoubletap(true)
                                .enableAnnotationRendering(false)
                                .enableAntialiasing(true)
                                .spacing(12)
                                .onLoad(OnLoadCompleteListener { nbPages ->
                                    totalPages = nbPages
                                    isLoading = false
                                    isError = false
                                })
                                .onPageChange(OnPageChangeListener { page, _ ->
                                    currentPage = page
                                    prefs.edit().putInt(pagePrefKey, page).apply()
                                })
                                .onError(OnErrorListener { t ->
                                    isLoading = false
                                    isError = true
                                    errorMessage = t.message ?: "Failed to render document"
                                })
                                .onTap(OnTapListener { _ ->
                                    showControls = !showControls
                                    true
                                })
                                .load()
                        }
                    },
                    update = { view ->
                        pdfViewRef = view
                        view.setBackgroundColor(if (isDarkMode) 0xFF0B131E.toInt() else 0xFFF1F5F9.toInt())
                        view.setLayerType(View.LAYER_TYPE_HARDWARE, if (isDarkMode) invertPaint else null)
                        view.invalidate()
                    }
                )
            }

            // Loading Indicator
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(SurfaceDarkGlass)
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
                            .padding(28.dp)
                    ) {
                        CircularProgressIndicator(
                            color = AccentGreen,
                            modifier = Modifier.size(46.dp),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "Loading Document...",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Error Display
            if (isError) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = SurfaceDarkGlass,
                        shape = RoundedCornerShape(22.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = DangerRed,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Failed to load document",
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = errorMessage,
                                color = TextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = onBack,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Go Back", color = BackgroundDark, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Top Glassmorphic Navigation Bar
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    color = SurfaceDarkGlass,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                    shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                    shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = AccentGreen
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = if (bookTitle.isNotEmpty()) bookTitle else "Document Viewer",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (totalPages > 0) "Page ${currentPage + 1} of $totalPages" else "PDF Document",
                                color = AccentGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Jump to page button
                        IconButton(onClick = { showJumpDialog = true }) {
                            Icon(
                                Icons.Default.FindInPage,
                                contentDescription = "Jump to page",
                                tint = Color.White
                            )
                        }

                        // Bookmark option (Replaced layout mode)
                        val isCurrentBookmarked = savedBookmarks.contains(currentPage)
                        IconButton(onClick = { showBookmarksDialog = true }) {
                            Icon(
                                if (isCurrentBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmarks",
                                tint = if (isCurrentBookmarked) AccentGreen else Color.White
                            )
                        }

                        // Color inversion dark mode toggle
                        IconButton(onClick = {
                            isDarkMode = !isDarkMode
                            prefs.edit().putBoolean(KEY_DARK_MODE_USER, isDarkMode).apply()
                            pdfViewRef?.apply {
                                setBackgroundColor(if (isDarkMode) 0xFF0B131E.toInt() else 0xFFF1F5F9.toInt())
                                setLayerType(View.LAYER_TYPE_HARDWARE, if (isDarkMode) invertPaint else null)
                                invalidate()
                            }
                            Toast.makeText(
                                context,
                                if (isDarkMode) R.string.night_mode_on else R.string.night_mode_off,
                                Toast.LENGTH_SHORT
                            ).show()
                        }) {
                            Icon(
                                if (isDarkMode) Icons.Default.NightlightRound else Icons.Default.WbSunny,
                                contentDescription = "Toggle Dark Mode",
                                tint = if (isDarkMode) AccentGreen else Color(0xFFFFD54F)
                            )
                        }
                    }
                }
            }

            // Bottom Glassmorphic Navigation Bar (Two force controls)
            AnimatedVisibility(
                visible = showControls && totalPages > 0,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    color = SurfaceDarkGlass,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = {
                                if (currentPage > 0) {
                                    pdfViewRef?.jumpTo(currentPage - 1, true)
                                }
                            },
                            enabled = currentPage > 0
                        ) {
                            Icon(
                                Icons.Default.ChevronLeft,
                                contentDescription = "Previous Page",
                                tint = if (currentPage > 0) AccentGreen else TextMuted,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        // Page indicator pill (clickable to jump)
                        Surface(
                            shape = CircleShape,
                            color = SurfaceDarkElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                            modifier = Modifier.clickable { showJumpDialog = true }
                        ) {
                            Text(
                                text = "Page ${currentPage + 1} / $totalPages",
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (currentPage < totalPages - 1) {
                                    pdfViewRef?.jumpTo(currentPage + 1, true)
                                }
                            },
                            enabled = currentPage < totalPages - 1
                        ) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Next Page",
                                tint = if (currentPage < totalPages - 1) AccentGreen else TextMuted,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            }

            // Jump to Page Dialog
            if (showJumpDialog) {
                JumpPageDialog(
                    currentPage = currentPage + 1,
                    totalPages = totalPages,
                    onDismiss = { showJumpDialog = false },
                    onJump = { targetPage ->
                        pdfViewRef?.jumpTo(targetPage - 1, true)
                        showJumpDialog = false
                    }
                )
            }

            // Bookmarks Management Dialog
            if (showBookmarksDialog) {
                BookmarksDialog(
                    currentPage = currentPage,
                    totalPages = totalPages,
                    savedBookmarks = savedBookmarks,
                    onToggleCurrentPage = { toggleBookmark(currentPage) },
                    onJumpToPage = { page ->
                        pdfViewRef?.jumpTo(page, true)
                        showBookmarksDialog = false
                    },
                    onDeleteBookmark = { page -> toggleBookmark(page) },
                    onDismiss = { showBookmarksDialog = false }
                )
            }
        }
    }
}

@Composable
private fun BookmarksDialog(
    currentPage: Int,
    totalPages: Int,
    savedBookmarks: List<Int>,
    onToggleCurrentPage: () -> Unit,
    onJumpToPage: (Int) -> Unit,
    onDeleteBookmark: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val isCurrentBookmarked = savedBookmarks.contains(currentPage)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = SurfaceDarkGlass,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bookmarks (${savedBookmarks.size})",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Toggle bookmark for current page
                Button(
                    onClick = onToggleCurrentPage,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCurrentBookmarked) DangerRed.copy(alpha = 0.25f) else AccentGreen.copy(alpha = 0.25f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isCurrentBookmarked) DangerRed else AccentGreen
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        if (isCurrentBookmarked) Icons.Default.BookmarkRemove else Icons.Default.BookmarkAdd,
                        contentDescription = null,
                        tint = if (isCurrentBookmarked) DangerRed else AccentGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCurrentBookmarked) "Remove Bookmark for Page ${currentPage + 1}" else "+ Bookmark Current Page (${currentPage + 1})",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                HorizontalDivider(color = SurfaceBorder.copy(alpha = 0.5f))

                Spacer(modifier = Modifier.height(12.dp))

                if (savedBookmarks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No bookmarks yet.\nBookmark pages to quickly jump to them anytime.",
                            color = TextMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(savedBookmarks, key = { it }) { page ->
                            val isThisCurrent = page == currentPage
                            Surface(
                                onClick = { onJumpToPage(page) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isThisCurrent) SurfaceDarkElevated else Color(0x151E293B),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isThisCurrent) AccentGreen else SurfaceBorder.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Bookmark,
                                            contentDescription = null,
                                            tint = if (isThisCurrent) AccentGreen else TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Page ${page + 1}",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = if (isThisCurrent) FontWeight.Bold else FontWeight.Medium
                                        )
                                        if (isThisCurrent) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "(Current)",
                                                color = AccentGreen,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { onDeleteBookmark(page) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Delete bookmark",
                                            tint = TextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDarkElevated),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", color = TextSecondary, fontSize = 13.5.sp)
                }
            }
        }
    }
}

@Composable
private fun JumpPageDialog(
    currentPage: Int,
    totalPages: Int,
    onDismiss: () -> Unit,
    onJump: (Int) -> Unit
) {
    var textInput by remember { mutableStateOf(currentPage.toString()) }
    var sliderValue by remember { mutableFloatStateOf(currentPage.toFloat()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = SurfaceDarkGlass,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Jump to Page",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() }
                        textInput = filtered
                        filtered.toIntOrNull()?.let {
                            if (it in 1..totalPages) {
                                sliderValue = it.toFloat()
                            }
                        }
                    },
                    label = { Text("Page (1 - $totalPages)", color = TextSecondary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val target = textInput.toIntOrNull()?.coerceIn(1, totalPages) ?: currentPage
                            onJump(target)
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AccentGreen,
                        unfocusedBorderColor = SurfaceBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Slider(
                    value = sliderValue,
                    onValueChange = {
                        sliderValue = it
                        textInput = it.toInt().toString()
                    },
                    valueRange = 1f..totalPages.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = AccentGreen,
                        activeTrackColor = AccentGreen,
                        inactiveTrackColor = SurfaceBorder
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            val target = textInput.toIntOrNull()?.coerceIn(1, totalPages) ?: currentPage
                            onJump(target)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Go", color = BackgroundDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
