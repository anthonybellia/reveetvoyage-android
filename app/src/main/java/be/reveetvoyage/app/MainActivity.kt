package be.reveetvoyage.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import be.reveetvoyage.app.ui.RootScreen
import be.reveetvoyage.app.ui.theme.ReveEtVoyageTheme
import be.reveetvoyage.app.ui.theme.RevBackground
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* user granted/denied — silently ignored */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make system bars blend with our background (works on all Android versions)
        try {
            val color = androidx.compose.ui.graphics.Color(0xFFFFFBF7).toArgb()
            window.statusBarColor = color
            window.navigationBarColor = color
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        } catch (t: Throwable) { /* ignored */ }

        // Android 13+ requires runtime permission for POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            ReveEtVoyageTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().background(RevBackground),
                    color = RevBackground,
                ) {
                    RootScreen()
                }
            }
        }
    }
}
