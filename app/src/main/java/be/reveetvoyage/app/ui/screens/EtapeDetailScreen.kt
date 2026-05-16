package be.reveetvoyage.app.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.view.MotionEvent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import be.reveetvoyage.app.data.api.ApiConfig
import be.reveetvoyage.app.data.model.VoyageEtape
import be.reveetvoyage.app.ui.components.*
import be.reveetvoyage.app.ui.components.IOSAlertDialog
import be.reveetvoyage.app.ui.components.IOSButton
import be.reveetvoyage.app.ui.components.IOSButtonStyle
import be.reveetvoyage.app.ui.components.IOSTopBar
import be.reveetvoyage.app.ui.theme.*
import coil.compose.AsyncImage
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EtapeDetailScreen(
    voyageId: Int,
    etapeId: Int,
    onBack: () -> Unit,
    vm: VoyageDetailViewModel = hiltViewModel(),
) {
    val etapes by vm.etapes.collectAsState()
    val toggling by vm.toggling.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(voyageId) { vm.load(voyageId) }

    val etape = etapes.firstOrNull { it.id == etapeId }
    var showItinerarySheet by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var resolvedLat by remember(etapeId) { mutableStateOf<Double?>(null) }
    var resolvedLng by remember(etapeId) { mutableStateOf<Double?>(null) }
    var geocodingInFlight by remember(etapeId) { mutableStateOf(false) }
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }

    // Fallback geocoding when DB has no coordinates but an address is present.
    LaunchedEffect(etape?.id, etape?.adresse, etape?.lieu) {
        if (etape == null) return@LaunchedEffect
        if (etape.hasCoordinates) return@LaunchedEffect
        val query = listOfNotNull(etape.adresse, etape.lieu)
            .filter { it.isNotBlank() }
            .joinToString(", ")
        if (query.isBlank()) return@LaunchedEffect
        geocodingInFlight = true
        val pair = geocodeAddress(context, query)
        geocodingInFlight = false
        if (pair != null) {
            resolvedLat = pair.first
            resolvedLng = pair.second
        }
    }

    val mapLat = etape?.latitude ?: resolvedLat
    val mapLng = etape?.longitude ?: resolvedLng
    val hasResolvedCoords = mapLat != null && mapLng != null

    Column(modifier = Modifier.fillMaxSize().background(RevBackground)) {
        IOSTopBar(title = "Étape ${etape?.ordre ?: ""}", onBack = onBack)
        Box(modifier = Modifier.fillMaxSize()) {
            if (etape == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RevOrange)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (hasResolvedCoords) {
                        MapHero(etape, mapLat!!, mapLng!!)
                    } else if (geocodingInFlight) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(220.dp)
                                .background(Brush.linearGradient(listOf(RevYellow.copy(alpha = .12f), RevOrange.copy(alpha = .06f)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = RevOrange, strokeWidth = 2.dp,
                                    modifier = Modifier.size(24.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Localisation de l'étape…", color = RevTextSecondary, fontSize = 12.sp)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        HeaderCard(etape)

                        val rows = buildInfoRows(etape)
                        if (rows.isNotEmpty()) InfoCard(rows)

                        if (!etape.description.isNullOrBlank()) {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SectionTitle("Notes", Icons.Default.Description)
                                    Text(etape.description, color = RevBrown, fontSize = 14.sp)
                                }
                            }
                        }

                        // --- Attachments section ---
                        if (etape.hasAttachments) {
                            AttachmentsSection(
                                etape = etape,
                                onImageTap = { url -> fullScreenImageUrl = url },
                                onImageLongPress = { url -> downloadFile(context, url, guessFileName(url)) },
                                onDocumentOpen = { url -> openDocument(context, url) },
                                onDocumentDownload = { url -> downloadFile(context, url, guessFileName(url)) },
                            )
                        }

                        if (hasResolvedCoords) {
                            IOSButton(
                                text = "Itinéraire",
                                icon = Icons.Default.Navigation,
                                style = IOSButtonStyle.Primary,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { showItinerarySheet = true },
                            )
                        }

                        IOSButton(
                            text = if (etape.is_completed) "Marquer non effectuée" else "Marquer comme effectuée",
                            icon = if (etape.is_completed) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.CheckCircle,
                            style = if (etape.is_completed) IOSButtonStyle.Ghost else IOSButtonStyle.Secondary,
                            isLoading = etape.id in toggling,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { showConfirm = true },
                        )

                        Spacer(Modifier.height(20.dp))
                    }
                }
            }
        }

        // Itinerary picker (iOS-style)
        if (showItinerarySheet && etape != null && hasResolvedCoords) {
            IOSAlertDialog(
                title = "Ouvrir l'itinéraire",
                message = "Choisis l'application de navigation.",
                confirmText = "Choisir une app",
                cancelText = "Annuler",
                onConfirm = {
                    openMapsChooser(context, mapLat!!, mapLng!!, etape.titre)
                    showItinerarySheet = false
                },
                onDismiss = { showItinerarySheet = false },
            )
        }

        // Toggle confirmation (iOS-style)
        if (showConfirm && etape != null) {
            IOSAlertDialog(
                title = if (etape.is_completed) "Marquer non effectuée ?" else "As-tu bien réalisé cette étape ?",
                message = if (!etape.is_completed) "Tu peux passer à l'étape suivante. On te rappellera les prochaines." else null,
                confirmText = if (etape.is_completed) "Marquer non effectuée" else "Oui, c'est fait",
                cancelText = "Annuler",
                isDestructive = etape.is_completed,
                onConfirm = {
                    vm.toggle(voyageId, etape)
                    showConfirm = false
                },
                onDismiss = { showConfirm = false },
            )
        }
    }

    // Full-screen image viewer overlay
    if (fullScreenImageUrl != null) {
        FullScreenImageViewer(
            imageUrl = resolveUrl(fullScreenImageUrl!!),
            onDismiss = { fullScreenImageUrl = null },
            onDownload = {
                downloadFile(context, fullScreenImageUrl!!, guessFileName(fullScreenImageUrl!!))
                fullScreenImageUrl = null
            },
        )
    }
}

