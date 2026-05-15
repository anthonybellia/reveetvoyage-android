package be.reveetvoyage.app.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.reveetvoyage.app.data.api.ApiService
import be.reveetvoyage.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PageViewModel @Inject constructor(
    private val api: be.reveetvoyage.app.data.api.ApiService,
) : ViewModel() {
    private val _state = MutableStateFlow<PageState>(PageState.Loading)
    val state: StateFlow<PageState> = _state.asStateFlow()

    fun load(slug: String) {
        viewModelScope.launch {
            _state.value = PageState.Loading
            runCatching { api.page(slug) }
                .onSuccess { _state.value = PageState.Loaded(it.title, it.content_html) }
                .onFailure { _state.value = PageState.Error(it.message ?: "Erreur") }
        }
    }
}

sealed class PageState {
    data object Loading : PageState()
    data class Loaded(val title: String, val html: String) : PageState()
    data class Error(val message: String) : PageState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageScreen(
    slug: String,
    fallbackTitle: String,
    onBack: () -> Unit,
    vm: PageViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(slug) { vm.load(slug) }

    val title = (state as? PageState.Loaded)?.title ?: fallbackTitle

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, color = RevBrown, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(RevBackground)) {
            when (val s = state) {
                is PageState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RevOrange)
                }
                is PageState.Error -> Column(
                    modifier = Modifier.fillMaxSize().padding(40.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Default.Error, null, tint = RevRed, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(s.message, color = RevTextSecondary)
                }
                is PageState.Loaded -> StyledHtmlWebView(html = s.html, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun StyledHtmlWebView(html: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = false
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                webViewClient = WebViewClient()
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(
                "https://www.reveetvoyage.be",
                wrapHtml(html),
                "text/html",
                "UTF-8",
                null
            )
        }
    )
}

private fun wrapHtml(body: String): String = """
<!DOCTYPE html>
<html lang="fr">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<style>
  :root {
    --rev-brown: #1F1008;
    --rev-orange: #F09D6B;
    --rev-yellow: #F2C61D;
    --rev-red: #E45F60;
    --rev-text-secondary: #7d7d7d;
  }
  * { box-sizing: border-box; }
  body {
    margin: 0;
    padding: 18px;
    font-family: -apple-system, BlinkMacSystemFont, 'Roboto', sans-serif;
    font-size: 15px;
    line-height: 1.55;
    color: var(--rev-brown);
    background: transparent;
  }
  h1 {
    font-size: 22px;
    font-weight: 700;
    color: var(--rev-brown);
    margin: 0 0 12px;
  }
  h2 {
    font-size: 17px;
    font-weight: 600;
    color: var(--rev-brown);
    margin: 24px 0 8px;
    border-bottom: 2px solid var(--rev-yellow);
    padding-bottom: 4px;
    display: inline-block;
  }
  h3 { font-size: 15px; font-weight: 600; margin: 18px 0 6px; }
  p { margin: 8px 0 12px; }
  a { color: var(--rev-orange); text-decoration: none; font-weight: 500; }
  ul, ol { margin: 8px 0 14px 18px; }
  li { margin: 4px 0; }
  strong { color: var(--rev-brown); }
  em { color: var(--rev-orange); }
</style>
</head>
<body>$body</body>
</html>
""".trimIndent()
