package be.reveetvoyage.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import be.reveetvoyage.app.data.model.VoyageEtape
import be.reveetvoyage.app.ui.components.*
import be.reveetvoyage.app.ui.theme.*
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Étape ${etape?.ordre ?: ""}", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(RevBackground)) {
            if (etape == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RevOrange)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (etape.hasCoordinates) {
                        MapHero(etape)
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
                                    Text(etape.description, color = RevText, fontSize = 14.sp)
                                }
                            }
                        }

                        if (etape.hasCoordinates) {
                            Button(
                                onClick = { showItinerarySheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = RevOrange),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                            ) {
                                Icon(Icons.Default.Navigation, null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("Itinéraire", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        OutlinedButton(
                            onClick = { showConfirm = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            enabled = etape.id !in toggling,
                        ) {
                            if (etape.id in toggling) {
                                CircularProgressIndicator(color = RevOrange, strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp))
                            } else {
                                Icon(
                                    if (etape.is_completed) Icons.AutoMirrored.Filled.ArrowBack
                                    else Icons.Default.CheckCircle,
                                    null, tint = if (etape.is_completed) RevTextSecondary else RevOrange
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (etape.is_completed) "Marquer non effectuée"
                                    else "Marquer comme effectuée",
                                    color = if (etape.is_completed) RevTextSecondary else RevBrown,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))
                    }
                }
            }
        }

        // Itinerary picker
        if (showItinerarySheet && etape != null && etape.hasCoordinates) {
            AlertDialog(
                onDismissRequest = { showItinerarySheet = false },
                title = { Text("Ouvrir l'itinéraire") },
                text = { Text("Choisis l'application de navigation.") },
                confirmButton = {
                    TextButton(onClick = {
                        openMapsChooser(context, etape.latitude!!, etape.longitude!!, etape.titre)
                        showItinerarySheet = false
                    }) { Text("Choisir une app", fontWeight = FontWeight.Bold, color = RevOrange) }
                },
                dismissButton = {
                    TextButton(onClick = { showItinerarySheet = false }) { Text("Annuler") }
                }
            )
        }

        // Toggle confirmation
        if (showConfirm && etape != null) {
            AlertDialog(
                onDismissRequest = { showConfirm = false },
                title = {
                    Text(if (etape.is_completed) "Marquer non effectuée ?"
                         else "As-tu bien réalisé cette étape ?")
                },
                text = {
                    if (!etape.is_completed) {
                        Text("Tu peux passer à l'étape suivante. On te rappellera les prochaines.")
                    } else null
                },
                confirmButton = {
                    TextButton(onClick = {
                        vm.toggle(voyageId, etape)
                        showConfirm = false
                    }) {
                        Text(
                            if (etape.is_completed) "Marquer non effectuée"
                            else "Oui, c'est fait",
                            color = if (etape.is_completed) RevRed else RevOrange,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirm = false }) { Text("Annuler") }
                }
            )
        }
    }
}

@Composable
private fun MapHero(etape: VoyageEtape) {
    val lat = etape.latitude ?: return
    val lng = etape.longitude ?: return

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
