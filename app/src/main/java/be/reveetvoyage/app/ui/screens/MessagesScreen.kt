package be.reveetvoyage.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.reveetvoyage.app.data.model.Message
import be.reveetvoyage.app.data.repo.MessageRepository
import be.reveetvoyage.app.ui.components.IOSTopBar
import be.reveetvoyage.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessagesViewModel @Inject constructor(private val repo: MessageRepository) : ViewModel() {
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()
    private val _draft = MutableStateFlow("")
    val draft: StateFlow<String> = _draft.asStateFlow()
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private var pollingJob: kotlinx.coroutines.Job? = null

    fun start() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            // initial fetch
            _messages.value = runCatching { repo.list() }.getOrDefault(emptyList())
            // poll every 5s
            while (true) {
                delay(5000)
                val since = _messages.value.lastOrNull()?.created_at
                runCatching { repo.list(since) }.onSuccess { fresh ->
                    if (fresh.isNotEmpty()) {
                        val existing = _messages.value.map { it.id }.toSet()
                        _messages.value = _messages.value + fresh.filter { it.id !in existing }
                    }
                }
            }
        }
    }

    fun stop() {
        pollingJob?.cancel(); pollingJob = null
    }

    fun setDraft(s: String) { _draft.value = s }

    fun send() {
        val body = _draft.value.trim()
        if (body.isEmpty() || _isSending.value) return
        viewModelScope.launch {
            _isSending.value = true
            val snapshot = _draft.value
            _draft.value = ""
            runCatching { repo.send(body) }
                .onSuccess { _messages.value = _messages.value + it }
                .onFailure { _draft.value = snapshot }
            _isSending.value = false
        }
    }

    fun sendAttachment(bytes: ByteArray, fileName: String, mime: String) {
        viewModelScope.launch {
            _isSending.value = true
            val body = _draft.value.trim()
            val snapshot = _draft.value
            _draft.value = ""
            runCatching { repo.sendWithAttachment(body, bytes, fileName, mime) }
                .onSuccess { _messages.value = _messages.value + it }
                .onFailure { _draft.value = snapshot }
            _isSending.value = false
        }
    }

    override fun onCleared() {
        stop(); super.onCleared()
    }
}

@Composable
fun MessagesScreen(
    onBack: () -> Unit,
    initialDraft: String? = null,
    onOpenFiles: () -> Unit = {},
    vm: MessagesViewModel = hiltViewModel(),
) {
    val messages by vm.messages.collectAsState()
    val draft by vm.draft.collectAsState()
    val isSending by vm.isSending.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            val name = uri.lastPathSegment ?: "photo.jpg"
            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            if (bytes != null) vm.sendAttachment(bytes, name, mime)
        }
    }
    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "document.pdf"
            val mime = context.contentResolver.getType(uri) ?: "application/pdf"
            if (bytes != null) vm.sendAttachment(bytes, name, mime)
        }
    }

    LaunchedEffect(initialDraft) {
        if (!initialDraft.isNullOrEmpty() && draft.isEmpty()) {
            vm.setDraft(initialDraft)
        }
    }

    DisposableEffect(Unit) { vm.start(); onDispose { vm.stop() } }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(modifier = Modifier.fillMaxSize().background(RevBackground)) {
        IOSTopBar(
            title = "Équipe Rêve et Voyage",
            onBack = onBack,
            trailing = {
                IconButton(onClick = onOpenFiles) {
                    Icon(Icons.Default.Folder, null, tint = RevOrange)
                }
            },
        )

        Column(modifier = Modifier.fillMaxSize()) {
            if (messages.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💬", fontSize = 60.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Discute avec l'équipe", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = RevBrown)
                        Text("Pose tes questions, on te répondra rapidement.",
                             color = RevTextSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(messages, key = { it.id }) { MessageBubble(it) }
                }
            }

            // iOS-style input bar — hairline border field + round attachment & send buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(
                    onClick = {
                        photoPicker.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(RevOrange.copy(alpha = .10f)),
                ) { Icon(Icons.Default.Image, null, tint = RevOrange, modifier = Modifier.size(18.dp)) }

                IconButton(
                    onClick = { docPicker.launch(arrayOf("application/pdf")) },
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(RevOrange.copy(alpha = .10f)),
                ) { Icon(Icons.Default.AttachFile, null, tint = RevOrange, modifier = Modifier.size(18.dp)) }

                // Hairline-bordered input matching IOSTextField look but multiline
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    color = RevCardBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x14000000)),
                ) {
                    Box(
                        modifier = Modifier
                            .heightIn(min = 38.dp)
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = draft,
                            onValueChange = vm::setDraft,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4,
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(RevOrange),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = RevBrown,
                                fontSize = 15.sp,
                            ),
                            decorationBox = { inner ->
                                if (draft.isEmpty()) {
                                    Text("Ton message…", color = RevTextSecondary, fontSize = 15.sp)
                                }
                                inner()
                            },
                        )
                    }
                }

                IconButton(
                    onClick = vm::send,
                    enabled = (draft.isNotBlank() && !isSending),
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(
                        Brush.linearGradient(listOf(RevOrange, RevRed))
                    ),
                ) {
                    if (isSending) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp))
                    else Icon(Icons.Default.Send, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: Message) {
    val isUser = msg.isFromUser
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (msg.hasAttachment) {
                AttachmentPreview(msg, onOpen = { url ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                })
            }

            if (msg.body.isNotEmpty()) {
                // iOS chat bubble: user = orange gradient + tail bottom-right;
                // admin = white + hairline border + RevBrown text
                val bubbleShape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isUser) 18.dp else 6.dp,
                    bottomEnd = if (isUser) 6.dp else 18.dp,
                )
                if (isUser) {
                    Box(
                        modifier = Modifier
                            .clip(bubbleShape)
                            .background(Brush.linearGradient(listOf(RevOrange, RevRed)))
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                    ) {
                        Text(msg.body, color = Color.White, fontSize = 15.sp)
                    }
                } else {
                    Surface(
                        shape = bubbleShape,
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x14000000)),
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                            Text(msg.body, color = RevBrown, fontSize = 15.sp)
                        }
                    }
                }
            }
            Text(msg.created_at.substring(11, 16), color = RevTextSecondary, fontSize = 10.sp,
                 modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
        }
    }
}

@Composable
private fun AttachmentPreview(msg: Message, onOpen: (String) -> Unit) {
    val url = msg.attachment_url ?: return
    when (msg.attachment_type) {
        "image" -> AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable { onOpen(url) },
            contentScale = ContentScale.Crop,
        )
        else -> AttachmentCard(
            icon = if (msg.attachment_type == "pdf") Icons.Default.PictureAsPdf else Icons.Default.FileOpen,
            tint = if (msg.attachment_type == "pdf") RevRed else RevOrange,
            name = msg.attachment_name ?: "Document",
            type = msg.attachment_type ?: "fichier",
            size = msg.attachment_size,
            onClick = { onOpen(url) },
        )
    }
}

@Composable
private fun AttachmentCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    name: String,
    type: String,
    size: Int?,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.width(240.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x14000000)),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(tint),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = RevBrown, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                Text("${type.uppercase()} · ${formatSize(size)}", color = RevTextSecondary, fontSize = 11.sp)
            }
        }
    }
}

private fun formatSize(bytes: Int?): String {
    if (bytes == null) return "—"
    if (bytes < 1024) return "$bytes B"
    if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0)
    return String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}
