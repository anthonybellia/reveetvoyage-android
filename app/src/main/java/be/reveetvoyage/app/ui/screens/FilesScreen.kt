package be.reveetvoyage.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.reveetvoyage.app.data.model.Message
import be.reveetvoyage.app.data.repo.MessageRepository
import be.reveetvoyage.app.ui.theme.*
import coil.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FilesViewModel @Inject constructor(private val repo: MessageRepository) : ViewModel() {
    private val _files = MutableStateFlow<List<Message>>(emptyList())
    val files: StateFlow<List<Message>> = _files.asStateFlow()
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init { reload() }

    fun reload() {
        viewModelScope.launch {
            _isLoading.value = true
            _files.value = runCatching { repo.files() }.getOrDefault(emptyList())
            _isLoading.value = false
        }
    }
}

private enum class Filter(val label: String) {
    All("Tous"), Images("Photos"), Pdfs("Documents");
    fun matches(m: Message): Boolean = when (this) {
        All -> m.hasAttachment
        Images -> m.attachment_type == "image"
        Pdfs -> m.attachment_type == "pdf" || m.attachment_type == "other"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(onBack: () -> Unit, vm: FilesViewModel = hiltViewModel()) {
    val files by vm.files.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    var filter by remember { mutableStateOf(Filter.All) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes fichiers", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).background(
                Brush.verticalGradient(listOf(RevYellow.copy(alpha = .06f), RevBackground))
            )
        ) {
            // Filter chips
            Row(
                modifier = Modifier.padding(horizontal = 18.dp).padding(top = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Filter.values().forEach { f ->
                    val sel = f == filter
                    Box(
                        modifier = Modifier.clip(CircleShape)
                            .background(if (sel) Brush.horizontalGradient(listOf(RevOrange, RevRed))
                                        else Brush.horizontalGradient(listOf(RevCardBackground, RevCardBackground)))
                            .clickable { filter = f }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(f.label, color = if (sel) Color.White else RevBrown,
                             fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }

            val filtered = files.filter(filter::matches)
            when {
                isLoading && files.isEmpty() -> Box(modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center) { CircularProgressIndicator(color = RevOrange) }
                filtered.isEmpty() -> Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Default.Folder, null, tint = RevOrange.copy(alpha = .4f),
                         modifier = Modifier.size(60.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Aucun fichier dans cette catégorie",
                         color = RevTextSecondary, fontSize = 14.sp)
                }
                filter == Filter.Images -> ImageGrid(filtered, onOpen = { url ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                })
                else -> FileList(filtered, onOpen = { url ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                })
            }
        }
    }
}

@Composable
private fun ImageGrid(items: List<Message>, onOpen: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        items(items, key = { it.id }) { msg ->
            val url = msg.attachment_url
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(10.dp))
                        .clickable { onOpen(url) },
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Composable
private fun FileList(items: List<Message>, onOpen: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        items(items, key = { it.id }) { msg ->
            FileRow(msg, onOpen = { msg.attachment_url?.let(onOpen) })
        }
    }
}

@Composable
private fun FileRow(msg: Message, onOpen: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(RevCardBackground).clickable(onClick = onOpen).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val (icon, color) = when (msg.attachment_type) {
            "pdf" -> Icons.Default.PictureAsPdf to RevRed
            else -> Icons.Default.FileOpen to RevOrange
        }
        Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(color),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(msg.attachment_name ?: "Fichier", color = RevBrown, fontWeight = FontWeight.SemiBold,
                 fontSize = 14.sp, maxLines = 1)
            val date = msg.created_at.substring(0, 10)
            Text("${(msg.attachment_type ?: "fichier").uppercase()} · ${formatSize(msg.attachment_size)} · $date",
                 color = RevTextSecondary, fontSize = 11.sp)
        }
    }
}

private fun formatSize(bytes: Int?): String {
    if (bytes == null) return "—"
    if (bytes < 1024) return "$bytes B"
    if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0)
    return String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}
