package com.needed.books.ui

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.needed.books.BookModel
import com.needed.books.LocaleHelper
import com.needed.books.NativePdfActivity
import com.needed.books.R
import com.needed.books.data.BookRepository
import com.needed.books.security.SecurityVault
import com.needed.books.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

data class NavigationFolder(
    val title: String,
    val path: String,
    val items: List<BookModel>
)

private fun isInternetAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    repository: BookRepository,
    onLanguageChanged: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var rootBooks by remember { mutableStateOf<List<BookModel>>(emptyList()) }
    var currentBooks by remember { mutableStateOf<List<BookModel>>(emptyList()) }
    val navStack = remember { mutableStateListOf<NavigationFolder>() }

    // Start with false so refresh icon is not continuously rotating
    var isLoading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var cacheVersion by remember { mutableIntStateOf(0) }

    // Download state
    var isDownloading by remember { mutableStateOf(false) }
    var downloadBookTitle by remember { mutableStateOf("") }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var downloadSpeed by remember { mutableStateOf("") }
    var downloadDownloadedSize by remember { mutableStateOf("") }
    var downloadActualSize by remember { mutableStateOf("") }
    var downloadRemainingSize by remember { mutableStateOf("") }
    var downloadJob by remember { mutableStateOf<Job?>(null) }
    var isDownloadCancelled by remember { mutableStateOf(false) }

    // Dialogs
    var showInstructionsDialog by remember { mutableStateOf(false) }
    var showAboutDevDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showCacheDialog by remember { mutableStateOf(false) }

    fun openPdfViewer(file: File, bookTitle: String) {
        val intent = Intent(context, NativePdfActivity::class.java).apply {
            putExtra(NativePdfActivity.EXTRA_FILE_PATH, file.absolutePath)
            putExtra(NativePdfActivity.EXTRA_BOOK_TITLE, bookTitle)
        }
        context.startActivity(intent)
    }

    fun startDownload(book: BookModel) {
        if (isDownloading) {
            Toast.makeText(context, "A download is already in progress", Toast.LENGTH_SHORT).show()
            return
        }

        downloadBookTitle = book.name
        downloadProgress = 0
        downloadSpeed = "Connecting..."
        downloadDownloadedSize = "0 KB"
        downloadActualSize = if (book.size.isNotBlank()) book.size else "Calculating..."
        downloadRemainingSize = downloadActualSize
        isDownloading = true
        isDownloadCancelled = false

        downloadJob = coroutineScope.launch {
            val result = repository.downloadBook(
                book = book,
                onProgress = { progress, speed, downloadedSize, actualSize, remainingSize ->
                    downloadProgress = progress
                    downloadSpeed = speed
                    downloadDownloadedSize = downloadedSize
                    downloadActualSize = actualSize
                    downloadRemainingSize = remainingSize
                },
                isCancelled = { isDownloadCancelled }
            )

            isDownloading = false
            if (result.isSuccess && !isDownloadCancelled) {
                cacheVersion++
                Toast.makeText(context, R.string.download_completed, Toast.LENGTH_SHORT).show()
                result.getOrNull()?.let { downloadedFile ->
                    openPdfViewer(downloadedFile, book.name)
                }
            } else if (!isDownloadCancelled) {
                Toast.makeText(context, "Download failed. Please check internet connection.", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun refreshLibrary(isUserInitiated: Boolean = false) {
        if (isLoading) return

        if (!isInternetAvailable(context)) {
            if (isUserInitiated) {
                Toast.makeText(context, "No internet connection. Displaying offline library.", Toast.LENGTH_SHORT).show()
            }
            isLoading = false
            return
        }

        isLoading = true
        coroutineScope.launch {
            try {
                val result = repository.fetchFirebaseBooks()
                if (result.isSuccess) {
                    val books = result.getOrNull() ?: emptyList()
                    rootBooks = books
                    if (navStack.isEmpty()) {
                        currentBooks = books
                    } else {
                        var ptr = books
                        for (folder in navStack) {
                            val matching = ptr.find { it.isFolder && it.name == folder.title }
                            if (matching != null) {
                                ptr = matching.children
                            } else {
                                break
                            }
                        }
                        currentBooks = ptr
                    }
                    if (isUserInitiated) {
                        Toast.makeText(context, "Library refreshed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    if (isUserInitiated) {
                        Toast.makeText(context, "Unable to refresh books. Check connection.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                if (isUserInitiated) {
                    Toast.makeText(context, "Refresh error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isLoading = false
            }
        }
    }

    // Initial load: Instant cache first, then re-fetch ONLY if internet is on (neither don't interrupt user or app)
    LaunchedEffect(Unit) {
        val cached = repository.getCachedBooks()
        if (cached != null) {
            rootBooks = cached
            currentBooks = cached
        }

        if (isInternetAvailable(context)) {
            refreshLibrary(isUserInitiated = false)
        } else {
            isLoading = false
        }

        if (!repository.hasSeenInstructions()) {
            showInstructionsDialog = true
            repository.setHasSeenInstructions(true)
        }
    }

    // Filter books based on search query
    val displayedBooks = remember(currentBooks, searchQuery) {
        if (searchQuery.isBlank()) {
            currentBooks
        } else {
            val query = searchQuery.trim().lowercase()
            currentBooks.filter {
                it.name.lowercase().contains(query) || it.author.lowercase().contains(query)
            }
        }
    }

    val currentPathText = remember(navStack.size) {
        if (navStack.isEmpty()) {
            context.getString(R.string.header_path_root)
        } else {
            "/ " + navStack.joinToString(" / ") { it.title }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xF508111C),
                drawerContentColor = TextPrimary,
                modifier = Modifier
                    .width(316.dp)
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(SurfaceBorder, GlassBorderSubtle, Color.Transparent)
                        ),
                        shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                    )
            ) {
                DrawerContent(
                    onAboutDevClick = {
                        coroutineScope.launch { drawerState.close() }
                        showAboutDevDialog = true
                    },
                    onItemClick = { url ->
                        coroutineScope.launch { drawerState.close() }
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onShareClick = {
                        coroutineScope.launch { drawerState.close() }
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Needed Books")
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Download Needed Books App - Free Offline Book Library & Reader by ${SecurityVault.getDevName()}: ${SecurityVault.getShareAppUrl()}"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Needed Books via"))
                    },
                    onManageCacheClick = {
                        coroutineScope.launch { drawerState.close() }
                        showCacheDialog = true
                    },
                    onLanguageClick = {
                        coroutineScope.launch { drawerState.close() }
                        showLanguageDialog = true
                    },
                    onInstructionsClick = {
                        coroutineScope.launch { drawerState.close() }
                        showInstructionsDialog = true
                    }
                )
            }
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = BackgroundDark,
            topBar = {
                GlassTopAppBar(
                    title = stringResource(R.string.header_library),
                    subtitle = currentPathText,
                    isRoot = navStack.isEmpty(),
                    isLoading = isLoading,
                    onNavClick = {
                        if (navStack.isNotEmpty()) {
                            navStack.removeLastOrNull()
                            currentBooks = if (navStack.isEmpty()) rootBooks else navStack.last().items
                        } else {
                            coroutineScope.launch { drawerState.open() }
                        }
                    },
                    onRefresh = { refreshLibrary(isUserInitiated = true) },
                    onDevClick = { showAboutDevDialog = true }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                BackgroundDark,
                                Color(0xFF091421),
                                Color(0xFF060B12)
                            )
                        )
                    )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search bar
                    SearchBarView(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onClear = { searchQuery = "" }
                    )

                    // Breadcrumb folder bar if inside subdirectory
                    if (navStack.isNotEmpty()) {
                        BreadcrumbBar(
                            currentFolder = navStack.last().title,
                            onBackClick = {
                                navStack.removeLastOrNull()
                                currentBooks = if (navStack.isEmpty()) rootBooks else navStack.last().items
                            }
                        )
                    }

                    // Content State
                    if (isLoading && rootBooks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(SurfaceDarkGlass)
                                    .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
                                    .padding(26.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = AccentGreen,
                                    modifier = Modifier.size(44.dp),
                                    strokeWidth = 3.dp
                                )
                                Text(
                                    text = stringResource(R.string.loading_connecting),
                                    color = TextPrimary,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else if (displayedBooks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(SurfaceDarkGlass)
                                    .border(1.dp, SurfaceBorder, RoundedCornerShape(22.dp))
                                    .padding(26.dp)
                            ) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = AccentGreen,
                                    modifier = Modifier.size(52.dp)
                                )
                                Text(
                                    text = stringResource(R.string.empty_title),
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.empty_subtitle),
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = { refreshLibrary(isUserInitiated = true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        stringResource(R.string.btn_refresh),
                                        color = BackgroundDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(displayedBooks, key = { it.id }) { item ->
                                val isDownloaded = remember(item, cacheVersion) {
                                    if (!item.isFolder) repository.isBookDownloaded(item) else false
                                }

                                BookItemCard(
                                    book = item,
                                    isDownloaded = isDownloaded,
                                    onClick = {
                                        if (item.isFolder) {
                                            navStack.add(NavigationFolder(item.name, item.path, item.children))
                                            currentBooks = item.children
                                            searchQuery = ""
                                        } else {
                                            val localFile = repository.getLocalPdfFile(item)
                                            if (localFile.exists() && localFile.length() > 0) {
                                                openPdfViewer(localFile, item.name)
                                            } else {
                                                startDownload(item)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Downloading Overlay
                if (isDownloading) {
                    DownloadProgressOverlay(
                        title = downloadBookTitle,
                        progress = downloadProgress,
                        speed = downloadSpeed,
                        downloadedSize = downloadDownloadedSize,
                        actualSize = downloadActualSize,
                        remainingSize = downloadRemainingSize,
                        onCancel = {
                            isDownloadCancelled = true
                            downloadJob?.cancel()
                            isDownloading = false
                        }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showInstructionsDialog) {
        InstructionsDialog(onDismiss = { showInstructionsDialog = false })
    }

    if (showAboutDevDialog) {
        AboutDevDialog(onDismiss = { showAboutDevDialog = false })
    }

    if (showLanguageDialog) {
        LanguageDialog(
            currentLang = LocaleHelper.getLanguage(context),
            onDismiss = { showLanguageDialog = false },
            onApply = { selectedLang ->
                showLanguageDialog = false
                if (selectedLang != LocaleHelper.getLanguage(context)) {
                    LocaleHelper.setLocale(context, selectedLang)
                    onLanguageChanged(selectedLang)
                }
            }
        )
    }

    if (showCacheDialog) {
        ManageCacheDialog(
            repository = repository,
            onDismiss = {
                showCacheDialog = false
                cacheVersion++
            },
            onOpenFile = { file ->
                showCacheDialog = false
                openPdfViewer(file, file.nameWithoutExtension)
            }
        )
    }
}

@Composable
private fun SearchBarView(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Surface(
        color = SurfaceDarkGlass,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = AccentGreen,
                modifier = Modifier.size(22.dp)
            )

            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        stringResource(R.string.search_hint),
                        color = TextMuted,
                        fontSize = 13.5.sp
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            if (query.isNotEmpty()) {
                IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BreadcrumbBar(
    currentFolder: String,
    onBackClick: () -> Unit
) {
    Surface(
        color = SurfaceDarkGlass,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AccentGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = "📂  $currentFolder",
                color = AccentGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

@Composable
private fun BookItemCard(
    book: BookModel,
    isDownloaded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = CardGlass,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDownloaded) AccentGreen.copy(alpha = 0.55f) else SurfaceBorder
        ),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            if (isDownloaded) Color(0x1A38EF7D) else Color(0x0F1A2D40),
                            Color(0x050C1622)
                        )
                    )
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container with neon glass glow
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        if (book.isFolder) Color(0x331E40AF)
                        else AccentGreen.copy(alpha = 0.16f)
                    )
                    .border(
                        1.dp,
                        if (book.isFolder) Color(0x4D3B82F6) else AccentGreen.copy(alpha = 0.4f),
                        RoundedCornerShape(13.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (book.isFolder) Icons.Default.Folder else Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = if (book.isFolder) Color(0xFF60A5FA) else AccentGreen,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Author subtitle if book and author exists
                if (!book.isFolder && book.author.isNotBlank()) {
                    Text(
                        text = "✍️ ${book.author}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Metadata badge: Size or Item Count
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = book.size,
                        color = TextMuted,
                        fontSize = 11.5.sp
                    )

                    if (isDownloaded) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = AccentGreen.copy(alpha = 0.22f),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, AccentGreen.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "SAVED OFFLINE",
                                color = AccentGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Action / Indicator Icon
            Icon(
                if (book.isFolder) Icons.Default.ChevronRight
                else if (isDownloaded) Icons.Default.CheckCircle
                else Icons.Default.FileDownload,
                contentDescription = null,
                tint = if (isDownloaded) AccentGreen else TextMuted,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun GlassTopAppBar(
    title: String,
    subtitle: String,
    isRoot: Boolean,
    isLoading: Boolean,
    onNavClick: () -> Unit,
    onRefresh: () -> Unit,
    onDevClick: () -> Unit
) {
    Surface(
        color = SurfaceDarkGlass,
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavClick) {
                Icon(
                    if (isRoot) Icons.Default.Menu else Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (isRoot) "Menu" else "Back",
                    tint = AccentGreen
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = subtitle,
                    color = AccentGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onRefresh, enabled = !isLoading) {
                if (isLoading) {
                    val infiniteTransition = rememberInfiniteTransition(label = "spin_refresh")
                    val angle by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 850, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "spin_angle"
                    )
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refreshing...",
                        tint = AccentGreen,
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer { rotationZ = angle }
                    )
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            IconButton(onClick = onDevClick) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceDarkElevated)
                        .border(1.5.dp, AccentGreen, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_dev_sr7_art),
                        contentDescription = "About Developer (SR7)",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerContent(
    onAboutDevClick: () -> Unit,
    onItemClick: (String) -> Unit,
    onShareClick: () -> Unit,
    onManageCacheClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onInstructionsClick: () -> Unit
) {
    val context = LocalContext.current
    val currentLang = LocaleHelper.getLanguage(context)
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xF50A1422),
                        Color(0xFA070E18)
                    )
                )
            )
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        // Drawer Header with Logo & Adaptive App Identity
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF02060B))
                    .border(1.5.dp, AccentGreen, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.needed_books_logo),
                    contentDescription = "Needed Books Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = stringResource(R.string.header_library),
                    color = AccentGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = stringResource(R.string.app_subtitle),
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${stringResource(R.string.drawer_lead_dev)}: ${SecurityVault.getDevName()}",
                    color = TextMuted,
                    fontSize = 10.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Prominent Developer Profile Card with uploaded artwork
        Surface(
            onClick = onAboutDevClick,
            shape = RoundedCornerShape(14.dp),
            color = SurfaceDarkElevated,
            border = androidx.compose.foundation.BorderStroke(1.2.dp, AccentGreen.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.2.dp, AccentGreen, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_dev_sr7_art),
                        contentDescription = "Developer Artwork",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = SecurityVault.getDevName(),
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Verified Developer",
                            tint = AccentGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = "Lead Developer & Modder",
                        color = AccentGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = SurfaceBorder)
        Spacer(modifier = Modifier.height(16.dp))

        // Community & Social (Powered by SecurityVault obfuscation)
        Text(
            text = stringResource(R.string.drawer_social_profiles),
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        DrawerItem(
            iconRes = R.drawable.ic_tg,
            title = stringResource(R.string.menu_tg),
            onClick = { onItemClick(SecurityVault.getTelegramUrl()) }
        )

        DrawerItem(
            iconRes = R.drawable.ic_wa,
            title = stringResource(R.string.menu_wa),
            onClick = { onItemClick(SecurityVault.getWhatsAppUrl()) }
        )

        DrawerItem(
            iconRes = R.drawable.ic_fb,
            title = stringResource(R.string.menu_fb),
            onClick = { onItemClick(SecurityVault.getFacebookUrl()) }
        )

        DrawerItem(
            iconRes = R.drawable.img_dev_sr7_art,
            title = stringResource(R.string.menu_portfolio),
            onClick = { onItemClick(SecurityVault.getPortfolioUrl()) }
        )

        DrawerItem(
            iconRes = R.drawable.ic_sc,
            title = stringResource(R.string.menu_share_app),
            onClick = onShareClick
        )

        DrawerItem(
            iconRes = R.drawable.ic_source_code,
            title = stringResource(R.string.menu_source_code),
            onClick = { onItemClick(SecurityVault.getSourceCodeUrl()) }
        )

        DrawerItem(
            iconRes = R.drawable.ic_github,
            title = stringResource(R.string.menu_github_profile),
            onClick = { onItemClick(SecurityVault.getGitHubUrl()) }
        )

        Spacer(modifier = Modifier.height(18.dp))
        HorizontalDivider(color = SurfaceBorder)
        Spacer(modifier = Modifier.height(16.dp))

        // Settings & Help
        Text(
            text = stringResource(R.string.drawer_settings_help),
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        DrawerItem(
            iconRes = R.drawable.ic_download,
            title = stringResource(R.string.menu_manage_cache),
            onClick = onManageCacheClick
        )

        // Language Item with dynamic badge
        Surface(
            onClick = onLanguageClick,
            shape = RoundedCornerShape(12.dp),
            color = SurfaceDarkGlass,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_language),
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.menu_language),
                    color = TextPrimary,
                    fontSize = 13.5.sp,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
                Surface(
                    shape = CircleShape,
                    color = AccentGreen.copy(alpha = 0.22f),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, AccentGreen.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = if (currentLang == LocaleHelper.LANG_BANGLA) "বাংলা" else "English",
                        color = AccentGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        DrawerItem(
            iconRes = R.drawable.ic_instructions,
            title = stringResource(R.string.menu_instructions),
            onClick = onInstructionsClick
        )
    }
}

@Composable
private fun DrawerItem(
    iconRes: Int,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = SurfaceDarkGlass,
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconRes == R.drawable.img_dev_sr7_art) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, AccentGreen.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                )
            } else {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.5.sp,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )
        }
    }
}

@Composable
private fun DownloadProgressOverlay(
    title: String,
    progress: Int,
    speed: String,
    downloadedSize: String,
    actualSize: String,
    remainingSize: String,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = SurfaceDarkGlass,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    progress = { if (progress > 0) progress / 100f else 0.5f },
                    color = AccentGreen,
                    modifier = Modifier.size(52.dp),
                    strokeWidth = 3.5.dp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.downloading_book),
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = title,
                    color = AccentGreen,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                LinearProgressIndicator(
                    progress = { if (progress > 0) progress / 100f else 0f },
                    color = AccentGreen,
                    trackColor = SurfaceBorder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (progress >= 0) "$progress%" else "...",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = speed,
                        color = AccentGreen,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Download Metrics: Actual Size, Downloaded Size, and Remaining Size
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceDarkElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Downloaded / Actual Size",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "$downloadedSize / $actualSize",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Remaining Size",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = remainingSize,
                                color = AccentLime,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CANCEL DOWNLOAD", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun InstructionsDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfaceDarkGlass,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AccentGreen.copy(alpha = 0.15f))
                        .border(1.dp, AccentGreen.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_instructions),
                        contentDescription = "Instructions Guide",
                        tint = AccentGreen,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.header_library),
                    color = AccentGreen,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = stringResource(R.string.instructions_subtitle),
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceDarkElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(R.string.instructions_browsing),
                            color = TextPrimary,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.instructions_navigation),
                            color = TextPrimary,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.instructions_offline),
                            color = TextPrimary,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.instructions_reading),
                            color = TextPrimary,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.instructions_contact_dev),
                            color = AccentGreen,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.btn_got_it),
                        color = BackgroundDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutDevDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val currentLang = LocaleHelper.getLanguage(context)
    val bioText = remember(currentLang) {
        if (currentLang == LocaleHelper.LANG_BANGLA) SecurityVault.getDevBioBn()
        else SecurityVault.getDevBioEn()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfaceDarkGlass,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(SurfaceDarkElevated)
                        .border(2.dp, AccentGreen, RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_dev_sr7_art),
                        contentDescription = "SR7 Mods Artwork",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = SecurityVault.getDevName(),
                    color = AccentGreen,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = stringResource(R.string.about_dev_subtitle),
                    color = TextSecondary,
                    fontSize = 12.5.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceDarkElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = bioText,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Connect Bar (Obfuscated URLs via SecurityVault)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SecurityVault.getTelegramUrl()))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGreen),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_tg),
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Telegram", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SecurityVault.getWhatsAppUrl()))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGreen),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_wa),
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SecurityVault.getFacebookUrl()))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGreen),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_fb),
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Facebook", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.btn_close),
                        color = BackgroundDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageDialog(
    currentLang: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    var selected by remember { mutableStateOf(currentLang) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfaceDarkGlass,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_language),
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.dialog_language_title),
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.dialog_language_subtitle),
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // English Card
                Surface(
                    onClick = { selected = LocaleHelper.LANG_ENGLISH },
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceDarkElevated,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selected == LocaleHelper.LANG_ENGLISH) AccentGreen else SurfaceBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "EN",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 14.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.lang_english),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.5.sp
                            )
                            Text(
                                text = "Default • English Interface",
                                color = TextSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                        if (selected == LocaleHelper.LANG_ENGLISH) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bangla Card
                Surface(
                    onClick = { selected = LocaleHelper.LANG_BANGLA },
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceDarkElevated,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selected == LocaleHelper.LANG_BANGLA) AccentGreen else SurfaceBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "বাং",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 14.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.lang_bangla),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.5.sp
                            )
                            Text(
                                text = "বাংলা ইন্টারফেস ও তথ্য",
                                color = TextSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                        if (selected == LocaleHelper.LANG_BANGLA) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = { onApply(selected) },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.btn_apply),
                        color = BackgroundDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ManageCacheDialog(
    repository: BookRepository,
    onDismiss: () -> Unit,
    onOpenFile: (File) -> Unit
) {
    val context = LocalContext.current
    var files by remember { mutableStateOf(repository.getDownloadedPdfs()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfaceDarkGlass,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.FolderZip,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.cache_dialog_title),
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (files.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.cache_empty),
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(files, key = { it.absolutePath }) { file ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceDarkElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenFile(file) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = AccentGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 10.dp)
                                    ) {
                                        Text(
                                            text = repository.getBookTitleForFile(file),
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val sizeMb = file.length() / (1024.0 * 1024.0)
                                        Text(
                                            text = String.format("%.2f MB", sizeMb),
                                            color = TextSecondary,
                                            fontSize = 11.5.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            repository.deleteDownloadedPdf(file)
                                            files = repository.getDownloadedPdfs()
                                            Toast.makeText(context, "Deleted file from cache", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = DangerRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val cleared = repository.clearAllPdfCache()
                            files = repository.getDownloadedPdfs()
                            Toast.makeText(context, "Cleared $cleared cache files", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed.copy(alpha = 0.85f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            stringResource(R.string.cache_clear_all),
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