// ============================================================
// Attachments Section
// ============================================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AttachmentsSection(
    etape: VoyageEtape,
    onImageTap: (String) -> Unit,
    onImageLongPress: (String) -> Unit,
    onDocumentOpen: (String) -> Unit,
    onDocumentDownload: (String) -> Unit,
) {
    // Combine single image + images list, deduplicating
    val allImages = buildList {
        etape.image?.takeIf { it.isNotBlank() }?.let { add(it) }
        addAll(etape.images.filter { it.isNotBlank() && it != etape.image })
    }

    if (allImages.isEmpty() && etape.fichier == null) return

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Documents et photos", Icons.Default.Photo)

            // Images gallery
            if (allImages.isNotEmpty()) {
                Text(
                    "Appui long pour télécharger",
                    color = RevTextSecondary,
                    fontSize = 11.sp,
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(end = 4.dp),
                ) {
                    itemsIndexed(allImages) { _, imageUrl ->
                        val resolved = resolveUrl(imageUrl)
                        Box(
                            modifier = Modifier
                                .size(width = 140.dp, height = 100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .combinedClickable(
                                    onClick = { onImageTap(imageUrl) },
                                    onLongClick = { onImageLongPress(imageUrl) },
                                ),
                        ) {
                            AsyncImage(
                                model = resolved,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                            // Small icon overlay bottom-right
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(RevBrown.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.ZoomIn,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Document (fichier)
            if (etape.fichier != null) {
                val fileName = guessFileName(etape.fichier)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = RevOrange.copy(alpha = 0.08f),
                    onClick = { onDocumentOpen(etape.fichier) },
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(RevOrange.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = RevOrange,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                fileName,
                                color = RevBrown,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "Appuyer pour ouvrir",
                                color = RevTextSecondary,
                                fontSize = 11.sp,
                            )
                        }
                        IconButton(
                            onClick = { onDocumentDownload(etape.fichier) },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = "Télécharger",
                                tint = RevOrange,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// Full-screen image viewer with pinch-to-zoom
// ============================================================
@Composable
private fun FullScreenImageViewer(
    imageUrl: String,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                        offset = Offset(
                            x = offset.x + pan.x,
                            y = offset.y + pan.y,
                        )
                    }
                },
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y,
                    ),
                contentScale = ContentScale.Fit,
            )

            // Top bar with close and download buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Fermer",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
                IconButton(onClick = onDownload) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Télécharger",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}

// ============================================================
// Helpers: resolve URL, guess file name, download, open document
// ============================================================
private fun resolveUrl(url: String): String {
    return if (url.startsWith("http")) url else ApiConfig.SITE_BASE + url
}

private fun guessFileName(url: String): String {
    val path = url.substringAfterLast("/").substringBefore("?")
    return if (path.isNotBlank() && path.contains(".")) path else "document"
}

private fun downloadFile(context: Context, url: String, fileName: String) {
    try {
        val resolved = resolveUrl(url)
        val request = DownloadManager.Request(Uri.parse(resolved)).apply {
            setTitle(fileName)
            setDescription("Rêve et Voyage - Téléchargement")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "ReveEtVoyage/$fileName")
        }
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
        Toast.makeText(context, "Téléchargement lancé : $fileName", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Erreur de téléchargement", Toast.LENGTH_SHORT).show()
    }
}

private fun openDocument(context: Context, url: String) {
    try {
        val resolved = resolveUrl(url)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(resolved)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // If no app can handle it, fall back to download
        downloadFile(context, url, guessFileName(url))
    }
}

// ============================================================
// Existing composables (unchanged)
// ============================================================

private suspend fun geocodeAddress(context: Context, query: String): Pair<Double, Double>? {
    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, java.util.Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                kotlinx.coroutines.suspendCancellableCoroutine<Pair<Double, Double>?> { cont ->
                    geocoder.getFromLocationName(query, 1) { addresses ->
                        val a = addresses.firstOrNull()
                        cont.resume(
                            if (a != null) Pair(a.latitude, a.longitude) else null
                        ) {}
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses: List<Address>? = geocoder.getFromLocationName(query, 1)
                addresses?.firstOrNull()?.let { Pair(it.latitude, it.longitude) }
            }
        } catch (t: Throwable) {
            null
        }
    }
}

@Composable
private fun MapHero(etape: VoyageEtape, lat: Double, lng: Double) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .shadow(8.dp)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(16.0)
                    controller.setCenter(GeoPoint(lat, lng))

                    // Intercept scroll so the map can be panned inside a ScrollView
                    setOnTouchListener { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> v.parent?.requestDisallowInterceptTouchEvent(true)
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                                v.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                        false
                    }

                    val marker = Marker(this)
                    marker.position = GeoPoint(lat, lng)
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    marker.title = etape.titre
                    marker.snippet = etape.lieu ?: etape.adresse
                    overlays.add(marker)
                }
            },
            update = { mapView ->
                mapView.controller.setCenter(GeoPoint(lat, lng))
                mapView.invalidate()
            }
        )

        // Floating type badge (like iOS)
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 16.dp),
            shape = RoundedCornerShape(50),
            color = RevBrown.copy(alpha = 0.85f),
            shadowElevation = 4.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(stepIcon(etape.type), null, tint = Color.White, modifier = Modifier.size(13.dp))
                Text(
                    typeLabel(etape.type).uppercase(),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun HeaderCard(etape: VoyageEtape) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row {
                Column(modifier = Modifier.weight(1f)) {
                    if (!etape.hasCoordinates) {
                        // Show type badge inline only when there's no map hero
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(28.dp).clip(CircleShape)
                                .background(Brush.linearGradient(listOf(RevOrange, RevRed))),
                                contentAlignment = Alignment.Center) {
                                Icon(stepIcon(etape.type), null, tint = Color.White, modifier = Modifier.size(15.dp))
                            }
                            Text(typeLabel(etape.type).uppercase(),
                                 color = RevOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(etape.titre, color = RevBrown, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    etape.lieu?.takeIf { it.isNotBlank() }?.let {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Place, null, tint = RevOrange,
                                 modifier = Modifier.size(13.dp))
                            Text(it, color = RevTextSecondary, fontSize = 13.sp)
                        }
                    }
                }
                if (etape.is_completed) {
                    Row(modifier = Modifier.clip(CircleShape).background(Color(0xFF4CAF50).copy(alpha = .15f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32),
                             modifier = Modifier.size(14.dp))
                        Text("Effectuée", color = Color(0xFF2E7D32), fontSize = 11.sp,
                             fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private data class InfoRow(val icon: ImageVector, val label: String, val value: String)

@Composable
private fun InfoCard(rows: List<InfoRow>) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Détails", Icons.Default.Info)
            rows.forEach { r ->
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(r.icon, null, tint = RevOrange, modifier = Modifier.size(18.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(r.label, color = RevTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(r.value, color = RevBrown, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

private fun buildInfoRows(e: VoyageEtape): List<InfoRow> {
    val rows = mutableListOf<InfoRow>()
    e.date?.take(10)?.let { rows += InfoRow(Icons.Default.CalendarMonth, "Date", it) }
    e.heure?.takeIf { it.isNotBlank() }?.let { rows += InfoRow(Icons.Default.Schedule, "Heure", it) }
    e.heure_retour?.takeIf { it.isNotBlank() }?.let { rows += InfoRow(Icons.Default.Schedule, "Heure retour", it) }
    e.adresse?.takeIf { it.isNotBlank() }?.let { rows += InfoRow(Icons.Default.LocationOn, "Adresse", it) }
    e.lieu_retour?.takeIf { it.isNotBlank() }?.let { rows += InfoRow(Icons.Default.Place, "Lieu de retour", it) }
    e.compagnie?.takeIf { it.isNotBlank() }?.let { rows += InfoRow(Icons.Default.Business, "Compagnie", it) }
    e.numero_ref?.takeIf { it.isNotBlank() }?.let { rows += InfoRow(Icons.Default.ConfirmationNumber, "Référence", it) }
    e.cout?.takeIf { it > 0 }?.let { rows += InfoRow(Icons.Default.Euro, "Coût", "${it.toInt()} €") }
    return rows
}

private fun stepIcon(type: String): ImageVector = when (type) {
    "vol", "vol_aller", "vol_retour" -> Icons.Default.Flight
    "hotel" -> Icons.Default.Hotel
    "activite" -> Icons.Default.DirectionsWalk
    "transfert" -> Icons.Default.DirectionsCar
    "restaurant" -> Icons.Default.Restaurant
    "note" -> Icons.Default.Description
    "document" -> Icons.Default.InsertDriveFile
    else -> Icons.Default.Circle
}

private fun typeLabel(type: String): String = when (type) {
    "vol_aller" -> "Vol aller"
    "vol_retour" -> "Vol retour"
    "vol" -> "Vol"
    "hotel" -> "Hôtel"
    "activite" -> "Activité"
    "transfert" -> "Transfert"
    "restaurant" -> "Restaurant"
    "note" -> "Note"
    "document" -> "Document"
    else -> type.replaceFirstChar { it.uppercase() }
}

private fun openMapsChooser(context: android.content.Context, lat: Double, lng: Double, label: String) {
    val encoded = Uri.encode(label)
    val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($encoded)")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    val chooser = Intent.createChooser(intent, "Itinéraire vers $label")
    chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(chooser)
}
