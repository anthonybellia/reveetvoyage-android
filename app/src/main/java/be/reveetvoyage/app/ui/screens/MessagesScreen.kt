package be.reveetvoyage.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
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

    override fun onCleared() {
        stop(); super.onCleared()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(onBack: () -> Unit, vm: MessagesViewModel = hiltViewModel()) {
    val messages by vm.messages.collectAsState()
    val draft by vm.draft.collectAsState()
    val isSending by vm.isSending.collectAsState()
    val listState = rememberLazyListState()

    DisposableEffect(Unit) { vm.start(); onDispose { vm.stop() } }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Équipe Rêve et Voyage", fontWeight = FontWeight.Bold, color = RevBrown) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, null)
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

            // Input bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = draft, onValueChange = vm::setDraft,
                    placeholder = { Text("Ton message…") },
                    shape = RoundedCornerShape(18.dp),
                    maxLines = 4,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = vm::send,
                    enabled = draft.isNotBlank() && !isSending,
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(
                        Brush.linearGradient(listOf(RevOrange, RevRed))
                    ),
                ) {
                    if (isSending) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp))
                    else Icon(Icons.Default.Send, null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: Message) {
    val isUser = msg.isFromUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp, topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp,
                        )
                    )
                    .background(
                        if (isUser) Brush.linearGradient(listOf(RevOrange, RevRed))
                        else Brush.linearGradient(listOf(RevCardBackground, RevCardBackground))
                    )
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            ) {
                Text(msg.body, color = if (isUser) Color.White else RevBrown, fontSize = 15.sp)
            }
            Text(msg.created_at.substring(11, 16), color = RevTextSecondary, fontSize = 10.sp,
                 modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
        }
    }
}
